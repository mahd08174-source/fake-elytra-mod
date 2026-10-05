#!/usr/bin/env bash
# Compiles against Minecraft remapped to Yarn names, then remaps the mod back to intermediary (what Fabric uses at runtime).
set -euo pipefail
MC=1.21.11
FAPI='0.141.6+1.21.11'
LOADER=0.19.5
REMAPPER=0.14.1
YARN='1.21.11+build.6'
VERSION=1.0.0
ROOT="$(pwd)"
J="java -Xmx5g"
rm -rf build-work && mkdir -p build-work/libs build-work/classes && cd build-work

M=https://maven.fabricmc.net/net/fabricmc
VURL=$(curl -fsSL https://piston-meta.mojang.com/mc/game/version_manifest_v2.json | jq -r --arg v "$MC" '.versions[]|select(.id==$v)|.url')
curl -fsSL "$VURL" -o version.json
curl -fsSL "$(jq -r .downloads.client.url version.json)" -o client.jar
jq -r '.libraries[]|select(.downloads.artifact!=null)|.downloads.artifact.url' version.json > libs.txt
(cd libs && xargs -n1 -P8 curl -fsSLO < ../libs.txt)

curl -fsSL "$M/intermediary/$MC/intermediary-$MC-v2.jar" -o inter.jar
unzip -o -q inter.jar mappings/mappings.tiny -d inter
curl -fsSL "$M/yarn/$YARN/yarn-$YARN-v2.jar" -o yarn.jar
unzip -o -q yarn.jar mappings/mappings.tiny -d yarn
curl -fsSL "$M/tiny-remapper/$REMAPPER/tiny-remapper-$REMAPPER-fat.jar" -o tr.jar

$J -jar tr.jar client.jar client-inter.jar inter/mappings/mappings.tiny official intermediary libs/*.jar
$J -jar tr.jar client-inter.jar client-named.jar yarn/mappings/mappings.tiny intermediary named libs/*.jar

curl -fsSL "$M/fabric-api/fabric-api/$FAPI/fabric-api-$FAPI.jar" -o fapi.jar
mkdir fapi fapi-named && unzip -q fapi.jar 'META-INF/jars/*' -d fapi
curl -fsSL "$M/fabric-loader/$LOADER/fabric-loader-$LOADER.jar" -o loader.jar
for m in fabric-api-base fabric-item-api-v1; do
  for j in fapi/META-INF/jars/$m-[0-9]*.jar; do
    $J -jar tr.jar "$j" "fapi-named/$(basename $j)" yarn/mappings/mappings.tiny intermediary named client-inter.jar libs/*.jar loader.jar
  done
done
ls fapi-named

find "$ROOT/src/main/java" -name '*.java' > sources.txt
javac -proc:none -Xlint:none --release 21 -cp "client-named.jar:loader.jar:libs/*:fapi-named/*" -d classes @sources.txt

jar --create --file mod-named.jar -C classes .
$J -jar tr.jar mod-named.jar mod-inter.jar yarn/mappings/mappings.tiny named intermediary client-named.jar libs/*.jar fapi-named/*.jar loader.jar

mkdir final && (cd final && unzip -q ../mod-inter.jar)
cp -r "$ROOT/src/main/resources/." final/
sed -i "s/\${version}/$VERSION/" final/fabric.mod.json
jar --create --file "fakemod-$VERSION.jar" -C final .
unzip -l "fakemod-$VERSION.jar"
