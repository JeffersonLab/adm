#!/bin/bash

# Located in root of container
. /kc-lib.sh

echo "----------------"
echo "| Create Roles |"
echo "----------------"
KC_ROLE_NAME=deployer-group
create_role

echo "----------------"
echo "| Assign Roles |"
echo "----------------"
KC_USERNAME=jsmith
assign_role

echo "------------------------------------"
echo "| Allow Password Grant (Test Only) |"
echo "------------------------------------"
# Integration tests get tokens for test users such as tbrown with their passwords
${KC_HOME}/bin/kcadm.sh update clients/${KC_CLIENT_NAME} -r "${KC_REALM}" -s directAccessGrantsEnabled=true
