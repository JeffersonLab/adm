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

echo "-------------------"
echo "| Token Audience  |"
echo "-------------------"
# ADM takes only bearer tokens whose audience names its client (verify-token-audience in the
# Dockerfile), and Keycloak doesn't name a client in the tokens it issues to that client itself.
# A client that CI deploys with, if not this one, needs the same mapper
${KC_HOME}/bin/kcadm.sh create clients/${KC_CLIENT_NAME}/protocol-mappers/models -r "${KC_REALM}" \
  -s name=adm-audience \
  -s protocol=openid-connect \
  -s protocolMapper=oidc-audience-mapper \
  -s 'config."included.client.audience"='"${KC_CLIENT_NAME}" \
  -s 'config."access.token.claim"=true' \
  -s 'config."id.token.claim"=false'

echo "--------------------------------------"
echo "| Another Application (Test Only)    |"
echo "--------------------------------------"
# A second client in the realm, as a shared realm has. The integration tests check that ADM
# refuses the tokens Keycloak issues to it
${KC_HOME}/bin/kcadm.sh create clients -r "${KC_REALM}" \
  -s clientId=other-app \
  -s id=other-app \
  -s enabled=true \
  -s serviceAccountsEnabled=true \
  -s directAccessGrantsEnabled=true \
  -s secret=other-app-test-only-secret
