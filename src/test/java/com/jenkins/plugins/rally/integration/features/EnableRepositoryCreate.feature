Feature: Enable repository create

  If the user has not yet created a Rally SCMRepository object with which to associate
  Changesets, we want to grant the user the ability to automatically create that object.

  Scenario: The user has not created the SCMRepository and has not selected the option
    Given a build that is configured not to create an SCMRepository object
    And that SCMRepository object does not exist
    When a job is executed from the build for commit message "US12345: implemented a test"
    Then an error should be thrown indicating that the configured repository does not exist

  Scenario: The user has not created the SCMRepository and has selected the option
    Given a build that is configured to create an SCMRepository object
    And that SCMRepository object does not exist
    When a job is executed from the build for commit message "US12345: implemented a test"
    Then a request to create the SCMRepository object should be sent
    And a request to create a Changeset object associated with the SCMRepository object should be sent

  Scenario: The SCMRepository has been created and the user has selected the option to create it
    Given a build that is configured to create an SCMRepository object
    And that SCMRepository object already exists
    When a job is executed from the build for commit message "US12345: implemented a test"
    Then a request to create the SCMRepository object should not be sent
    And a request to create a Changeset object associated with the SCMRepository object should be sent

  Scenario: The user has not created the SCMRepository and has not selected the option
    Given a build that is configured not to create an SCMRepository object
    And that SCMRepository object does not exist
    When a job is executed from the build for commit message "US12345: implemented a test"
    Then a request to create the SCMRepository object should not be sent
    And an error should be thrown indicating that the configured repository does not exist