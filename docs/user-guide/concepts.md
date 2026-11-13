# Concepts

[User Guide](README.md) › Concepts

## Why ADM

Deploying an app usually means logging in to its servers and running a script there. ADM does
that step for whoever is allowed to ask, most often a CI pipeline at the end of a release, so the
pipeline needs no SSH key or account on the servers. The only thing ADM runs on a server is the
command an administrator set for that app and env, with the requested version added, and every
run is recorded.

## Apps and envs

An **app** is something ADM deploys, such as `dtm`, with an optional link to its documentation.
App names are unique, ignoring case.

An **env** (environment) is one place an app is deployed to, such as `dev`, `test` or `prod`. Env
names belong to their app: `dtm` and `workmap` can each have a `dev` env, but one app can't have
two.

## The Deployment Allowlist

Each env is one record of the **Deployment Allowlist**, on the
[Inventory](inventory.md#deployment-allowlist) tab. The record says who may deploy the app to the
env, and how ADM deploys it:

| Field | Meaning |
|---|---|
| **App Name**, **Env Name** | The app and env, as a deploy request names them. |
| **Request Username** | The account allowed to request this deploy from ADM, usually a CI service account. Administrators may deploy any record too. |
| **Deploy Username** | The account on the hosts that ADM logs in to over SSH. |
| **Deploy Hostname** | One or more hosts, separated by commas. See [Several hosts](#several-hosts). |
| **Deploy Port** | The hosts' SSH port, usually 22. |
| **Deploy Command** | The command ADM runs on each host, with the version added. See [The deploy command](#the-deploy-command). |

If there is no record for an app and env, nobody can deploy that app there.

## Deploy jobs

A deploy request names an env, an app and a version. If the record exists, the requester may use
it and the version is valid, ADM starts a **job** and answers at once with the job's number; the
command runs in the background. The **Log** tab and the API's job status then show the job:

| | |
|---|---|
| **Start**, **End** | When the job started and finished. A job without an end is still running. |
| **Exit code** | The deploy command's exit code. **0 means the deploy succeeded**; anything else means it failed. None if the command couldn't run or finish. |
| **Stdout**, **Stderr** | What the command printed. |
| **Stack trace** (**error** in the API) | Why the command couldn't run or finish, such as an unknown host, a refused login or a timeout. |

A job that hasn't ended after **10 minutes** is stopped and fails with a timeout. ADM runs the
command without any input, so a command that asks a question, such as whether to trust a host's
key or certificate, fails at once rather than waiting.

ADM keeps every job, and anyone who can log in can read them all on the Log tab, output included.
Deploy commands shouldn't print passwords or other secrets.

### Versions

The version must be a [semantic version](https://semver.org/), such as `1.2.3` or `2.0.0-rc.1`,
without a leading `v`. ADM refuses anything else, which also keeps a request from adding its own
shell commands to the deploy command.

### The deploy command

ADM adds a space and the version to the end of the record's Deploy Command, and runs the result as
the Deploy Username on each host. For example, with the command `/opt/wildfly/cd/deploy.sh dtm` and
the version `1.2.3`, ADM runs:

```
/opt/wildfly/cd/deploy.sh dtm 1.2.3
```

The command's exit code decides whether the deploy succeeded, so the script it runs should exit
with a non-zero code when the deploy fails.

### Several hosts

A Deploy Hostname such as `epicsweb1, epicsweb2, epicsweb3` is for an app that runs on each of
them. ADM runs the command on each host in turn, in the order listed, and stops at the first host
where it fails, so a version that fails on one host doesn't reach the rest. It is still one job:

- It succeeds only if the command succeeded on every host. Its exit code is the last host's to
  run.
- The 10 minutes are for all the hosts together.
- Each host's part of the output starts with a line naming the host, such as `== epicsweb1 ==`.
- If a host fails, the error names it and the hosts not deployed to.

## Roles

| Who | Can |
|---|---|
| Anyone who logs in | See the Choose, Log and Inventory tabs, all jobs and their output. Deploy to an env only as its record's Request Username. |
| A record's **Request Username** | Deploy that app to that env, and see its jobs' status through the API. |
| Administrators (`adm-admin` role) | Deploy any record, edit the Inventory, and use the Setup tab. |

At a site that limits inventory edits to certain networks, administrators connecting from
elsewhere see the Inventory read-only; see [Inventory](inventory.md#read-only-from-other-networks).
