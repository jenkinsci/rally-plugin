package com.jenkins.plugins.rally;

import com.jenkins.plugins.rally.robot.RobotParser;
import com.jenkins.plugins.rally.robot.model.RobotCaseResult;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;

import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import com.jenkins.plugins.rally.scm.ChangeInformation;
import com.jenkins.plugins.rally.scm.Changes;

/**
 * 
 *
 * @author Tushar Shinde
 */
public class PostBuild extends Builder {

	private final String userName;
	private final String password;
	private final String workspace;
    private final String project;
    private final String filePattern;
    private final String testFolder;
    private final String scmuri;
	private final String scmRepoName;
	private final String changesSince;
	private final String startDate;
	private final String endDate;
	private final String debugOn;
	private final String proxy;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public PostBuild(
            String userName, String password, String workspace, String project,
            String filePattern, String testFolder, String scmuri, String scmRepoName,
            String changesSince, String startDate, String endDate, String debugOn, String proxy) {
        this.userName = userName;
        this.password = password;
    	this.workspace = workspace;
    	this.project = project;
        this.filePattern = filePattern;
        this.testFolder = testFolder;
    	this.scmuri = scmuri;
    	this.scmRepoName = scmRepoName;
    	this.changesSince = changesSince;
    	this.startDate = startDate;
    	this.endDate = endDate;
    	this.debugOn = debugOn;
        this.proxy = proxy;
    }

	@Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    	PrintStream out = listener.getLogger();

        //out.println("rally-update plugin getting changes...");

    	Changes changes = PostBuildHelper.getChanges(changesSince, startDate, endDate, build, out);
    	
    	RallyConnector rallyConnector = null;
    	boolean result;
    	try {
            String lastCommitId = build.getEnvironment(listener).expand("${GIT_BRANCH}:${GIT_COMMIT}");
            int indexOfOriginSlash = lastCommitId.indexOf("/"), indexOfColon = lastCommitId.indexOf(":");
            lastCommitId = lastCommitId.substring(indexOfOriginSlash + 1, indexOfColon + 8 + 1);

    		rallyConnector = new RallyConnector(userName, password, workspace, project, scmuri, scmRepoName, proxy);

            out.println("rally-update plugin found " + changes.getChangeInformation().size() + " changes...");
            updateChangeSets(build, out, changes, rallyConnector, lastCommitId);
            updateRallyTestCases(build, listener, rallyConnector, lastCommitId);
        } catch(Exception e) {
        	out.println("\trally update plug-in error: error while creating connection to rally: " + e.getMessage());
        	e.printStackTrace(out);
        } finally {
        	try {
        		if(rallyConnector != null) rallyConnector.closeConnection();
        	} catch(Exception e) {out.println("\trally update plug-in error: error while closing connection: " + e.getMessage()); 
        		e.printStackTrace(out);
        	}
        }

