# Deploying

[User Guide](README.md) › Deploying

Most deploys come from CI pipelines; see [Deploying from CI](ci.md). This page is for deploying
by hand, and for reading any deploy's job on the Log tab.

## Requesting a deploy

The **Choose** tab has the deploy form:

1. **Environment**: the env's name, such as `dev`.
2. **Application**: the app's name, such as `dtm`.
3. **Version**: a [semantic version](concepts.md#versions), such as `1.2.3`.

Names must match the allowlist exactly, case included; look them up on
**Inventory › Deployment Allowlist**. Then select **Deploy**.

ADM answers at once, before the deploy runs:

- **Success: Job submitted successfully. See asynchronous log for job #13**: the job has started.
  The form clears, and the link opens the job on the Log tab.
- **Fail:** and a reason: no job was started. The common reasons are:

| Message | What to do |
|---|---|
| AppEnv not found for app … and env … | No allowlist record has that app and env. Check the spelling and case, or ask an administrator to add the record. |
| User … is not authorized to deploy app … to env … | Only the record's Request Username and administrators may deploy it. |
| Version string must be semver formatted | Use a version such as `1.2.3`, without a `v`. |
| Unable to connect to adm | The request didn't reach ADM. Reload the page, logging in again if asked, and retry. |

"Success" means only that the job started. Whether the deploy worked is the job's exit code, on
the Log tab.

## The Log tab

The **Log** lists every deploy job, newest first, 10 to a page; **Previous** and **Next** page
through them. The line above the table says what is listed and how many.

| Column | Shows |
|---|---|
| **Job #** | The job's number, as the Choose tab and the API report it. |
| **App Env Ver** | The app, env and version deployed, such as `dtm / prod / 1.2.3`. |
| **Period** | When the job started and ended, in the server's time zone, and how long it took. A job with no end is still running. |
| **Exit Code / Stack Trace** | The command's exit code, 0 for success, or None if it couldn't run or finish. **View** opens the stack trace, which says why: see [Deploy jobs](concepts.md#deploy-jobs). |
| **Stdout/Stderr** | **View** opens what the command printed to standard output or standard error. |

The Log doesn't refresh by itself: reload the page to see a running job finish.

### Finding a job

Select **Choose...** to open the filter panel and narrow the list:

- **Job ID**: the one job with that number. The link on the Choose tab opens the Log filtered
  this way.
- **App Name**: the jobs of the app with that name, ignoring case. Use `%` as a wildcard:
  `dtm%` matches `dtm` and `dtm-test`. (The Inventory's filters use `*` instead.)

Select **Apply** to filter, and **Clear** to empty the fields.

## When a deploy fails

Look at the job on the Log tab:

- **Exit code other than 0**: the deploy command ran and failed. Its stdout and stderr usually
  say why. The command and the script it runs belong to the app's team, or to whoever looks
  after the hosts.
- **Exit code None, with a stack trace**: the command couldn't run or finish. The trace's first
  lines name the cause, such as an unknown host, a refused SSH login (the Deploy Username or ADM's
  key isn't set up on the host), a closed port, or `Deploy command did not finish within 600
  seconds`. For an env with several hosts, it says which host failed and which weren't deployed
  to. Ask an administrator to check the allowlist record and the host.
- **No end long after 10 minutes**: ADM stops a command at 10 minutes, so the job was cut off,
  such as by a restart of ADM's server, and its result is unknown. Check the app on its hosts,
  and request the deploy again if needed.

A failed deploy changes nothing in ADM: correct the cause and request the deploy again, as a new
job.
