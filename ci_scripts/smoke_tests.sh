#!/bin/bash

echo "Running smoke tests against $1"
cd smoke_tests
./gradlew clean test -Dbase_url=$1 --info