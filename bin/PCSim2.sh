#!/bin/sh
dn=`dirname $0`
cd "$dn"
. setenv.sh
#-Dswing.defaultlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel 
$JAVA -jar PC_Sim2-1.0-beta_1.jar
