#!/bin/bash
# Script to download hubitat backup

backupdir='/data/backups/hubitat/c4_1'
he_ipaddr=192.168.0.8

cd $backupdir
ls -1tr | head -n -14 | xargs -d '\n' rm -f --

curl http://$he_ipaddr/hub/backup | grep download | awk -F"=" '{ print $4}' | awk -F'"' '{print $2}' | sed '/^$/d' | tail -1 | xargs -I @ curl http://$he_ipaddr/hub//backupDB?fileName=@ -o $backupdir/C4_1_$(date +%Y-%m-%d-%H%M).lzf


backupdir='/data/backups/hubitat/c7_1'
he_ipaddr=192.168.0.41

cd $backupdir
ls -1tr | head -n -14 | xargs -d '\n' rm -f --

curl http://$he_ipaddr/hub/backup | grep download | awk -F"=" '{ print $4}' | awk -F'"' '{print $2}' | sed '/^$/d' | tail -1 | xargs -I @ curl http://$he_ipaddr/hub//backupDB?fileName=@ -o $backupdir/C7_1_$(date +%Y-%m-%d-%H%M).lzf
