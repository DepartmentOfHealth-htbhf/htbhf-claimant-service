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

- To run migrations: `gradle db:flyM`
- run `gradle -PdbName=products-test db:migrate` to upgrade the database
- run `gradle -PdbName=products-test db:drop` to drop the database
- run `gradle -PdbName=products-test db:create` to create the database
- run `gradle -PdbName=products-test db:reset` to drop,create,migrate the database
- note: you don't need to ovveride the dbName property, default is 'products' and sits in `gradle.properties` file

If you get an error for create, reset, drop tasks and it looks similar to this:
 `createdb: could not connect to database template1: FATAL:  role "xyz" does not exist
  :db:create FAILED`

Then you will need to create the user xyz. Following are the steps to do that:   

- Switch to the postgres user `sudo -i -u a`
- Create user - `createuser -s -r -d xyz` (xyz becomes a super-user, has permission to create new databases and roles)

Now try running the tasks again.
