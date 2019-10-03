# Test Claimant creator

This application is a utility for seeding the claimant and DWP database with data to be used for manual testing.

Information about a claimant will be read from claimant.yml (in [src/test/resources/test-claimant-creator](../../../../../../../resources/test-claimant-creator/)). There is an example file in [claimant-example.yml](../../../../../../../resources/test-claimant-creator/claimant-example.yml)

This will add an active claim with a card account id, a claimant, address, UCHousehold, UCAdult, UCChild(s) and a payment cycle with an end date of yesterday.

Children's dates of birth are added using an ISO-8601 duration and an enum value representing when a child should be that age. 

E.g.
```
    - age: P3Y11M21D 
      at: START_OF_NEXT_CYCLE
```
will create a child who is three years, 11 months and 21 days at the start of the next payment cycle. 

Adding this child would trigger a child turning four email to be sent as they will fall off the scheme during the next cycle.

Optionally, you can add `excludeFromExistingCycle: true` to a childInfo to prevent that child appearing on the existing payment cycle. This feature is necessary for testing back dated vouchers for a new child.

To run the test claimant creator, run the [TestClaimantCreator main method](TestClaimantCreator.java).

The database connection details are defined in [application-test-claimant-creator.yml](../../../../../../../resources/application-test-claimant-creator.yml). Make sure these match your local instance of the claimant and eligibility (DWP) database before running the application. 
