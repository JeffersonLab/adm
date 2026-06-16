ARG BUILD_IMAGE=gradle:9-jdk21
ARG RUN_IMAGE=jeffersonlab/wildfly:3.0.1
ARG SSHD_VER=2.18.0

################## Stage 0
FROM ${BUILD_IMAGE} AS builder
ARG CUSTOM_CRT_URL
USER root
WORKDIR /
RUN if [ -z "${CUSTOM_CRT_URL}" ] ; then echo "No custom cert needed"; else \
       wget -O /usr/local/share/ca-certificates/customcert.crt $CUSTOM_CRT_URL \
       && update-ca-certificates \
       && keytool -import -alias custom -file /usr/local/share/ca-certificates/customcert.crt -cacerts -storepass changeit -noprompt \
       && export OPTIONAL_CERT_ARG=--cert=/etc/ssl/certs/ca-certificates.crt \
    ; fi
COPY . /app
RUN cd /app && gradle build -x test --no-watch-fs $OPTIONAL_CERT_ARG

################## Stage 1
FROM ${RUN_IMAGE} AS runner
ARG SSHD_VER
COPY --from=builder /app/container/app/app-setup.env /
COPY --from=builder /app/container/app/pre-entrypoint.sh /
USER root
RUN /server-setup.sh /app-setup.env wildfly_start_and_wait \
     && /app-setup.sh /app-setup.env config_keycloak_client \
     && /app-setup.sh /app-setup.env config_oracle_client \
     && /server-setup.sh /app-setup.env config_email \
     && /server-setup.sh /app-setup.env wildfly_reload \
     && /server-setup.sh /app-setup.env wildfly_stop \
     && rm -rf /opt/wildfly/current/standalone/configuration/standalone_xml_history

RUN mkdir -p /home/dev/.ssh \
     && chown -R dev /home/dev \
     && chmod 0700 /home/dev/.ssh \
     && cd /tmp \
     && curl -fO https://dlcdn.apache.org/mina/sshd/${SSHD_VER}/apache-sshd-${SSHD_VER}.tar.gz \
     && tar -xvzf apache-sshd-${SSHD_VER}.tar.gz \
     && chmod +x apache-sshd-${SSHD_VER}/bin/ssh.sh

USER dev
COPY --from=builder /app/build/libs/* /opt/wildfly/current/standalone/deployments
ENTRYPOINT ["/pre-entrypoint.sh"]

ENV TZ='America/New_York'

# Used by app runtime smoothness weblib User Directory Cache
ENV KEYCLOAK_FRONTEND_SERVER_URL='http://localhost:8081/auth'
ENV KEYCLOAK_BACKEND_SERVER_URL='http://keycloak:8080/auth'
ENV KEYCLOAK_REALM='test-realm'
ENV KEYCLOAK_RESOURCE='adm'
ENV KEYCLOAK_SECRET='yHi6W2raPmLvPXoxqMA7VWbLAA2WN0eB'

# Used by container-entrypoint.sh
ENV ORACLE_DATASOURCE='adm'
ENV ORACLE_SERVER='oracle:1521'
ENV ORACLE_USER='ADM_OWNER'
ENV ORACLE_PASS='password'
ENV ORACLE_SERVICE='xepdb1'

# Used by app for path building
ENV BACKEND_SERVER_URL='http://adm:8080'
ENV FRONTEND_SERVER_URL='https://localhost:8443'