#! /bin/sh
## 
## Start smppgw app
## 
NODE_ID=00

if [[ ! -e $MC_HOME/tmp ]]; then
    mkdir -p $MC_HOME/tmp
fi
if [[ ! -e $MC_HOME/tmp/archive ]]; then
    mkdir -p $MC_HOME/tmp/archive
fi

echo "Starting smpp.gw$NODE_ID app...."
nohup java -d64 -XX:+UseParallelGC -XX:ParallelGCThreads=4 -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$MC_HOME/gc/smppgw$NODE_ID.gc -Dlog4j.configurationFile="file:$MC_HOME/properties/log4j2.$NODE_ID.xml" SmppGw $MC_HOME/properties/smpp.gw$NODE_ID.properties > $MC_HOME/tmp/smpp.gw$NODE_ID.tmp &
