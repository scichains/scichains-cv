@echo off
rem Should be called from result folder of the artifact
set properties=-Dnet.algart.executors.path=net.algart.executors.core;net.algart.executors.cv
set properties=%properties% -Dnet.algart.executors.api.showInfo=true
set properties=%properties% -Dnet.algart.executors.check.existing.paths=false
rem - We specify JAR files below in non-standard way and much disable built-in checking of class-paths
set properties=%properties% -Dpolyglot.engine.WarnInterpreterOnly=false
set cp=./*
echo on
java --enable-native-access=ALL-UNNAMED -cp %cp% %properties% net.algart.executors.api.tests.ExecutingChain %1 %2 %3 %4 %5
rem --enable-native-access is necessary since JDK 24