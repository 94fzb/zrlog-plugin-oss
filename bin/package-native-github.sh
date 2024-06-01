java -version
./mvnw clean package
./mvnw -Pnative -Dagent exec:exec@java-agent -U
./mvnw -Pnative package
basePath=/tmp/download/plugin
mkdir -p ${basePath}
binName=plugin-core
if [ -f "target/${binName}" ];
then
  mv target/${binName} ${basePath}/plugin-core-$(uname -s)-$(uname -m).bin
fi
if [ -f "target/${binName}.exe" ];
then
  mv target/${binName}.exe ${basePath}/${binName}-Windows-$(uname -m).bin
fi