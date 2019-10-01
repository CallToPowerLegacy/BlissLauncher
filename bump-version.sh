#!/bin/bash
git diff-index --quiet HEAD --

#if [ $? -ne 0 ]; then
  #echo "Working tree must be empty before bumping the version"
#fi

RED="\033[1;31m"
GREEN='\033[1;32m'
YELLOW="\033[1;33m"
BLUE="\033[1;34m"
PURPLE="\033[1;35m"
CYAN="\033[1;36m"
WHITE="\033[1;37m"
NOCOLOR="\033[0m"

QUESTION_FLAG="${GREEN}?"
NOTICE_FLAG="${CYAN}❯"

BUMPING_MSG="${NOTICE_FLAG} Bumping up version...${NOCOLOR}"
PUSHING_MSG="${NOTICE_FLAG} Pushing new version to the ${WHITE}origin${CYAN}...${NOCOLOR}"

usage() {
    # Get the script name
    script_name=${0##*/}

    # Echo usage info
    echo " "
	echo -e " ${GREEN}${script_name}${NOCOLOR}"
	echo " ======================================"
	echo " ======================================"
	echo -e " ${BLUE}Author: ${WHITE}Amit Kumar"
	echo -e " "
	echo -e " Used to increment the version of ${RED}BlissLauncher${NOCOLOR} safely by performing predefined actions: "
	echo "  1. Checks the category of the revision (major,minor or patch) based on the argument (revision_type) passed into the script."
	echo -e "  2. Overwrite the version name and version code in app module level build.gradle based on the following logic"
	echo -e "     * - If this upgrade is a major, new version name will be ${CYAN}{old_major + 1}.0.0${NOCOLOR}"
	echo -e "     * - If upgrade type is minor, updated version name will be ${CYAN}old_major.{old_minor + 1}.0${NOCOLOR}"
	echo -e "     * - If it is a patch (hotfix), updated version name will be ${CYAN}old_major.old_minor.{old_patch + 1}.0${NOCOLOR}"
	echo -e "  3. Add and Commit the updated files with the message ${GREEN}Bump version to vX.Y.Z${NOCOLOR}."
	echo "  4. Create a new tag (named the same as new version name) which refers to"
	echo "     the commit created in (3)."
	echo "  5. Push the commit and tag to the remote git server."
	echo " "
	echo -e " ${BLUE}Usage:${NOCOLOR}"
	echo "   ${script_name} [<revision_type>]"
	echo " "
	echo -e " ${BLUE}Arguments:${NOCOLOR}"
	echo -e "   revision_type     Could be one of the following - ${RED}major, minor or patch${NOCOLOR}."
	echo -e " "
	exit 1
}

commit_and_push() {
git add app/build.gradle
git commit -m "Bump version to v$1"
echo -e "${QUESTION_FLAG} ${RED}Do you want to push it to remote now?[Y/n]: ${NOCOLOR}"
read -r -p "" response
response=${response,,}
if [[ ${response} =~ ^(yes|y| ) ]] || [[ -z ${response} ]]; then
    echo -e ${PUSHING_MSG}
    branch=$(git branch | sed -n -e 's/^\* \(.*\)/\1/p')
    git push -u origin ${branch}
else
    exit 1
fi
}

VERSION_UPGRADE_TYPE=$1

old_major=$((`cat app/build.gradle | grep "versionMajor = " | awk '{print $4}'`))
old_minor=$((`cat app/build.gradle | grep "versionMinor = " | awk '{print $4}'`))
old_patch=$((`cat app/build.gradle | grep "versionPatch = " | awk '{print $4}'`))

if [[ "${VERSION_UPGRADE_TYPE,,}" = "major" ]]; then
    echo -e ${BUMPING_MSG}
    echo -e "${NOTICE_FLAG} Current version: ${WHITE}${old_major}.${old_minor}.${old_patch}"

    new_major=$(($old_major + 1))
    new_version="${new_major}.0.0"

    echo -e "${NOTICE_FLAG} Will set new version to ${GREEN}${new_version}${NOCOLOR}."
    echo -ne "${QUESTION_FLAG} ${RED}Are you sure? [Y/n]: ${NOCOLOR}"
    response=${response,,}

    if [[ ${response} =~ ^(yes|y| ) ]] || [[ -z ${response} ]]; then
        sed -i "s/versionMajor = .*/versionMajor = ${new_major}/" app/build.gradle
        sed -i "s/versionMinor = .*/versionMinor = 0/" app/build.gradle
        sed -i "s/versionPatch = .*/versionPatch = 0/" app/build.gradle
        new_version="${new_major}.0.0"
        commit_and_push ${new_version}
    else
        exit 1
    fi

elif [[ "${VERSION_UPGRADE_TYPE,,}" = "minor" ]]; then
    echo -e ${BUMPING_MSG}
    echo -e "${NOTICE_FLAG} Current version: ${WHITE}${old_major}.${old_minor}.${old_patch}"

    new_minor=$((old_minor + 1))
    new_version="${old_major}.${new_minor}.0"

    echo -e "${NOTICE_FLAG} Will set new version to ${GREEN}${new_version}${NOCOLOR}."
    echo -ne "${QUESTION_FLAG} ${RED}Are you sure? [Y/n]: ${NOCOLOR}"
    read response
    response=${response,,}

    if [[ ${response} =~ ^(yes|y| ) ]] || [[ -z ${response} ]]; then
        sed -i "s/versionMinor = .*/versionMinor = ${new_minor}/" app/build.gradle
        sed -i "s/versionPatch = .*/versionPatch = 0/" app/build.gradle
        commit_and_push ${new_version}
    else
        exit 1
    fi

elif [[ "${VERSION_UPGRADE_TYPE,,}" = "patch" ]]; then
    echo -e ${BUMPING_MSG}
    echo -e "${NOTICE_FLAG} Current version: ${WHITE}${old_major}.${old_minor}.${old_patch}"

    new_patch=$((old_patch + 1))
    new_version="${old_major}.${old_minor}.${new_patch}"

    echo -e "${NOTICE_FLAG} Will set new version to ${GREEN}${new_version}${NOCOLOR}."
    echo -ne "${QUESTION_FLAG} ${RED}Are you sure? [Y/n]: ${NOCOLOR}"
    read response
    response=${response,,}

    if [[ ${response} =~ ^(yes|y| ) ]] || [[ -z ${response} ]]; then
        sed -i "s/versionPatch = .*/versionPatch = ${new_patch}/" app/build.gradle
        commit_and_push ${new_version}
    else
        exit 1
    fi

elif [[ "${VERSION_UPGRADE_TYPE,,}" = "help" ]]; then
    usage
else
    echo " Wrong or empty arguments passed for <revision_type>. See usage below."
    usage
fi