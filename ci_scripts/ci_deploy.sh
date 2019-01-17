#!/bin/bash

# if this is a pull request or branch (non-master) build, then just exit
echo "TRAVIS_PULL_REQUEST=$TRAVIS_PULL_REQUEST, TRAVIS_BRANCH=$TRAVIS_BRANCH"
if [[ "$TRAVIS_PULL_REQUEST" != "false"  || "$TRAVIS_BRANCH" != "master" ]]; then
   echo "Not deploying pull request or branch build"
   exit
fi

check_variable_is_set(){
    if [[ -z ${!1} ]]; then
        echo "$1 must be set and non empty"
        exit 1
    fi
}

check_variable_is_set APP_NAME
check_variable_is_set BIN_DIR
check_variable_is_set GH_WRITE_TOKEN

# download the deployment script(s)
echo "Installing deploy scripts"
mkdir -p ${BIN_DIR}
rm -rf ${BIN_DIR}/deployment-scripts
mkdir ${BIN_DIR}/deployment-scripts

curl -H "Authorization: token ${GH_WRITE_TOKEN}" -s https://api.github.com/repos/DepartmentOfHealth-htbhf/htbhf-deployment-scripts/releases/latest \
| grep zipball_url \
| cut -d'"' -f4 \
| wget -qO deployment-scripts.zip -i -

unzip deployment-scripts.zip
mv -f DepartmentOfHealth-htbhf-htbhf-deployment-scripts-*/* ${BIN_DIR}/deployment-scripts
rm -rf DepartmentOfHealth-htbhf-htbhf-deployment-scripts-*
rm deployment-scripts.zip

export SCRIPT_DIR=${BIN_DIR}/deployment-scripts

# determine APP_PATH
export APP_VERSION=`cat version.properties | grep "version" | cut -d'=' -f2`
export APP_PATH="api/build/libs/$APP_NAME-$APP_VERSION.jar"

# run the deployment script
/bin/bash ${SCRIPT_DIR}/deploy.sh