        return true;
    }

    private void updateRallyTestCases(AbstractBuild build, BuildListener listener, RallyConnector rallyConnector, String lastCommitId) throws InterruptedException, IOException {
        PrintStream out = listener.getLogger();
        String xmlFilePattern = build.getEnvironment(listener).expand(filePattern);
        String xmlTestFolder = build.getEnvironment(listener).expand(testFolder);
        List<RobotCaseResult> results = new RobotParser().parse(xmlFilePattern, xmlTestFolder, build);
        Pattern p = Pattern.compile("(US\\d+|DE\\d+)", Pattern.CASE_INSENSITIVE);

        if(lastCommitId == null){
            lastCommitId = "automated-build:" + (new Date()).getTime();
        }

        lastCommitId+= ":" + build.getNumber();

        if(debugOn.equals("true")){
            out.println("Test results populated from [" + xmlTestFolder + "] for [" + xmlFilePattern + "]");
            out.println("There are [" + results.size() + "] test cases found for build [" + lastCommitId + "]");
        }

        for (RobotCaseResult res : results) {
            try {
                //rallyConnector.createTestCaseResult(res.getName(), res.isPassed(), "build-automated" );
                String end = getDateFormattedForRally(res.getEndtime());
                String workProduct = null;

                // extract the work product if one was set on the test case
                for(String tag: res.getTags()){
                    Matcher m = p.matcher(tag);
                    if(m.find()) {
                        workProduct = m.group(1).toUpperCase();
                        break;
                    }
                }

                if(debugOn.equals("true")){
                    out.println("Updating test case [" + workProduct + ':'+ res.getName() + "] with [" + res.isPassed() + "]");
                }
                rallyConnector.updateRallyTestCaseResult(
                        res.getName(), res.getDescription(), workProduct,
                        lastCommitId, res.isPassed(), end, res.getErrorMsg(), res.getFile());

            } catch (Exception e) {
                out.println("\trally update plug-in error: error setting test case result: " + e.getMessage());
                e.printStackTrace(out);
            }
        }

        // add all the test cases to the test set and create their result objects
        rallyConnector.createResultsInTestSets(debugOn.equals("true"), out);
    }

    private void updateChangeSets(AbstractBuild build, PrintStream out, Changes changes, RallyConnector rallyConnector, String lastCommitId) {
        boolean result;
        for(ChangeInformation ci : changes.getChangeInformation()) { //build level
            try {
                for(Object item : ci.getChangeLogSet().getItems()) { //each changes in above build
                    ChangeLogSet.Entry cse = (ChangeLogSet.Entry) item;
                    RallyDetailsDTO rdto = PostBuildHelper.populateRallyDetailsDTO(debugOn, build, ci, cse, lastCommitId, out);
                    if(!rdto.getId().isEmpty()) {
                        try {
                            result = rallyConnector.updateRallyChangeSet(rdto);
                        } catch(Exception e) {
                            out.println("\trally update plug-in error: could not update changeset entry: "  + e.getMessage());
                            e.printStackTrace(out);
                        }

                        try {
                            result = rallyConnector.updateRallyTaskDetails(rdto);
                        } catch(Exception e) {
                            out.println("\trally update plug-in error: could not update TaskDetails entry: "  + e.getMessage());
                            e.printStackTrace(out);
                        }
                    } else {
                        out.println("Could not update rally due to absence of id in a comment " + rdto.getMsg());
                    }
                }
            } catch(Exception e) {
                out.println("\trally update plug-in error: could not iterate or populate through getChangeLogSet().getItems(): "  + e.getMessage());
                e.printStackTrace(out);
            }
        }
    }

    private String getDateFormattedForRally(String time) throws ParseException {
        SimpleDateFormat robotFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
        SimpleDateFormat rallyFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        return rallyFormat.format(robotFormat.parse(time));
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This get displayed at 'Add build step' button.
         */
        public String getDisplayName() {
            return "Update Rally Task and ChangeSet";
        }
    }
    
    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getWorkspace() {
		return workspace;
	}

    public String getFilePattern() {
        return filePattern;
    }

    public String getTestFolder() {
        return testFolder;
    }

    public String getProject() {
        return project;
    }

    public String getScmuri() {
        return scmuri;
    }

    public String getScmRepoName() {
		return scmRepoName;
	}

	public String getChangesSince() {
		return changesSince;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public String getDebugOn() {
		return debugOn;
	}   
  
    public String getProxy(){
      return proxy;
  }
}

/*'
private static String createTestSet(RallyRestApi restApi, String TSName, String points)throws IOException, URISyntaxException {
  QueryRequest testcases = new QueryRequest("Test Case");
  testcases.setFetch(new Fetch("FormattedID", "Name", "Owner","Test Folder"));
  // All Test cases
  testcases.setQueryFilter(new QueryFilter("TestFolder.Name", "=","testFolder").and(new QueryFilter("Method", "=", "Manual")));

  testcases.setOrder("FormattedID ASC");
  QueryResponse queryResponse = restApi.query(testcases);
  JsonArray testCaseList = new JsonArray();
  if (queryResponse.wasSuccessful()) {
    System.out.println(String.format("\nTotal results: %d", queryResponse.getTotalResultCount()));
    testCaseList=queryResponse.getResults().getAsJsonArray();
  }else{
    for (String err : queryResponse.getErrors()) {
      System.err.println("\t" + err);
    }
  }

  String ref = "null";
  System.out.println("Creating TestSet: "+TSName);
  try {
    if(!testCaseList.isJsonNull()){
      restApi.setApplicationName("PSN");
      JsonObject newTS = new JsonObject();
      newTS.addProperty("Name", TSName);
      newTS.addProperty("PlanEstimate", points);
      newTS.addProperty("Project", Project_ID);
      newTS.addProperty("Release", Release_ID);
      newTS.addProperty("Iteration", Iteration_ID);
      newTS.add("TestCases", testCaseList);
      CreateRequest createRequest = new CreateRequest("testset",newTS);
      CreateResponse createResponse = restApi.create(createRequest);
      ref = createResponse.getObject().get("_ref").getAsString();
    }
  } catch (Exception e) {
    //System.out.println("Exception Caught: " + e.getMessage());
  }
  return ref;
}
* '*/