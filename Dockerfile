ARG BUILD_IMAGE=gradle:7.4-jdk17
ARG RUN_IMAGE=jeffersonlab/wildfly:1.6.2

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
COPY --from=builder /app/container/app/app-setup.env /
COPY --from=builder /app/container/app/pre-entrypoint.sh /
USER root
RUN /server-setup.sh /app-setup.env wildfly_start_and_wait \
     && /app-setup.sh /app-setup.env config_keycloak_client \
     && /app-setup.sh /app-setup.env config_oracle_client \
     && /server-setup.sh /app-setup.env config_email \
     && /server-setup.sh /app-setup.env wildfly_reload \
     && /server-setup.sh /app-setup.env wildfly_stop \
     && rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history \
     && mkdir /opt/jboss/.ssh \
     && chown -R jboss:jboss /opt/jboss \
     && chmod 0700 /opt/jboss/.ssh \
     && cd /tmp \
     && curl -O https://dlcdn.apache.org/mina/sshd/2.12.1/apache-sshd-2.12.1.tar.gz \
     && tar -xvzf apache-sshd-2.12.1.tar.gz \
     && chmod +x apache-sshd-2.12.1/bin/ssh.sh

# old centos yum is busted: https://serverfault.com/a/1161847/107456
RUN sed -i s/mirror.centos.org/vault.centos.org/g /etc/yum.repos.d/CentOS-*.repo \
    && sed -i s/^#.*baseurl=http/baseurl=http/g /etc/yum.repos.d/CentOS-*.repo \
    && sed -i s/^mirrorlist=http/#mirrorlist=http/g /etc/yum.repos.d/CentOS-*.repo \
    && yum install openssh-clients -y

USER jboss
COPY --from=builder /app/build/libs/* /opt/jboss/wildfly/standalone/deployments
ENTRYPOINT /pre-entrypoint.sh

FROM runner AS dev
USER root
RUN yum install git -y