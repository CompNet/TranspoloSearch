@echo off

set TREETAGGER=res\ner\treetagger
set TAGDIR=%TREETAGGER%

set BIN=%TAGDIR%\bin
set CMD=%TAGDIR%\cmd
set LIB=%TAGDIR%\lib
set TAGOPT=%LIB%\english-utf8.par -token -lemma -sgml -no-unknown

if "%2"=="" goto label1
perl %CMD%\utf8-tokenize.perl -e -a %LIB%\english-abbreviations "%~1" | %BIN%\tree-tagger %TAGOPT% > "%~2"
goto end

:label1
if "%1"=="" goto label2
perl %CMD%\utf8-tokenize.perl -e -a %LIB%\english-abbreviations "%~1" | %BIN%\tree-tagger %TAGOPT% 
goto end

:label2
echo.
echo Usage: tag-english file {file}
echo.

:end