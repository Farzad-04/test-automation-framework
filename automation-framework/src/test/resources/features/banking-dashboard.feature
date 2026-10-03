@ui
Feature: Banking dashboard customer journeys
  Customers can manage accounts and move money from the dashboard.

  Scenario: Registration creates a checking account
    Given I open the registration form
    When I register as "New" "Customer"
    Then the dashboard greets "New"
    And the customer has "1" checking account
    And transfers are disabled until another account is opened

  Scenario: A customer opens a savings account
    Given I am signed in as a customer
    When I open a savings account with an opening balance of "150.50"
    Then the dashboard shows "2" accounts and a total balance of "$150.50"

  Scenario: A customer transfers money between accounts
    Given I am signed in with a checking balance of "250.00" and a savings balance of "25.00"
    When I transfer "40.00" from checking to savings with note "Cucumber transfer"
    Then the checking balance is "$210.00" and the savings balance is "$65.00"
    And the transfer note "Cucumber transfer" appears in recent activity
