Feature: Regression

  We want to ensure that certain defects are tended to and never resurface.

  Scenario: The build should not fail if the Work Item lookup returns null
    Given an unremarkable build configuration
    When a job is executed from the build for commit message "US12345: update functionality" but no Story exists
    Then no Changeset object should be created
    And the build should not fail