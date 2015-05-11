package com.jenkins.plugins.rally;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.PrintStream;

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
	private final String apiKey;
	private final String workspace;
	private final String project;
	private final String scmuri;
	private final String scmRepoName;
	private final String changesSince;
	private final String startDate;
	private final String endDate;
	private final String debugOn;
	private final String proxy;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public PostBuild(String userName, String apiKey, String workspace, String project, String scmuri, String scmRepoName, String changesSince, String startDate, String endDate, String debugOn, String proxy) {
		this.userName = userName;
        this.apiKey = apiKey;
    	this.workspace = workspace;
    	this.project = project;
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
    	Changes changes = null;
    	changes = PostBuildHelper.getChanges(changesSince, startDate, endDate, build, out);
    	
    	RallyConnector rallyConnector = null;
    	boolean result;
    	try {
    		rallyConnector = new RallyConnector(apiKey, workspace, scmuri, scmRepoName, proxy);
	        for(ChangeInformation ci : changes.getChangeInformation()) { //build level
	        	try {
		        	for(Object item : ci.getChangeLogSet().getItems()) { //each changes in above build
		        		ChangeLogSet.Entry cse = (ChangeLogSet.Entry) item;
		        		RallyDetailsDTO rdto = PostBuildHelper.populateRallyDetailsDTO(debugOn, build, ci, cse, out);
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

	public String getApiKey() {
		return apiKey;
	}

	public String getWorkspace() {
		return workspace;
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
