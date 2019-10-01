# Claimant creator

The application is a utility for loading the claimant and DWP database with data to be used for manual testing.

Information about a claimant will be read from claimant.yml (in [src/main/resources](src/main/resources)). There is an example claimant in [claimant-example.yml](src/main/resources/claimant-example.yml)

This will add an active claim with a card account id, a claimant, address, UCHousehold, UCAdult, UCChild(s) and a payment cycle with a start date of yesterday.

Children's dates of birth are added using an ISO-8601 duration and an enum value representing when a child should be that age. 

E.g.
```
    - age: P3Y11M21D 
      at: START_OF_NEXT_CYCLE
```
will create a child who is three years, 11 months and 21 days at the beginning for the next payment cycle. 
Adding this child would trigger a child turning four email as they will fall off the scheme during the next cycle.
