Feature: Multiple Story Update

  A user may want to associate a Changeset with more than one Story and/or Defect.

  Scenario: Commit message containing two Story identifiers instigates Changeset association with both Stories
    Given an unremarkable build configuration
    When a job is executed from the build for commit message "US12345 and US54321: update functionality"
    Then both stories should receive associations with the same Changeset