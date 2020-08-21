#!/bin/bash

#30 00 * * * ~/evpay/script/ct_housekeeping.sh
#sudo crontab -e

today="`date +'%Y-%m-%d'`"
logFile=~/evpay/log/housekeep.log.$today

log(){ 
	echo -e "`date +'%H:%M:%S'` $1" >> $logFile
}

log "Housekeeping job started"
find /home/ct/octopus/upload_finished -type f -mtime +365 -print -exec rm -f {} \;
find /home/ct/octopus/download_outdated -type f -mtime +90 -print -exec rm -f {} \;
find ~/evpay/log -type f -mtime +90 -print -exec rm -f {} \;
find ~/evpay/script/upload.log* -type f -mtime +1 -print -exec rm -f {} \;
log "Housekeeping job completed\n\n"
