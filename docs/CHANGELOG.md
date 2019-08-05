:paw_prints:  Back to Utility [README](README.md).

---

# Change Log
All notable changes to this project will be documented in this file. 
(if you need to update/polish tests please branch from the release tags)

## [[v5.2.0-2] - 2017-02-15](/tas/alfresco-tas-webdav-test/commits/v5.2.0-2)
### Info
Please notice that some of the tests implemented will only work on LINUX, or Windows. Those tests are marked with the appropiate OS version.
When those tests are executed, you will see that those are skipped if the OS agent is not the one defined.

Example:
```
This test was skipped because it was marked to be executed on differed operating system(s). Groups used: [protocols, webdav, core, linux] and was executed on: Windows 7
```

### Added
- Only unmount if is Windows OS
- Added unmount before network drive tests
- Fixed rename tests
- 'WIP: using last ContentModel resource model'
- fix bambooRun.sh
- Added extension to copiedFile from siteManagerCanCopyFileOnMappedDrive test
- 'test: Added tests for create, rename, update and quota'
- Fixed typos and other minor changes
- update TestGroup to use Linux
- fix bambooRun.sh
- add bambooRun.sh
- update suites with OSTestMethodListener
- 'test: Restricted tests to only run on a compatible node'
- 'test: Added more tests for mounted WebDav'
- Removed "/" from prefixSpace
- 'test: added TAS-2840 tests'
- 'core: Added support for mounting network drive; create folder in mounted
  drive and check if content exists or not'
- Updated utility version from 1.0.8 to 1.0.9
- added testCount.xml
- 'Changed JenkinsFile: specify if @bug tests are running or not'
- rest default.properties file
- update log4j to include threads
- added log4j properties for testRail
- add default setting for testRail
- fix TestGroup priority
- default of test server
- added 5.2 default property settings

## [[v5.2.0-1] - 2016-12-20](/tas/alfresco-tas-webdav-test/commits/v5.2.0-1)
### Added
- 100 % Core coverage test for WebDav on Alfresco 5.2

### Updated
- version of utility to 1.0-6, log4j properties

## [[v5.2.0-0] - 2016-11-25](/tas/alfresco-tas-webdav-test/commits/v5.2.0-0)

- 100 % Sanity test for WebDav on Alfresco 5.2
