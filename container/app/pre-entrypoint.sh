#!/bin/bash

##
# Passing in a demo/test private key is tricky because:
# 1. When using env var multi-line yaml is tricky (many multi-line rules such as '|-')
# 2. If you instead try to use bind mount of file you'll discover getting file permissions and ownership is tricky
# and the Apache Mina SSH client in particular will silently refuse to work without invoker owner and UNIX 600.
# (Requires using -vvv on cli mina client in /tmp/apache-mina.../bin/ssh.sh to see this.
#
# Since this is a test/demo key, we could probably just bundle it in Docker build.
##

if [ -n "${RSA_PUBLIC_KEY}" ]; then
  echo "Populating /home/dev/.ssh/id_rsa.pub with the value from RSA_PUBLIC_KEY env variable ..."
  echo "${RSA_PUBLIC_KEY}" > /home/dev/.ssh/id_rsa.pub
  chown dev /home/dev/.ssh/id_rsa.pub
  chmod 600 /home/dev/.ssh/id_rsa.pub
fi

if [ -n "${RSA_PRIVATE_KEY}" ]; then
  echo "Populating /home/dev/.ssh/id_rsa with the value from RSA_PRIVATE_KEY env variable ..."
  echo "${RSA_PRIVATE_KEY}" > /home/dev/.ssh/id_rsa
  chown dev /home/dev/.ssh/id_rsa
  chmod 600 /home/dev/.ssh/id_rsa
fi

# Now call the upstream jeffersonlab/wildfly entrypoint
/container-entrypoint.sh