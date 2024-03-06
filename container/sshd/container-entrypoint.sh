#!/bin/bash

if [ -n "${TEST_USER_AUTHORIZED_KEY}" ]; then
  echo "Populating /home/testuser/.ssh/authorized_keys with the value from TEST_USER_AUTHORIZED_KEY env variable ..."
  echo "${TEST_USER_AUTHORIZED_KEY}" > /home/testuser/.ssh/authorized_keys
fi

/usr/sbin/sshd -e -D

sleep infinity