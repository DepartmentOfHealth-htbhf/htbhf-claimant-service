db
=============

This sub-project is responsible for the definition of the Database Schema, using the Postgresql dialect.


DB upgrade scripts
-------------

Database migrations will be performed by [Flyway](https://flywaydb.org/documentation/),
using migration scripts in src/main/resources/db.migration.
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


Commands
-------------

Please note that database upgrades should normally be performed by the application during deployment, so you won't need to run any of the commands listed below.

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
If you prefer to do this manually:
```
sudo -u postgres psql
    DROP DATABASE IF EXISTS claimant;
    DROP USER IF EXISTS claimant_admin;
    CREATE USER claimant_admin WITH ENCRYPTED PASSWORD 'claimant_admin';
    CREATE DATABASE claimant;
    GRANT ALL PRIVILEGES ON DATABASE claimant TO claimant_admin;
    \q
```

Accessing PaaS databases
-------------
Sourced from: https://docs.cloud.service.gov.uk/deploying_services/postgresql/#connect-to-a-postgresql-service-from-your-local-machine

Having logged into the Paas:
```
cf login -a ${CF_API} -u ${CF_USER} -p "${CF_PASS}" -s ${CF_SPACE} -o ${CF_ORG}
```
And installed the `conduit` plugin:
```
cf install-plugin conduit
```
To use the psql command line, run:
```
cf conduit htbhf-claimant-service-postgres -- psql
```
Or, to connect the conduit and use a different application:
```
cf conduit htbhf-claimant-service-postgres
```
The output will provide connection details - the username & password change every time.
Note that the jdbcuri includes `&ssl=true` - which should be removed for a successful connection.
