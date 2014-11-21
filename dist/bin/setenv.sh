#!/bin/sh

msg="\
Java (JDK/JRE) 6 does not exist in the PATH

To set Java (JDK/JRE) 6 in the PATH do the following ...

1. edit \$HOME/.profile or \$HOME/.bash_profile (if you are using bash)

2. add following lines
	JAVA_HOME=/path/to/jdk6_or_jre6/home
	PATH=\$JAVA_HOME/bin:\$PATH
	export PATH

3. Save and quit
"
JAVA="java"
java -version 2>&1 | head -1 | grep "1\.6"  2<&1 > /dev/null 
if [ $? != 0 ]; then
	JAVA=""
	dn=`dirname $0`
	setenv="$dn/.setenv.sh"
	if [ -f "$setenv" ]; then
		. "$setenv"
		$JAVA_HOME/bin/java -version 2>&1 | head -1 | grep "1\.6"  2<&1 > /dev/null 
		[ $? == 0 ] && "JAVA=$JAVA_HOME/bin/java"
	fi
fi

if [ "$JAVA" == "" ]; then
	echo ""
	echo "$msg"
	[ -x /usr/bin/zenity ] && /usr/bin/zenity --error --text "$msg"
	exit 1
fi

export JAVA
