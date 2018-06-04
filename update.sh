#!/bin/bash
rm -rf /var/lib/jenkins/plugins/slave-*
cp /var/lib/jenkins/shared/slave-setup.hpi* /var/lib/jenkins/plugins
chown jenkins:jenkins /var/lib/jenkins/plugins/* -R
service jenkins restart