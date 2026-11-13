# Inventory

[User Guide](README.md) › Inventory

The **Inventory** tab has two pages: the **Deployment Allowlist**, of each app's envs, and the
**Apps**. Anyone who logs in can look at them; administrators (the `adm-admin` role) change them.

Both pages list 10 records at a time, or 100 with **Max Per Page**, and **Choose...** opens their
filters. Filters ignore case, and `*` is a wildcard: `dtm*` matches `dtm` and `dtm-test`.

## Deployment Allowlist

Each record allows its **Request Username** to deploy an app to an env: ADM runs the **Deploy
Command** as the **Deploy Username** on each **Deploy Hostname**. See
[The Deployment Allowlist](concepts.md#the-deployment-allowlist) for what each field means.

The filters are **App Name**, **Env Name** and **Deploy Hostname**. A hostname filter finds an env
that lists that host among several.

### Changing the allowlist

Select a row to **Edit**, **Copy Existing** or **Remove** it, or select **Add** for a new record;
**Unselect** clears the selection. Add, Edit and Copy open the same form:

| Field | Notes |
|---|---|
| **App Name** | Choose an existing app: click the field to list them, or type to search. To add the app as well, check **Create new App** (on Add only), type its name, and give its **App Doc URL** if it has one; the app is created with the record. |
| **Env Name** | Unique within the app. |
| **Request Username** | The Keycloak account allowed to request this deploy, usually a CI service account. Where the presets are set up, choose **GitHub Actions** or **GitLab CI** to fill in that CI's account, or **Custom account** to type one. |
| **Deploy Username** | The account on the hosts that ADM logs in to over SSH. |
| **Deploy Hostname** | One host, or several separated by commas: `host1, host2`. ADM deploys to them in that order, and stops at the first that fails. A host can't be listed twice. |
| **Deploy Port** | The hosts' SSH port: 22 unless they use another. |
| **Deploy Command** | What ADM runs on each host, with a space and the version added to the end. See [The deploy command](concepts.md#the-deploy-command). |

Every field but the App Doc URL is required. Select **Save**; if ADM refuses the record, it says
why, such as an env name the app already has, and the form stays open to correct it.

- **Edit** changes the record in place, and its jobs stay with it. It can move the record to
  another existing app, but not create one; to rename an app, use the [Apps](#apps) page.
  Renaming an env changes the name that deploy requests must use: update the pipelines that
  deploy it.
- **Copy Existing** opens Add filled in from the selected record, for a new env of the same app or
  the same env of another app. Change the App Name or the Env Name, check the other fields, and
  save. The original record isn't changed, and the copy starts with no jobs.
- **Remove** asks to confirm, then deletes the record **and its jobs**: they leave the Log. No one
  can deploy that app to that env afterwards.

Administrators may deploy any record, so an administrator can try a new record from the
[Choose](deploying.md#requesting-a-deploy) tab before a pipeline uses it. It is a real deploy.

## Apps

An app's name links to its documentation, when it has a **Doc URL**. The filter is **Name**.

Select **Add** to add an app with its **Name** and **Doc URL**, or select a row to **Edit** or
**Remove** it. An app is usually added along with its first env, from the allowlist's
**Create new App**.

- App names are unique, ignoring case.
- Renaming an app changes the name that deploy requests must use, for all its envs: update the
  pipelines that deploy it.
- **Removing an app removes all its envs and all their jobs.** To stop deploys to one env, remove
  that env's allowlist record instead.

## Read-only from other networks

A site can allow inventory edits only from certain networks, such as its own. An administrator
connecting from elsewhere sees the Inventory read-only, with a note such as "Read-only: The
inventory can be edited only from 192.0.2.0/24, and you are connecting from 198.51.100.7". Connect
from one of the networks listed, for example through the site's VPN, to make changes. Deploys
aren't limited this way. The networks are set in the server's configuration, not on the Settings
page; see [Server configuration](administration.md#server-configuration).
