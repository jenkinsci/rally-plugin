---
layout: default
---

## Rally Plugin

### We dislike logging into Rally.
You've committed your code, wrapped up the story, but now you have to log back into Rally (our organization makes us log in every three hours. THREE!) and update all the things.

**OR** you could install this plugin and just phrase your commit messages in an extremely deliberate fashion and let the Bookkeeper handle the bookkeeping.

### Configuration
Once you've installed the plugin through the normal channels (or by downloading the HPI file from the GitHub Releases page) you'll need to configure the plugin for your build.

But before you configure, you'll need to include the plugin in your build by clicking the following:

<img src="img/config-add-build.png" width="400">

Once added, you'll be presented with the following:

<img src="img/config-add-params.png" width="800">

* **Rally API Key**: Rally has moved to a token-based authentication instead of the old "username and password" standard. To create your own API key, go to the [Rally token page](https://rally1.rallydev.com/login), click `API Keys`, and then "Create your own API key". Once created, you'll want to copy that key into this field.
* **Rally Workspace Name**: Your workspace has a name. It should go here.
* **Rally Repository Name**: The name of the Repository object in Rally that you want the commits associated with. If you don't know what this is, just supply a generic value and check the checkbox for the "should create repository" configuration option.
* **Create Repository if it doesn't already exist??**: If you're supplying a new repository name and you haven't manually created it, this is a great box to check.
* **Commit URI**: Now we get to the fun stuff. This is a template of a URL that will be applied to all Changeset items (or "commits") that Rally adds. For example, if you're using GitHub, you may want your Changeset to link to `https://github.com/yourname/project-name/commit/[hash here]`. The way to do that is supply `https://github.com/yourname/project-name/commit/${revision}`.
* **File URI**: This is the same template of a URL but will be applied to each individual file (Change) in the Changeset. For example, if you're using GitHub, you may want your Change to link to `https://github.com/yourname/project-name/blob/[hash-here]/src/filename.rb`. The way to do that is to supply `https://github.com/yourname/project-name/blob/${revision}/${file}`.
* **Update Changes**: Select one from the dropdown.
* **Turn on debug level?**: If you select this you will get a teensy bit of debugging output in your build's log files.
* **Proxy URI**: This is a URI representing an HTTP proxy, if your build system is behind a proxy. You can add a username/password pair using this syntax: `http://username:password@myproxy.com:12345`.

### Commit Syntax (optional)
Once you get the plugin configured you will likely need to change the way you author your commit statements.

The plugin can update your Task information based on the commit messages you supply. Here is a guide:

* `TA12345` -- you can use this syntax to reference a specific Task under a Work Item
* `#3` -- you can also use this syntax. This is read as "Task #3 under the referenced Work Item"
* `status:` updates the Task's status to one of "In-Progress", "Defined", or "Completed". **Setting a status to "Completed" will automatically set the 'to do' field to 0**
* `todo:` updates the Task's "hours remaining" metric
* `actuals:` updates how long you've spent implementing a specific task
* `estimates:` updates the estimate for a specific task

Each hourly metric is updated with whole numbers only.

Here are some example messages using the above metrics:

```text
US12345: implements task #3 with status: completed; actuals: 5 hours, estimates: 6 hours (updated)
```
```text
US12345 for task TA54321 with status: in progress; actuals: 3 hours, to do: 15
```

### Contribution and License
If you want to contribute, please use the 'view project on GitHub' icon at the top of the page to see contribution information.

This project is distributed under the MIT license.
