# Automated Deployment Manager User Guide

The Automated Deployment Manager (ADM) deploys a new version of an app for you: given an app, an
env (environment) and a version, it logs in to the env's hosts over SSH and runs the deploy
command an administrator set up for them. Nobody who requests a deploy needs an account on those
hosts, and nobody can run anything there but the curated command. CI pipelines request most
deploys, through ADM's API; people can also request one on the **Choose** tab. Each deploy is a
**job**, and the **Log** tab keeps every job's output.

What may be deployed where, and by whom, is the **Deployment Allowlist**, on the **Inventory**
tab. Administrators keep it.

At JLab, ADM is at [ace.jlab.org/adm](https://ace.jlab.org/adm). Log in to use it.

The [guide's website](https://jeffersonlab.github.io/adm/) has a copy for each release; pick the
version the Help tab shows. On GitHub, this guide describes the `main` branch, which may be newer
than the version you are using.

## Contents

1. [Concepts](concepts.md): apps, envs, the allowlist, deploy jobs, and who may do what.
2. [Deploying](deploying.md): requesting a deploy on the Choose tab, and reading its job on the
   Log tab.
3. [Deploying from CI](ci.md): requesting deploys from a pipeline with the API, and the JLab
   GitHub Action that does it.
4. [Inventory](inventory.md): the Deployment Allowlist and Apps, and how administrators change
   them.
5. [Administration](administration.md): the Setup tab, settings and server configuration. For
   administrators.

## Tabs at a glance

| Tab | What it is for |
|---|---|
| **Choose** | Request a deploy: an env, an app and a version. |
| **Log** | Every deploy job, newest first, with its exit code and output. |
| **Inventory** | The **Deployment Allowlist** (each app's envs, who may deploy to them, and how) and the **Apps**. Administrators edit them here; others can look. |
| **Setup** | Administrators only: settings and the directory cache. |
| **Help** | Version, contacts, documentation and a feedback form. |

## Quick start

To deploy an app by hand:

1. Check that the allowlist has the app and env: **Inventory › Deployment Allowlist**. You must
   be its **Request Username**, or an administrator.
2. On **Choose**, enter the **Environment**, the **Application** and the **Version**, such as
   `1.2.3`, and select **Deploy**.
3. Follow the job's link to the **Log**, and reload it until the job has an end time. Exit code 0
   means the deploy succeeded.

To have CI deploy each release, ask an administrator to add an allowlist record for the app and
env with your CI's service account as the Request Username; then see
[Deploying from CI](ci.md).

## Questions and changes

Use the feedback form on the Help tab, or contact the content and technical contacts listed
there. To suggest a change to this guide, open an issue or pull request on
[GitHub](https://github.com/JeffersonLab/adm).
