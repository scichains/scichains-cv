@echo off
rem Should be called from result folder of the artifact
set properties=-Dnet.algart.executors.path=net.algart.executors.core;net.algart.executors.cv
set properties=%properties% -Dnet.algart.executors.api.showInfo=true
set cp=./*
echo on
java -cp %cp% %properties% net.algart.executors.api.model.tests.callers.ExecutingChain %1 %2 %3 %4 %5