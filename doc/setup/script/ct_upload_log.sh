#!/bin/bash

cd ~/evpay/script
source ./ct_upload_log.conf

today="`date +'%Y-%m-%d'`"
logFile="upload.log.$today"

log(){ 
	echo -e "`date +'%H:%M:%S'` $1" >> $logFile
}

ctId=$(grep -oPm1 "(?<=<ctId>)[^<]+" $configPath)
log "------------------------ CT: $ctId -------------------------------"

log "Upload job started"

rsync -e "ssh -o StrictHostKeyChecking=no" -avzh --port $port --out-format="%t %f %b" $localPath $user@$host:$remotePath/$ctId >> $logFile
rsync -e "ssh -o StrictHostKeyChecking=no" -avzh --port $port --out-format="%t %f %b" $localOctPath $user@$host:$remotePath/$ctId >> $logFile

log "Upload job completed\n\n"
cp -f $logFile $localLogPath