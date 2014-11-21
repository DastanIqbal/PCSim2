#!/bin/bash

outfile="IZPACK_JAR_FILE_NAME"

if [ "$1" == "self-install" ]; then
	dn=`dirname $0`
	instfile="$dn/PCSim2_Setup.bin"
	srcfile="$dn/$outfile"
	cp "$0" "$instfile"
	if [ ! -f "$srcfile" ]; then
		echo "$srcfile does not exist"
		exit 1;
	fi
	cat "$srcfile" >> $instfile
	chmod +x $instfile
	exit 0
fi


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
java -version 2>&1 | head -1 | grep "1\.6"  2<&1 > /dev/null 
if [ $? != 0 ]; then
	echo ""
	echo "$msg"
	[ -x /usr/bin/zenity ] && /usr/bin/zenity --error --text "$msg"
fi


num=47
tail -n +$num $0 > "$outfile"
trap 'rm -f "$outfile"; exit 1' HUP INT QUIT TERM
java -jar "$outfile"
rm -f "$outfile"
exit 0
