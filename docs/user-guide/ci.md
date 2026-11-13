# Deploying from CI

[User Guide](README.md) › Deploying from CI

A pipeline deploys through ADM's HTTP API, as a Keycloak service account. This page is for the
people who set up an app's pipeline.

## Setting it up

1. **A service account.** The pipeline needs a Keycloak client with a service account, and its
   client ID and secret stored as CI secrets. Its username is `service-account-` followed by the
   client ID.
2. **An allowlist record.** Ask an ADM administrator to add a
   [Deployment Allowlist](inventory.md#deployment-allowlist) record for each app and env the
   pipeline deploys, with the service account's username as the **Request Username**.
3. **The token's audience.** Where ADM checks that a token was issued for it, the client needs an
   audience mapper that adds ADM's client to its tokens; the Keycloak administrators add it. See
   the README's [Token audience](https://github.com/JeffersonLab/adm#token-audience).
4. **The pipeline.** On GitHub, call the JLab GitHub Action below; elsewhere, make the
   [requests](#the-api) yourself.

## The JLab GitHub Action

JeffersonLab's
[Deploy to JLab](https://github.com/JeffersonLab/general-workflows/blob/main/.github/workflows/jlab-deploy-app.yaml)
reusable workflow requests a deploy from ADM at `ace.jlab.org`. A release job can call it with the
release's version:

```yaml
  acctest_deploy:
    needs:
      - release
    uses: jeffersonlab/general-workflows/.github/workflows/jlab-deploy-app.yaml@v2
    with:
      app: myapp
      env: acctest
      ver: ${{ needs.release.outputs.semver }}
    secrets: inherit
```

It reads the service account's client ID and secret from the `DEPLOYER_USERNAME` and
`DEPLOYER_PASSWORD` secrets. It starts the job and prints ADM's answer, but doesn't wait for the
job: check the job's result on the [Log tab](deploying.md#the-log-tab), or
[wait for it](#waiting-for-a-job) in your own step.

## The API

Every request carries the service account's access token as a bearer token:
`Authorization: Bearer <token>`. A request without a token is redirected to the login page (HTTP
status 302), and one with an invalid or expired token gets HTTP status 401. Keycloak's access
tokens last 5 minutes by default, and a deploy may take longer, so request a new token for each
call.

### Deploy

`POST /adm/deploy` with the form parameters `env`, `app` and `ver` starts a job and returns its
number at once:

```json
{"jobId": 13}
```

A refused deploy returns HTTP status 200 with the reason, the same ones the
[Choose tab](deploying.md#requesting-a-deploy) shows:

```json
{"exception": "User service-account-ci is not authorized to deploy app myapp to env prod"}
```

Check for `jobId` in the answer, not only the HTTP status.

### Job status

`GET /adm/job?id=<jobId>` returns the job, if the caller may deploy to its env:

```json
{"jobId": 13, "app": "myapp", "env": "acctest", "version": "1.2.3",
 "start": "2026-10-05T14:12:55-04:00", "end": "2026-10-05T14:12:56-04:00",
 "exitCode": 0, "out": "1.2.3\n", "err": null, "error": null}
```

- `end` and `exitCode` are null while the job runs. The deploy succeeded if `exitCode` is 0.
- `out` and `err` are the command's standard output and standard error, and `error` says why the
  command couldn't run or finish, such as an unknown host or a timeout. Each is cut to its last
  64 KiB, and is null when empty.
- For an env with [several hosts](concepts.md#several-hosts), `exitCode` is the last host's to
  run, and `error` names the host that failed and the hosts not deployed to.
- A missing or invalid ID returns HTTP status 400, a caller who may not see the job 403, and an
  unknown job 404, each with `{"exception": "..."}`.

### Waiting for a job

ADM stops a deploy command after 10 minutes, so poll for a little longer than that, with a new
token for each poll, and fail unless the deploy succeeded:

```bash
token() {
  curl -sS --fail-with-body -u "${CLIENT_ID}:${SECRET}" -d "grant_type=client_credentials" \
    "https://ace.jlab.org/auth/realms/ace/protocol/openid-connect/token" | jq -er '.access_token'
}

ACCESS_TOKEN=$(token) || { echo "token request failed"; exit 1; }
ANSWER=$(curl -sS --fail-with-body -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d "env=${ENV}" -d "app=${APP}" -d "ver=${VER}" "https://ace.jlab.org/adm/deploy") || { echo "${ANSWER}"; exit 1; }
JOB_ID=$(echo "${ANSWER}" | jq -er '.jobId') || { echo "${ANSWER}"; exit 1; }

for i in $(seq 1 130); do
  ACCESS_TOKEN=$(token) || { echo "token request failed"; exit 1; }
  STATUS=$(curl -sS --fail-with-body -H "Authorization: Bearer ${ACCESS_TOKEN}" "https://ace.jlab.org/adm/job?id=${JOB_ID}") || { echo "${STATUS}"; exit 1; }
  [ "$(echo "${STATUS}" | jq '.end')" != "null" ] && break
  sleep 5
done
echo "${STATUS}"
echo "${STATUS}" | jq -e '.exitCode == 0'
```
