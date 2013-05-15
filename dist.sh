#!/bin/bash

USER=moat
HOST=moat
ARTIFACT=moat-1.0-SNAPSHOT

sbt dist

ssh $USER@$HOST "rm -rf $ARTIFACT && rm -rf $ARTIFACT.zip"
scp dist/$ARTIFACT.zip $USER@$HOST:.
ssh $USER@$HOST "unzip $ARTIFACT.zip"

ssh $USER@$HOST "kill \`cat $ARTIFACT-instance-1/RUNNING_PID\` && rm -rf $ARTIFACT-instance-1 && cp -R $ARTIFACT $ARTIFACT-instance-1"
ssh $USER@$HOST "chmod +x $ARTIFACT-instance-1/start && nohup $ARTIFACT-instance-1/start -Dconfig.file=conf/application.conf -Dhttp.port=9000 > /dev/null &"

ssh $USER@$HOST "kill \`cat $ARTIFACT-instance-2/RUNNING_PID\` && rm -rf $ARTIFACT-instance-2 && cp -R $ARTIFACT $ARTIFACT-instance-2"
ssh $USER@$HOST "chmod +x $ARTIFACT-instance-2/start && nohup $ARTIFACT-instance-2/start -Dconfig.file=conf/application.conf -Dhttp.port=9001 > /dev/null &"
