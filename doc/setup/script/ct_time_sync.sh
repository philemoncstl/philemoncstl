#!/bin/bash

#00 05 * * * ~/evpay/script/ct_time_sync.sh
timeLog=~/evpay/log/timesync.log
timeServ="stdtime.gov.hk time.hko.hk"
beforeDT=$(date +"%d/%m/%Y %T")
echo "`date +"%d/%m/%Y %T"`  Time sync		started." >> $timeLog
echo "`date +"%d/%m/%Y %T"`  Time sync		`ntpdate -u $timeServ`" >> $timeLog
echo "`date +"%d/%m/%Y %T"`  Time sync		completed, before:$beforeDT" >> $timeLog
hwclock --systohc