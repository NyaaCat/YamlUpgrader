#!/bin/bash

mkdir -p spigot_bin
pushd spigot_bin
    wget "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar" -O BuildTools.jar
    java -jar ./BuildTools.jar --rev 1.13.2
    java -jar ./BuildTools.jar --rev 1.12.2
popd

pushd YamlUpgrader_v1_12_R1
    gradle clean build
popd

pushd YamlUpgrader_v1_13_R2
    gradle clean build
popd

cp YamlUpgrader_*/build/libs/*.jar .