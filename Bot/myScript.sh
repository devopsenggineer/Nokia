#!/bin/bash

echo "hello world"
#jar=$(ls build/libs/)
jar=$(find ./build/libs/ -type f -exec grep -l 'Web' {} \; | cut -d/ -f4)
echo " "
echo "$jar"

echo " "
#sed -i "s|build/libs/.*|build/libs/$jar\'|g" ./build.gradle

sed -i "s|ADD\ .*|ADD\  $jar\ bot.jar|g" ./src/main/docker/Dockerfile



#sed -i "s|COPY\ .*|COPY\  $jar\ control-plane.jar|g" ./src/main/docker/Dockerfile

#cp ./build/libs/"$jar"  ./src/main/docker/

ls ./src/main/docker/

#sed "/build/libs/$jar/ s/$/ ' /" ./Fxt/Web/build.gradle

#sed -i "/Web-0.0.1-SNAPSHOT.jar/  s/^/$jar/" ./Fxt/Web/src/main/docker/Dockerfile
#sed -i 's/Web-0.0.1-SNAPSHOT.jar/"$jar"/g' ./Fxt/Web/src/main/docker/Dockerfile

echo " "
cat ./src/main/docker/Dockerfile

#cat ./Fxt/Web/build.gradle
