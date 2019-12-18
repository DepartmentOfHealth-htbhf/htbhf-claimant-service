db
=============

This sub-project is responsible for the definition of the Database Schema, using the Postgresql dialect.


DB upgrade scripts
-------------

Database migrations will be performed by [Flyway](https://flywaydb.org/documentation/), invoked automagically by spring
(Search [Spring Boot properties](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html)
for 'flyway' to find the relevant properties, as configured in api/src/main/resources/application.yml).

Flyway uses migration scripts in src/main/resources/db.migration.
There is a strong naming convention for these scripts:
```
V_major_minor__description.sql
```
For example: `V1_001__create_claimant_table.sql`.

Please note that to ensure correct ordering on the filesystem minor version numbers should be 3 digits long.

Try to ensure that each migration script runs as quickly as possible, and avoid actions that create broader or longer-lasting locks than necessary.
Try not to make large data changes in the same script as a EXCLUSIVE lock task like `alter table`.
Flyway creates a transaction per migration script and if you mix the two the non blocking data task becomes a blocking one because of the lock required for the schema change, for example.
When altering indexes make sure to use CONCURRENT wherever possible.

See the following links for an idea of what is likely to create blocking locks:
- https://www.citusdata.com/blog/2018/02/15/when-postgresql-blocks/
- https://www.postgresql.org/docs/9.4/mvcc-intro.html

(Re-)Creating the database locally
-------------
 The following instructions work on linux, but other OS's may require you to use a different command to connect to psql:
 (replace `sudo -u postgres psql` with the appropriate command to connect to postgres)
 ```
 sudo -u postgres psql
     DROP DATABASE IF EXISTS claimant;
     DROP USER IF EXISTS claimant_admin;
     CREATE USER claimant_admin WITH ENCRYPTED PASSWORD 'claimant_admin';
     CREATE DATABASE claimant;
     GRANT ALL PRIVILEGES ON DATABASE claimant TO claimant_admin;
     \q
 ```

Gradle Commands
-------------

Please note that database upgrades should normally be performed by the application during deployment, so **you won't need to run any of the commands listed below**.

- To run migrations stand-alone: `gradle flywayMigrate` (when run from the top-level project, must be run as: `./gradlew htbhf-claimaint-service-db:flywayMigrate`)
- run `gradle -PdbName=claimant-test db:migrate` to upgrade the database
- run `gradle -PdbName=claimant-test db:drop` to drop the database
- run `gradle -PdbName=claimant-test db:create` to create the database
- run `gradle -PdbName=claimant-test db:reset` to drop,create,migrate the database
- note: you don't need to override the dbName property, default is 'claimant' and sits in `gradle.properties` file

If you get an error for create, reset, drop tasks and it looks similar to this:
 `createdb: could not connect to database template1: FATAL:  role "xyz" does not exist
  :db:create FAILED`

Then you will need to create the user xyz. Following are the steps to do that:   

- Switch to the postgres user `sudo -i -u postgres`
- Create user - `createuser -s -r -d xyz` (xyz becomes a super-user, has permission to create new databases and roles)

Now try running the tasks again. Note that this is only necessary to run the create, reset and drop tasks. 


Accessing PaaS databases
-------------
Sourced from: https://docs.cloud.service.gov.uk/deploying_services/postgresql/#connect-to-a-postgresql-service-from-your-local-machine

Having logged into the Paas:
```
cf login -a ${CF_API} -u ${CF_USER} -p "${CF_PASS}" -s ${CF_SPACE} -o ${CF_ORG}
```
(And having already created the database: `cf create-service postgres small-ha-10.5 htbhf-claimant-service-postgres`)

And installed the `conduit` plugin:
```
cf install-plugin -r CF-Community "conduit"
```
To use the psql command line, run: (you will need to have `psql` installed - see https://www.postgresql.org/download/)
```
cf conduit htbhf-claimant-service-postgres -- psql
```
Or, to connect the conduit and use a different application:
```
cf conduit htbhf-claimant-service-postgres
```
The output will provide connection details - the username & password change every time.
Note that the jdbcuri includes `&ssl=true` - which should be removed for a successful connection.

Querying the db for claimants
-------------
If you have successfully run the psql command over the cf conduit (see above), then you can run the following sql to select all claimants in the db:
```
\copy (select * from claimant inner join address on claimant.card_delivery_address_id = address.id) to '/path/to/file.csv' DELIMITER ',' CSV HEADER;
```

Setting the preferred maintenance window
------------
Source from https://docs.cloud.service.gov.uk/deploying_services/postgresql/#postgresql-maintenance-amp-backups

Having logged into the Paas:
```
cf login -a ${CF_API} -u ${CF_USER} -p "${CF_PASS}" -s ${CF_SPACE} -o ${CF_ORG}
```
(And having already created the database: `cf create-service postgres small-ha-10.5 htbhf-claimant-service-postgres`)

Set the preferred maintenance window (For production this is currently every Sunday between 03:00 am and 3:30 am)
```
cf update-service htbhf-claimant-service-postgres -c '{"preferred_maintenance_window": "Sun:03:00-Sun:03:30"}'
```

Emptying the database
----------------------

The development and staging spaces may need their database emptied from time to time, as the scheduled services will continue to send emails and letters for all active claims.
To do so, connect to the database as described above then run the following commands:
```
TRUNCATE TABLE message_queue, payment, payment_cycle, claim, claimant, address CASCADE;
TRUNCATE TABLE schedule_tasks_locks, jv_snapshot, jv_global_id, jv_commit_property, jv_commit CASCADE;
```

Claim auditing
-----------
All changes to claims are audited using the [javers](https://javers.org/) library. 

* jv_global_id — domain object identifiers,
* jv_commit — Javers commits metadata,
* jv_commit_property — commit properties,
* jv_snapshot — domain object snapshots

Below is an example of how to view changes to claims (does not include new values being set or child entities):
```
// view all changes to all claims
JqlQuery jqlQuery = QueryBuilder.byClass(Claim.class).build();
Changes changes = javers.findChanges(jqlQuery);
System.out.println(javers.getJsonConverter().toJson(changes));

//view all changes to individual claims
JqlQuery jqlQuery = QueryBuilder.byInstanceId(claim.getId(), Claim.class).build();
Changes changes = javers.findChanges(jqlQuery);
System.out.println(javers.getJsonConverter().toJson(changes));
```

To see changes including new values being set use `withNewObjectChanges`
```
JqlQuery jqlQuery = QueryBuilder.byClass(Claim.class)
                    .withNewObjectChanges()
                    .build();
Changes changes = javers.findChanges(jqlQuery);
System.out.println(javers.getJsonConverter().toJson(changes))
```

Note, child entities are not included in the changes and must be queried directly. For example, if we wanted to see changes to an address of a claim:
```
JqlQuery jqlQuery = QueryBuilder.byInstanceId(claim.getClaimant().getAddress().getId(), Address.class).build();
Changes changes = javers.findChanges(jqlQuery);
System.out.println(javers.getJsonConverter().toJson(changes))
```
