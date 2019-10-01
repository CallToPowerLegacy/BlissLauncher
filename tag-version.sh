#!/bin/bash

# It must check for master branch and tag only if current branch is master.

YELLOW="\033[1;33m"

WARNING_FLAG=${YELLOW}!
branch=$(git branch | sed -n -e 's/^\* \(.*\)/\1/p')

if [[ ${branch} = "master" ]]; then

    # Check if your working tree is cleaned or not.
    git diff-index --quiet HEAD
    if [[ $? == 1 ]] ; then
        echo -e "${WARNING_FLAG} Working tree must be empty before tagging the version."
        exit 1
    fi

    # Check if your current source is not already tagged by using current hash
    GIT_COMMIT=`git rev-parse HEAD`
    NEEDS_TAG=`git describe --contains ${GIT_COMMIT}`
    # Only tag if no tag already (would be better if the git describe command above could have a silent option)
    if [[ -n "$NEEDS_TAG" ]]; then
        echo -e "${WARNING_FLAG} Latest commit is already tagged. Aborting now..."
        exit 0
    fi

    major=$((`cat app/build.gradle | grep "versionMajor = " | awk '{print $4}'`))
    minor=$((`cat app/build.gradle | grep "versionMinor = " | awk '{print $4}'`))
    patch=$((`cat app/build.gradle | grep "versionPatch = " | awk '{print $4}'`))

    version="${major}.${minor}.${patch}"
    git tag -a "v${version}" -m "Bliss Launcher Version ${version}"
    echo -e "Do you want to push tag to remote now?[Y/n]: "
    read -r -p "" response
    response=${response,,}
    if [[ ${response} =~ ^(yes|y| ) ]] || [[ -z ${response} ]]; then
        git push origin --tags
    else
        exit 1
    fi
else
    echo "Can only be used on master branch."
fi