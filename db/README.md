db
=============

Responsibility
-------------

Definition of the Database Schema


Collaboration
-------------

- Postgres Database


Commands
-------------

- To run migrations: `gradle flywayMigrate` (when run from the top-level project, must be run as: `./gradlew htbhf-claimaint-service-db:flywayMigrate`)
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
