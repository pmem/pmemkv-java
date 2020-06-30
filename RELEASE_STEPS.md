# pmemkv-java release steps

This document contains all the steps required to make a new release of pmemkv-java.

Make a release locally:
- add an entry to ChangeLog, remember to change the day of the week in the release date
  - for major/minor releases mention compatibility with the previous release
- update "pmemkv-java" project's version in all pom.xml files
- git commit -a -S -m "common: $VERSION release"
- git tag -a -s -m "pmemkv-java version $VERSION" $VERSION

Make a package:
- mvn package
- verify created packages: jar (with java code) and so (JNI, C++ code)

Publish changes:
- for major/minor release:
  - git push upstream HEAD:master $VERSION
  - create and push to upstream stable-$VERSION branch
  - create PR from stable-$VERSION to master
- for patch release:
  - git push upstream HEAD:stable-$VER $VERSION
  - create PR from stable-$VER to next stable (or master, if release is from most recent stable branch)

Publish package and make it official:
- go to [GitHub's releases tab](https://github.com/pmem/pmemkv-java/releases/new):
  - tag version: $VERSION, release title: pmemkv-java version $VERSION, description: copy entry from ChangeLog and format it with no tabs and no characters limit in line
- announce the release on pmem group
- publish package to mvn central repository

Later, for major/minor release:
- bump version of Docker images ("TAG" variable in build.sh, build-image.sh, push-image.sh, pull-or-rebuild-image.sh) to $VER+1 on master branch
- add new branch to valid-branches.sh on stable-$VER branch
- once gh-pages contains new documentation, add $VER entry in docs links and in "Releases' support status" table (and update any status if needed) on gh-pages
