# Git Workflow
A popular and well-known branching model and strategy with a little tweak is used in BlissLauncher. 
[Click here to read more about the original git branching model](https://nvie.com/posts/a-successful-git-branching-model/)

## Main Branches
At the very core, BlissLauncher have two main branches with infinite lifetime:
- `master`
- `dev`

We consider `origin/master` to be the main branch where the source code
of HEAD always reflects a production-ready state.

We consider `origin/dev` to be the main branch where the source code of
HEAD always reflects a state with the latest delivered development
changes for the next release.

When the source code in the dev branch reaches a stable point and is
ready to be released, all of the changes should be merged back into
master somehow and then tagged with a release number.

## Support Branches
- `Feature branches`
- `Hotfix branches`

We don't use release branches separately to simplify the deployment
process for our dev team. *That's the only difference, this model has as
compare to original model.*

### Feature Branches
Must branch off from: `dev`

Must merge back into: `dev`

Branch naming convention: anything except master, develop, hotfix-*
(preferable `feature-*`)

#### Steps for working on features
When starting work on a new feature, branch off from the `dev` branch.
```sh
$ git checkout -b feature-blur-background dev
# Switched to a new branch "feature-blur-background"
```
Finished features may be merged into the develop branch to definitely 
add them to the upcoming release:
```sh
git checkout dev
# Switched to branch "dev"
$ git merge --no-ff feature-blur-background
# Updating ea1b82a..05e9557
# (Summary of changes)
$ git branch -d feature-blur-background
Deleted branch feature-blur-background (was 05e9557).
$ git push origin dev
```

### Hotfix Branches
Must branch off from: `master`

Must merge back into: `master` and `dev`

Branch naming convention: `hotfix-*`

#### Steps for working on hotfixes
Hotfix branches are created from the master branch. As soon as, a hotfix
branch is created, version must be bumped using the command in the root
of the project `bump-version.sh patch`

```sh
$ git checkout -b hotfix-1.2.1 master
# Switched to a new branch "hotfix-1.2.1"
$ ./bump-version.sh patch 
```
Then, fix the bug and commit the fix in one or more separate commits.
```sh
$ git commit -m "Fixed severe production problem"
# [hotfix-1.2.1 abbe5d6] Fixed severe production problem
# 5 files changed, 32 insertions(+), 17 deletions(-)
```
When finished, the bugfix needs to be merged back into master, but also
needs to be merged back into dev, in order to safeguard that the bugfix
is included in the next release as well. Use
`tag-version.sh` for tagging on master branch.
```sh
$ git checkout master
# Switched to branch 'master'
$ git merge --no-ff hotfix-1.2.1
# Merge made by recursive.
# (Summary of changes)
$ ./tag-version.sh

$ git checkout dev
# Switched to branch 'dev'
$ git merge --no-ff hotfix-1.2.1
# Merge made by recursive.
# (Summary of changes)
```
Finally, remove the temporary branch: 
```sh
$ git branch -d hotfix-1.2.1
Deleted branch hotfix-1.2.1 (was abbe5d6).
```
### Prepare Release
Whenever all the features that are targeted for the release-to-be-built
are merged into the dev and we are ready to prepare a test-release
build, version must be bumped by using the command `bump-version.sh
[major | minor]` based on the changes in the feature. If the
test-release works without problem and bugs are fixed for this release,
we are ready to release and move this code into `master`. 

#### Steps for finishing a release
First, the `dev` is merged into `master` (since every commit on master
is a new release by definition, remember). Next, that commit on master
must be tagged for easy future reference to this historical version. Use
`tag-version.sh` for tagging on master branch.
```sh
$ git checkout master
# Switched to branch 'master'
$ git merge --no-ff dev
# Merge made by recursive.
# (Summary of changes)
$ git push origin master
$ ./tag-version.sh
```

# Versioning
Semantic Versioning is used for BlissLauncher that is every version is
named as major.minor.patch. For proper versioning, one MUST use the
`bump-version.sh` every-time a new hotfix branch is created or intended
features have been merged into dev branch and are ready to be released
in production. After merging into the master, `tag-version.sh` MUST be
used to tag the release version at the latest commit.