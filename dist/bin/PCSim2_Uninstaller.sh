#!/bin/sh
dn=`dirname $0`
cd "$dn"
. setenv.sh
$JAVA -jar ../Uninstaller/uninstaller.jar
