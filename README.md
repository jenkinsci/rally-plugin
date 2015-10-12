## Rally Plugin

[![Build Status](https://jenkins.ci.cloudbees.com/buildStatus/icon?job=plugins/rally-plugin)](https://jenkins.ci.cloudbees.com/job/plugins/job/rally-plugin/)

This is a Jenkins Plug-in which

1. pulls SCM ChangeSet information from Jenkins Builds and updates relevant Rally defect or story as a build action.
1. picks up Task details [status, actual hrs, todo hrs etc] from scm comments (if provided) and updates rally task details accordingly.
1. saves developers the effort of writing, installing, and maintaining check-in hooks for SCM tools (svn, cvs, perforce etc) in order to update Rally changsets, a task which is especially difficult if your organisation uses more than one configuration management tool.
1. stores your Jenkins build statuses in Rally every time a story or defect is updated by commit message (optional).

For more information, please see the [project page](https://jenkinsci.github.io/rally-plugin).

## Development

### Maven Tasks

Here is a list of maven tasks that I use on this project:

* **mvn verify**: runs all tests
* **mvn package**: creates the `hpi` plugin archive to be used with Jenkins
* **mvn hpi:run -Djetty.port=8090**: runs the Jenkins server (with the plugin pre-loaded) on port 8090
* **mvn cobertura:cobertura**: runs all the tests, gathering code coverage metrics
* **mvn org.pitest:pitest-maven:mutationCoverage**: runs [Pitest](http://pitest.org/) mutation coverage
* **mvn org.pitest:pitest-maven:scmMutationCoverage -Dinclude=ADDED,UNKNOWN,MODIFIED -DmutationThreshold=85**: runs PITest mutation coverage only on modified files, failing if the threshold is below 85%

## License

This project is distributed under the MIT license.