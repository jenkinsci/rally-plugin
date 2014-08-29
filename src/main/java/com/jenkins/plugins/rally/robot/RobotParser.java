/*
 * Copyright 2008-2011 Nokia Siemens Networks Oyj
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jenkins.plugins.rally.robot;

        import hudson.AbortException;
        import hudson.FilePath;
        import hudson.Util;
        import hudson.model.AbstractBuild;
        import com.jenkins.plugins.rally.robot.model.RobotTestObject;
        import com.jenkins.plugins.rally.robot.model.RobotCaseResult;
        import com.jenkins.plugins.rally.robot.model.RobotResult;
        import com.jenkins.plugins.rally.robot.model.RobotSuiteResult;
        import hudson.remoting.VirtualChannel;

        import java.io.File;
        import java.io.FileInputStream;
        import java.io.IOException;
        import java.util.ArrayList;
        import java.util.List;

        import javax.xml.stream.Location;
        import javax.xml.stream.XMLInputFactory;
        import javax.xml.stream.XMLStreamException;
        import javax.xml.stream.XMLStreamReader;

        import org.apache.commons.lang.StringUtils;
        import org.apache.tools.ant.DirectoryScanner;
        import org.apache.tools.ant.types.FileSet;

public class RobotParser {

    public List<RobotCaseResult> parse(String outputFileLocations, String outputPath, AbstractBuild<?, ?> build)
            throws InterruptedException, IOException {
        RobotParserCallable callable = new RobotParserCallable(outputFileLocations);
        RobotResult result = new FilePath(build.getWorkspace(), outputPath).act(callable);
        return callable.getResults();
    }

    public static final class RobotParserCallable implements
            FilePath.FileCallable<RobotResult> {

        private static final long serialVersionUID = 1L;
        private final String outputFileLocations;
        private List<RobotCaseResult> results = new ArrayList<RobotCaseResult>();

        public RobotParserCallable(String outputFileLocations) {
            this.outputFileLocations = outputFileLocations;
        }

        public List<RobotCaseResult> getResults(){
            return this.results;
        }

        public RobotResult invoke(File ws, VirtualChannel channel)
                throws IOException {
            FileSet setInWorkspace = Util
                    .createFileSet(ws, outputFileLocations);
            DirectoryScanner resultScanner = setInWorkspace
                    .getDirectoryScanner();

            String[] files = resultScanner.getIncludedFiles();
            if (files.length == 0) {
                throw new AbortException(
                        "No files found in path " + ws.getAbsolutePath() + " with configured filemask: " + outputFileLocations);
            }
            RobotResult result = new RobotResult();

            for(String file : files){
                XMLInputFactory factory = XMLInputFactory.newInstance();
                factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
                File baseDirectory = resultScanner.getBasedir();
                File reportFile = new File(baseDirectory, file);

                //get the potential directories emerging from the use of GLOB filemask accounted in the splitted file parsing
                String dirFromFileGLOB = new File(file).getParent();
                if(dirFromFileGLOB != null)
                    baseDirectory = new File(baseDirectory, dirFromFileGLOB.toString());
                FileInputStream inputStream = new FileInputStream(reportFile);
                try {
                    XMLStreamReader reader = factory.createXMLStreamReader(inputStream, "UTF-8");
                    try {
                        parseResult(result, reader, baseDirectory);
                    } finally {
                        reader.close();
                    }
                } catch (XMLStreamException e1) {
                    throw new IOException("Parsing of output xml failed!", e1);
                } finally {
                    inputStream.close();
                }
            }
            return result;
        }

        private RobotResult parseResult(RobotResult result, XMLStreamReader reader, File baseDirectory) throws XMLStreamException, IOException {
            while(reader.hasNext()){
                reader.next();
                if(reader.isStartElement()){
                    String tagName = reader.getLocalName();
                    if("statistics".equals(tagName))
                        //we already have all data from suites and tests so we can stop parsing
                        break;
                    else if("robot".equals(tagName)){
                        result.setTimeStamp(reader.getAttributeValue(null, "generated"));
                    } else if("suite".equals(tagName)){
                        result.addSuite(processSuite(reader, result, baseDirectory));
                    }
                }
            }
            return result;
        }

        private RobotSuiteResult processSuite(XMLStreamReader reader, RobotTestObject parent, File baseDirectory) throws IOException, XMLStreamException {
            String splitXMLPath = reader.getAttributeValue(null, "src");
            if (splitXMLPath != null) {
                return getSplitXMLSuite(parent, baseDirectory, splitXMLPath);
            }
            RobotSuiteResult suite = new RobotSuiteResult();
            suite.setParent(parent);
            suite.setName(reader.getAttributeValue(null, "name"));
            //parse children, which can be test cases or test suites
            while(reader.hasNext()){
                reader.next();
                if(reader.isStartElement()){
                    String tagName = reader.getLocalName();
                    if("suite".equals(tagName)){
                        suite.addChild(processSuite(reader, suite, baseDirectory));
                    } else if("test".equals(tagName)){
                        suite.addCaseResult(processTest(reader, suite));
                    } else if("kw".equals(tagName) && "teardown".equals(reader.getAttributeValue(null, "type"))) {
                        ignoreUntilStarts(reader, "status");
                        if ("FAIL".equals(reader.getAttributeValue(null, "status"))) {
                            suite.failTeardown();
                        }
                    } else if("status".equals(tagName)){
                        suite.setStartTime(reader.getAttributeValue(null, "starttime"));
                        suite.setEndTime(reader.getAttributeValue(null, "endtime"));
                    }
                } else if (reader.isEndElement() && "suite".equals(reader.getLocalName())) {
                    return suite;
                }
            }
            throw xmlException("No matching end tag found for test suite: " + suite.getName(), reader);
        }

        private XMLStreamException xmlException(String message, XMLStreamReader reader) {
            Location location = reader.getLocation();
            return new XMLStreamException(message +
                    " (at line: "+
                    location.getLineNumber()+
                    " column: "+
                    location.getColumnNumber()+")");
        }

        private RobotSuiteResult getSplitXMLSuite(RobotTestObject parent, File baseDirectory, String path) throws XMLStreamException, IOException {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            FileInputStream inputStream = new FileInputStream(new File(baseDirectory, path));
            try {
                XMLStreamReader splitReader = factory.createXMLStreamReader(inputStream, "UTF-8");
                try {
                    while(splitReader.hasNext()){
                        splitReader.next();
                        if(splitReader.isStartElement() && "suite".equals(splitReader.getLocalName())) {
                            return processSuite(splitReader, parent, baseDirectory);
                        }
                    }
                    throw xmlException("Illegal split xml output. Could not find suite element.", splitReader);
                } finally {
                    splitReader.close();
                }
            } finally {
                inputStream.close();
            }
        }

        private void ignoreUntilStarts(XMLStreamReader reader, String element) throws XMLStreamException {
            List<String> elementStack = new ArrayList<String>();
            while(reader.hasNext()) {
                reader.next();
                if (reader.isStartElement()) {
                    if (elementStack.isEmpty() && reader.getLocalName().equals(element)) {
                        return;
                    } else {
                        elementStack.add(reader.getLocalName());
                    }
                } else if (reader.isEndElement()) {
                    if (elementStack.isEmpty()) {
                        throw xmlException("Could not find element "+element, reader);
                    }
                    if (!elementStack.get(elementStack.size()-1).equals(reader.getLocalName())) {
                        throw xmlException("Illegal xml input. End element "+
                                reader.getLocalName()+
                                " while waiting for element "+
                                elementStack.get(elementStack.size()-1), reader);
                    }
                    elementStack.remove(elementStack.size()-1);
                }
            }
            throw xmlException("Could not find element "+element, reader);
        }

        private void ignoreUntilEnds(XMLStreamReader reader, String element) throws XMLStreamException {
            List<String> elementStack = new ArrayList<String>();
            while(reader.hasNext()) {
                reader.next();
                if (reader.isStartElement()) {
                    elementStack.add(reader.getLocalName());
                } else if (reader.isEndElement()) {
                    String tagName = reader.getLocalName();
                    if (elementStack.isEmpty()) {
                        if (tagName.equals(element)) {
                            return;
                        }
                        throw xmlException("Illegal xml input. Could not find end of element "+
                                element+ ". Unexpected end of element "+ tagName, reader);
                    } else if (!elementStack.get(elementStack.size()-1).equals(tagName)) {
                        throw xmlException("Illegal xml input. End element "+
                                tagName + " while waiting for element "+
                                elementStack.get(elementStack.size()-1), reader);
                    } else {
                        elementStack.remove(elementStack.size()-1);
                    }
                }
            }
            throw xmlException("Could not find end of element "+element, reader);
        }


        private RobotCaseResult processTest(XMLStreamReader reader, RobotSuiteResult result) throws XMLStreamException {
            RobotCaseResult caseResult = new RobotCaseResult();
            caseResult.setParent(result);
            //parse attributes
            caseResult.setName(reader.getAttributeValue(null, "name"));
            setCriticalityIfAvailable(reader, caseResult);
            //parse keywords text
            String testCaseText = processTestKeywords(reader);
            caseResult.setDescription(testCaseText);
            //parse test tags
            ignoreUntilStarts(reader, "tags");
            caseResult.addTags(processTags(reader));
            //parse test details from nested status
            ignoreUntilStarts(reader, "status");
            caseResult.setPassed("PASS".equals(reader.getAttributeValue(null, "status")));
            caseResult.setStarttime(reader.getAttributeValue(null, "starttime"));
            caseResult.setEndtime(reader.getAttributeValue(null, "endtime"));
            setCriticalityIfAvailable(reader, caseResult);
            while(reader.hasNext()){
                reader.next();
                if(reader.getEventType() == XMLStreamReader.CHARACTERS){
                    String error = reader.getText();
                    caseResult.setErrorMsg(error);
                } else if (reader.isEndElement()) {
                    if ("status".equals(reader.getLocalName())) {
                        break;
                    } else {
                        throw xmlException("No end tag found for status while parsing test case: " +
                                caseResult.getName(), reader);
                    }
                }
            }
            ignoreUntilEnds(reader, "test");

            results.add(caseResult);

            return caseResult;
        }

        private String processTestKeywords(XMLStreamReader reader) throws XMLStreamException {
            List<String> keywords = new ArrayList<String>();
            while(reader.hasNext()){
                reader.next();
                if(reader.isStartElement()){
                    String localName = reader.getLocalName();
                    if("kw".equals(localName)) {
                        String type = reader.getAttributeValue(null, "type");
                        String name = reader.getAttributeValue(null, "name");

                        if ("kw".equals(type)) {
                            // keyword name is prefixed with name of library, strip off the name of the library
                            if (name.indexOf('.') > 0) {
                                name = name.substring(name.indexOf('.') + 1);
                            }
                            keywords.add(name);
                        }
                        ignoreUntilEnds(reader, localName);
                    }
                    else{
                        ignoreUntilEnds(reader, localName);
                        break;
                    }
                }
            }

            return StringUtils.join(keywords, "<br />");
        }

        private List<String> processTags(XMLStreamReader reader) throws XMLStreamException {
            List<String> taglist = new ArrayList<String>();
            while(reader.hasNext()){
                reader.next();
                if(reader.isStartElement() && "tag".equals(reader.getLocalName())){
                    while(reader.hasNext()){
                        reader.next();
                        if(reader.getEventType() == XMLStreamReader.CHARACTERS){
                            taglist.add(reader.getText());
                        } else if(reader.isEndElement() && "tag".equals(reader.getLocalName())){
                            break;
                        }
                    }
                } else if(reader.isEndElement() && "tags".equals(reader.getLocalName())){
                    break;
                }
            }
            return taglist;
        }

        private static void setCriticalityIfAvailable(XMLStreamReader reader, RobotCaseResult caseResult) {
            String criticality = reader.getAttributeValue(null, "critical");
            if (criticality != null) {
                if(criticality.equals("yes"))
                    caseResult.setCritical(true);
                else
                    caseResult.setCritical(false);
            }
        }
    }


}