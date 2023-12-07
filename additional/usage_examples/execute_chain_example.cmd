@echo off

echo ----------
echo NOTE: /executors-chains folder should contain the following files:
echo        /executors-chains/examples/arrays.json
echo        /executors-chains/examples/images/circles_1.jpg
echo The result will be saved in "%TEMP%"
echo:
pause
echo:

execute_chain /executors-chains/examples/arrays.json /executors-chains/examples/images/circles_1.jpg %TEMP% 1