@echo off

set SCICHAINS=/SciChains
if not %1.==. set SCICHAINS=%1

echo ----------
echo NOTE: The actual version of SciChains application must be in installed in the folder %SCICHAINS%
echo If it is installed in another folder, please specify it as the argument of this cmd-file:
echo     execute_chain_example.cmd /actual_SciChains_folder
echo %%TEMP%% environment variable should set to the OS temporary directory where the results will be saved in BMP files.
echo Its current value is:
echo %TEMP%
echo:
pause
echo:

execute_chain %SCICHAINS%/demo/chains/image_processing/extract_face.chain %SCICHAINS%/demo/images/lenna.png %TEMP% 1