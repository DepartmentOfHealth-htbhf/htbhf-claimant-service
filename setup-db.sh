#! /bin/bash
database_name=$1
if [ -z "${database_name}" ]; then
    database_name='claimant'
fi

pretty_echo( ) {
echo ">>> $1 <<<"
}

echo "--------------------------------------------"
pretty_echo "using $database_name as the database"
echo "--------------------------------------------"

# CHECK CONTAINER
check_if_container_is_running() {
  container_running=$(docker ps --filter NAME=htbhf_postgis -q)
}

htbhf_postgis_container_exists=$(docker ps -a --filter NAME=htbhf_postgis -q)
if [ ${htbhf_postgis_container_exists} ]; then
    pretty_echo "container exists"

   check_if_container_is_running
    if [ -z ${container_running} ]; then
      pretty_echo "starting container"
      docker start htbhf_postgis
    fi
else
    pretty_echo "creating postgis container"
    docker run -e "POSTGRES_PASSWORD=password" \
               -p 5432:5432 \
               --name htbhf_postgis -d mdillon/postgis:9.4
fi

check_if_container_is_running
RETRIES=0
until [ ${container_running} ] || [ $RETRIES -eq 10 ]; do
   pretty_echo "waiting for container to start postgis service. Retry: $RETRIES"
   sleep 2
   (( RETRIES++ ))
   check_if_container_is_running
done

if [ ${container_running} ]; then
    pretty_echo "container running"
else
    pretty_echo "postgis container could not be started. Is there another container using the port? If not, try to rerun the script."
    exit 1
fi

# CHECK DATABASE ACCESSIBILITY
check_if_database_is_accessible() {
  docker exec htbhf_postgis su postgres -c "psql -c 'select datname from pg_database;'"
  accessible=$?
}

check_if_database_is_accessible
RETRIES=0
until [ $accessible -eq 0 ] || [ $RETRIES -eq 10 ]; do
   pretty_echo "waiting for database to be accessible. Retry: $RETRIES"
   sleep 2
    (( RETRIES++ ))
   check_if_database_is_accessible
done

if [ $accessible -eq 0 ]; then
    pretty_echo "database accessible"
else
    pretty_echo "database is not accessible - maybe it is not ready yet? Try to rerun the script."
    exit 1
fi

# CREATE THE SUPERUSER
if [ -z ${htbhf_postgis_container_exists} ]; then
   pretty_echo "since this is a new container, we create a superuser role"
   docker exec htbhf_postgis /bin/su postgres -c "psql -c 'CREATE ROLE rds_superuser WITH SUPERUSER;'"
fi

# CHECK THE DATABASE SCHEMA
check_if_database_exists() {
  all_dbs=$(docker exec htbhf_postgis su postgres -c "psql -c 'select datname from pg_database;'")
  db_exists=`echo "${all_dbs}" | grep "${database_name}$"`
}

check_if_database_exists
if [ -n "$db_exists"  ]; then
    pretty_echo "$database_name database exists"
else
    pretty_echo "creating $database_name database"
    docker exec htbhf_postgis /bin/su postgres -c "createdb $database_name -U postgres"
fi

check_if_database_exists
RETRIES=0
until [ -n "$db_exists" ] || [ $RETRIES -eq 10 ]; do
   pretty_echo "waiting for database to be available. Retry: $RETRIES"
   sleep 2
    (( RETRIES++ ))
   check_if_database_exists
done
if [ -n "$db_exists" ]; then
    pretty_echo "$database_name database exists"
else
    pretty_echo "database doesn't exists - maybe it is not ready yet? Try to rerun the script."
    exit 1
fi

pretty_echo "run flywayMigrate on $database_name"
gradle "-Dflyway.url=jdbc:postgresql://localhost:5432/$database_name" db:flyMigrate

pretty_echo "if flyway failed, please re-run the script"
