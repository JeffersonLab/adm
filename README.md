# adm [![CI](https://github.com/JeffersonLab/adm/actions/workflows/ci.yaml/badge.svg)](https://github.com/JeffersonLab/adm/actions/workflows/ci.yaml) [![Docker](https://img.shields.io/docker/v/jeffersonlab/adm?sort=semver&label=DockerHub)](https://hub.docker.com/r/jeffersonlab/adm)
A [Jakarta EE 10](https://en.wikipedia.org/wiki/Jakarta_EE) web application for app deployment at Jefferson Lab built with the [Smoothness](https://github.com/JeffersonLab/smoothness) web template.


![Screenshot](https://github.com/JeffersonLab/adm/raw/main/Screenshot.png?raw=true "Screenshot")

---
- [Overview](https://github.com/JeffersonLab/adm#overview)
- [Quick Start with Compose](https://github.com/JeffersonLab/adm#quick-start-with-compose)
- [Install](https://github.com/JeffersonLab/adm#install)
- [Configure](https://github.com/JeffersonLab/adm#configure)
- [API](https://github.com/JeffersonLab/adm#api)
- [Build](https://github.com/JeffersonLab/adm#build)
- [Develop](https://github.com/JeffersonLab/adm#develop)
- [Release](https://github.com/JeffersonLab/adm#release)
- [Deploy](https://github.com/JeffersonLab/adm#deploy)
- [See Also](https://github.com/JeffersonLab/adm#see-also)
---

## Overview
The automated deployment manager app aids programmatic app deployment by providing an HTTP endpoint for executing 
curated remote deployment commands securely.

## Quick Start with Compose
1. Grab project
```
git clone https://github.com/JeffersonLab/adm
cd adm
```
2. Launch [Compose](https://github.com/docker/compose)
```
docker compose up
```
3. Navigate to page
```
http://localhost:8080/adm
```

**Note**: Login with demo username "tbrown" and password "password".   Use env "local-demo", app "testapp", ver "1.0.0".

### Ports
The containers publish these ports on the host, listening only on its local address (127.0.0.1), so other computers can't reach the demo database, accounts, and management consoles.  To use them from another computer, forward them over SSH, such as `ssh -N -L 8443:localhost:8443 -L 8081:localhost:8081 <host>` for the app and Keycloak.  If another project already uses some of them, or you run a second copy of adm, set other ports in a `.env` file next to `compose.yaml`, which Compose reads, or in the environment:

| Variable | Default | Port of |
|---|---|---|
| ADM_HTTPS_PORT | 8443 | app (HTTPS) |
| ADM_HTTP_PORT | 8080 | app (HTTP) |
| ADM_MANAGEMENT_PORT | 9990 | Wildfly management |
| ADM_KEYCLOAK_PORT | 8081 | Keycloak |
| ADM_KEYCLOAK_MANAGEMENT_PORT | 9991 | Keycloak management |
| ADM_ORACLE_PORT | 1521 | Oracle |
| ADM_ORACLE_EM_PORT | 5500 | Oracle Enterprise Manager |
| ADM_SSHD_PORT | 1234 | sshd (the demo data's "local-dev" env deploys to port 1234) |

For example, a `.env` for a second copy:
```
ADM_HTTPS_PORT=18443
ADM_HTTP_PORT=18080
ADM_MANAGEMENT_PORT=19990
ADM_KEYCLOAK_PORT=18081
ADM_KEYCLOAK_MANAGEMENT_PORT=19991
ADM_ORACLE_PORT=11521
ADM_ORACLE_EM_PORT=15500
ADM_SSHD_PORT=11234
```
Compose names the containers, network, and images after the project's directory, such as `adm-oracle-1`, so a copy in another directory, such as a git worktree, gets its own.  Each copy uses about 4 GB of memory.

## Install
This application requires a Java 17+ JVM and standard library to run, plus a Jakarta EE 10 application server (developed with Wildfly).

1. Install service [dependencies](https://github.com/JeffersonLab/adm/blob/main/deps.yaml)
2. Download [Wildfly 37.0.1](https://www.wildfly.org/downloads/)
3. [Configure](https://github.com/JeffersonLab/adm#configure) Wildfly and start it
4. Download [adm.war](https://github.com/JeffersonLab/workmap/releases) and deploy it to Wildfly
5. Navigate your web browser to [localhost:8080/adm](http://localhost:8080/adm)


## Configure

### Configtime
Wildfly must be pre-configured before the first deployment of the app.  The [wildfly bash scripts](https://github.com/JeffersonLab/wildfly#configure) can be used to accomplish this.  See the [Dockerfile](https://github.com/JeffersonLab/adm/blob/main/Dockerfile) for an example.

### Runtime
Uses a subset of the [Smoothness Environment Variables](https://github.com/JeffersonLab/smoothness#global-runtime) including:
- BACKEND_SERVER_URL
- FRONTEND_SERVER_URL

The deployment commands rely on ssh and therefore the user running Wildlfy needs to have ssh configured such that remote
connections can be made non-interactively (programmatically) without password prompt.  This generally means creating a 
public and private key pair for the Wildfly user via `ssh-keygen` and then add the public key to the `authorized_keys` 
file of each remote user account needed for deployments.   It's also possible to use Kerberos and keytabs instead.

### Database
The application requires an Oracle 19+ database with the following [schema](https://github.com/JeffersonLab/adm/tree/main/container/oracle/initdb.d) installed.   The application server hosting the app must also be configured with a JNDI datasource.

## API
Clients such as CI authenticate with a Keycloak bearer token, usually a service account's from the client credentials grant, as the [Deploy to JLab](https://github.com/JeffersonLab/general-workflows/blob/main/.github/workflows/jlab-deploy-app.yaml) action does.  Only an app env's request service user and users with the `adm-admin` role may deploy to it and see its jobs.

A request without a token is redirected to the login page (HTTP status 302), and one with an invalid or expired token returns HTTP status 401.  Access tokens are short-lived, 5 minutes by default in Keycloak, and a deploy may take longer, so a client that waits for a job should request a new token for each poll.

### Deploy
`POST /adm/deploy` with the form parameters `env`, `app` and `ver` (a semantic version) starts a deploy job and returns its ID at once:
```
{"jobId": 13}
```
A refused deploy returns its reason, with HTTP status 200: `{"exception": "..."}`.

### Job status
`GET /adm/job?id=<jobId>` returns a deploy job's status:
```
{"jobId": 13, "app": "testapp", "env": "local-demo", "version": "1.2.3",
 "start": "2026-10-05T14:12:55-04:00", "end": "2026-10-05T14:12:56-04:00",
 "exitCode": 0, "out": "1.2.3\n", "err": null, "error": null}
```
- `end` and `exitCode` are null while the job runs; poll until `end` is set.  A deploy command is stopped after 10 minutes.  The deploy succeeded if `exitCode` is 0.
- `out` and `err` are the deploy command's standard output and standard error.  `error` says why the command could not run or finish, such as an unknown host or a timeout.  Each is cut to its last 64 KiB, and is null when empty.
- A missing or invalid ID returns HTTP status 400, a caller who may not see the job 403, and an unknown job 404, each with the reason: `{"exception": "..."}`.

For example, to wait for a job, for a little longer than a deploy command may run, with a new token for each poll, and fail unless it succeeded:
```
token() {
  curl -sS --fail-with-body -u "${CLIENT_ID}:${SECRET}" -d "grant_type=client_credentials" "https://ace.jlab.org/auth/realms/ace/protocol/openid-connect/token" | jq -er '.access_token'
}

for i in $(seq 1 130); do
  ACCESS_TOKEN=$(token) || { echo "token request failed"; exit 1; }
  STATUS=$(curl -sS --fail-with-body -H "Authorization: Bearer ${ACCESS_TOKEN}" "https://ace.jlab.org/adm/job?id=${JOB_ID}") || { echo "${STATUS}"; exit 1; }
  [ "$(echo "${STATUS}" | jq '.end')" != "null" ] && break
  sleep 5
done
echo "${STATUS}"
echo "${STATUS}" | jq -e '.exitCode == 0'
```

## Build
This project is built with [Java 21](https://adoptium.net/) (compiled to Java 17 bytecode), and uses the [Gradle 9](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/adm
cd adm
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

## Develop
In order to iterate rapidly when making changes it's often useful to run the app directly on the local workstation, perhaps leveraging an IDE.  In this scenario run the service dependencies with:
```
docker compose -f deps.yaml up
```
**Note**: The local install of Wildfly should be [configured](https://github.com/JeffersonLab/workmap#configure) to proxy connections to services via localhost and therefore the environment variables should contain:
```
KEYCLOAK_BACKEND_SERVER_URL=http://localhost:8081
FRONTEND_SERVER_URL=https://localhost:8443
```
Further, the local DataSource must also leverage localhost port forwarding so the `standalone.xml` connection-url field should be: `jdbc:oracle:thin:@//localhost:1521/xepdb1`.

The [server](https://github.com/JeffersonLab/wildfly/blob/main/scripts/server-setup.sh) and [app](https://github.com/JeffersonLab/wildfly/blob/main/scripts/app-setup.sh) setup scripts can be used to setup a local instance of Wildfly.

The user you use to run Wildfly needs to have an SSH public/private key pair (ssh-keygen) in the default location (~/.ssh).   The public key needs to be added to the `authorized_keys` file of user `testuser` in the container of the `sshd` service.  This can be done by creating a file named `.env` in the root of the project containing the env name "TEST_USER_AUTHORIZED_KEY" with value being Wildfly user's public key.  This env will then be passed in via deps.yaml environment setting.

The unit tests run with `gradlew build`, or alone with `gradlew test`.  They need no database, Wildfly, or containers: the SSH tests start their own in-process SSH server.

The integration tests run against the app and its services in containers, built from source, and are not part of `gradlew build`:
```
docker compose -f build.yaml up -d --build
gradlew integrationTest
```
They wait up to 5 minutes for the services to start (set `ADM_READY_TIMEOUT_SECONDS` to change that), then log in with the demo users and the `adm` client's service account, as CI deploys do.  They add their own app envs to deploy to, and remove them afterwards.  They use the same [ports](#ports) as Compose, including those in `.env`.  To test an app elsewhere, set `ADM_URL` (default `https://localhost:8443/adm`) and `KEYCLOAK_URL` (default `http://localhost:8081/auth`).

## Release
1. Bump the version number in the VERSION file and commit and push to GitHub (using [Semantic Versioning](https://semver.org/)).
2. The [CD](https://github.com/JeffersonLab/adm/blob/main/.github/workflows/cd.yaml) GitHub Action should run automatically invoking:
    - The [Create release](https://github.com/JeffersonLab/java-workflows/blob/main/.github/workflows/gh-release.yaml) GitHub Action to tag the source and create release notes summarizing any pull requests.   Edit the release notes to add any missing details.  A war file artifact is attached to the release.
    - The [Publish docker image](https://github.com/JeffersonLab/container-workflows/blob/main/.github/workflows/docker-publish.yaml) GitHub Action to create a new demo Docker image.
    - The [Deploy to JLab](https://github.com/JeffersonLab/general-workflows/blob/main/.github/workflows/jlab-deploy-app.yaml) GitHub Action to deploy to the JLab test environment.

## Deploy
The deploy to JLab's acctest is handled automatically via the release workflow.

At JLab this app is found at [ace.jlab.org/adm](https://ace.jlab.org/adm) and internally at [acctest.acc.jlab.org/adm](https://acctest.acc.jlab.org/adm).  However, those servers are proxies for `wildfly1.acc.jlab.org` and `wildflytest1.acc.jlab.org` respectively.   A [deploy script](https://github.com/JeffersonLab/wildfly/blob/main/scripts/deploy.sh) is provided on each server to automate wget and deploy.  Example:

```
/opt/wildfly/cd/deploy.sh adm v1.2.3
```

**JLab Internal Docs**:  [RHEL9 Wildfly](https://acgdocs.acc.jlab.org/en/ace/builds/rhel9-wildfly)

## See Also
- [JLab ACE management-app list](https://github.com/search?q=org%3Ajeffersonlab+topic%3Aace+topic%3Amanagement-app&type=repositories)
