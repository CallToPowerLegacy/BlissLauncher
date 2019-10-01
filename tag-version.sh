#!/bin/bash

# It must check for master branch and tag only if current branch is master.

branch=$(git branch | sed -n -e 's/^\* \(.*\)/\1/p')
if [[ ${branch} = "master" ]]; then
    major=$((`cat app/build.gradle | grep "versionMajor = " | awk '{print $4}'`))
    minor=$((`cat app/build.gradle | grep "versionMinor = " | awk '{print $4}'`))
    patch=$((`cat app/build.gradle | grep "versionPatch = " | awk '{print $4}'`))

    version="${major}.${minor}.${patch}"
    git tag -a "v${version}" -m "Bliss Launcher Version ${version}"
    echo -e "Do you want to push tag to remote now?[Y/n]: "
    read -r -p "" response
    response=${response,,}
    if [[ ${response} =~ ^(yes|y| ) ]] || [[ -z ${response} ]]; then
        branch=$(git branch | sed -n -e 's/^\* \(.*\)/\1/p')
        git push origin --tags
    else
        exit 1
    fi
else
    echo "Can only be used on master branch."
fi