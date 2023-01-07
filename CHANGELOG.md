# Changelog

## Version 1.10 (July 1, 2016)

-   Fixed [JENKINS-31175](https://issues.jenkins-ci.org/browse/JENKINS-31175) -
    License missing in source code
-   Added per-slave start & stop scripts specifically for on-demand
    slaves. This also
    resolves [JENKINS-20489](https://issues.jenkins-ci.org/browse/JENKINS-20489) - start
    slave on demand and don't power off afterwards

## Version 1.9 (June 17, 2015)

-   Fixed
    [JENKINS-28297](https://issues.jenkins-ci.org/browse/JENKINS-28297) -
    NPE in pre-launch Script for Jenkins Versions bigger than 1.609

## Version 1.8 (September 23, 2014)

-   Fix a bug when Slave could not be started of no labels of the
    pre-launch Scripts match.

## Version 1.7 (September 22, 2014)

-   Expose NODE\_TO\_SETUP\_NAME and NODE\_TO\_SETUP\_LABELS
    ([JENKINS-24107](https://issues.jenkins-ci.org/browse/JENKINS-24107))
-   Help files are displayed now
-   Possibility to add pre-launch scripts which will be executed before
    the slave is launched

## Version 1.6 (July 2, 2012)

-   Fixed error on using empty Slave Setup Config Item.

## Version 1.5 (June 18, 2012)

-   Added the possibility to create a list of slave setup items to
    support different setups.
-   Added field for prepare script to provide the possibility to prepare
    the files to be copied to a slave.

## Version 1.4 (Mar 08, 2012)

-   Added checkbox to enable setup deployment on save of the system
    configuration page.
-   Added label field to filter slaves to be used for setup deployment.

## Version 1.0 (Sep 17, 2011)

-   Initial version
