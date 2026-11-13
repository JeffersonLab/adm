# Administration

[User Guide](README.md) › Administration

This page is for ADM administrators: users with the `adm-admin` role, who see the **Setup** tab,
may deploy any allowlist record and edit the [Inventory](inventory.md).

## Setup tab

| Page | For |
|---|---|
| **Settings** | The app's settings; see below. Select a row to edit its value. |
| **Directory Cache** | ADM caches users' names and roles, and clears the cache every night at midnight. Clear it, or look up a user or role, after a change in the directory. Users must log out and in again for a role change to apply. |

## Settings

**Setup › Settings** lists the settings with their descriptions; **Choose...** filters them by
tag. Some changes can break the app or the Settings page itself, so that they can only be undone in
the database. Changes made in the database directly take effect after **Refresh Cache** or a
redeploy, and `IP_READ_FILTER_ENABLED` and `IP_READ_URL_PATTERN` always need a redeploy.

| Setting | Effect |
|---|---|
| `GITHUB_REQUEST_USERNAME`, `GITLAB_REQUEST_USERNAME` | The service accounts that the allowlist form offers as the **GitHub Actions** and **GitLab CI** Request Username presets. A preset is offered only if its setting exists. |
| `NOTIFICATION_ENABLED`, `NOTIFICATION_MESSAGE`, `NOTIFICATION_LINK_NAME`, `NOTIFICATION_LINK_URL` | The banner at the top of every page. |
| `HELP_CONTENT_CONTACT`, `HELP_TECHNICAL_CONTACT` | The contacts on the Help tab. |
| `HELP_DOC_CSV` | The Help tab's documentation links: `URL\|Label` items separated by commas (neither can contain a comma). |
| `ADMIN_ROLE_NAME` | The role whose members are listed on the Help tab and get the feedback form's emails. It doesn't change who is an administrator: that is always the `adm-admin` role. |
| `EMAIL_ENABLED` | Turns on the Help tab's feedback emails. |
| `EMAIL_TESTING_ENABLED` | Sends emails to the members of the `testlead` role instead, for testing. |
| `EMAIL_SENDER_ADDRESS` | The sender of feedback emails. |
| `EMAIL_DOMAIN_NAME` | Added to a username to make an email address, such as `@jlab.org`. |
| `IP_READ_FILTER_ENABLED`, `IP_READ_ALLOWLIST_PATTERN`, `IP_READ_URL_PATTERN` | Whether users outside the allowed addresses must log in to view pages. |
| `SMOOTHNESS_CDN_ENABLED`, `SMOOTHNESS_SERVER` | Where the pages' shared scripts and styles come from. |

## Server configuration

These are set where ADM is installed, not on the Settings page; the
[README](https://github.com/JeffersonLab/adm#configure) has the details.

- **SSH access to the hosts.** ADM logs in to the hosts as each record's Deploy Username, with the
  key of the account that runs the app server, and never asks for a password. For a new host or
  Deploy Username, add that key to the account's `authorized_keys` on the host before the first
  deploy.
- **Networks for inventory edits.** The environment variable `ADM_INVENTORY_EDIT_NETWORKS` lists
  the addresses or networks, such as `192.0.2.0/24`, that administrators must connect from to edit
  the inventory. Unset, they can edit from anywhere. See
  [Read-only from other networks](inventory.md#read-only-from-other-networks).
- **Token audience.** ADM can refuse bearer tokens that Keycloak issued for other apps; then each
  client that deploys needs an audience mapper.

## Help tab

The Help tab shows the version, the contacts, the administrators, the `HELP_DOC_CSV` links and a
feedback form, which emails the administrators. To link this guide from it, add an item such as
`https://jeffersonlab.github.io/adm/latest/|User Guide` to `HELP_DOC_CSV`.
