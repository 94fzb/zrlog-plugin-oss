#!/usr/bin/env bash

java -version
./mvnw clean package
./mvnw -Pnative -Dagent exec:exec@java-agent -U
./mvnw -Pnative package
basePath=/tmp/download/plugin
mkdir -p ${basePath}
binName=oss
if [ -f "target/${binName}" ];
then
  echo "err"
  mv target/${binName} ${basePath}/${binName}-$(uname -s)-$(uname -m).bin
fi
if [ -f "target/${binName}.exe" ];
then
  echo "ok ?"
  mv target/${binName}.exe ${basePath}/${binName}-Windows-$(uname -m).exe
fi