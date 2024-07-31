@echo off

echo ----------
echo NOTE: /chains/scichains-examples folder must exist and contain the following files:
echo        /chains/scichains-examples/demo/image_processing/extract_face.chain
echo        /chains/scichains-examples/demo/images/lenna.png
echo %%TEMP%% environment variable should set to the OS temporary directory where the results will be saved in BMP files.
echo Its current value is:
echo %TEMP%
echo:
pause
echo:

execute_chain /chains/scichains-examples/demo/image_processing/extract_face.chain /chains/scichains-examples/demo/images/lenna.png %TEMP% 1