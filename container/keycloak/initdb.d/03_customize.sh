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