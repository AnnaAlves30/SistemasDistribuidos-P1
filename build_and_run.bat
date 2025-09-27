\
@echo off
set SRC=src
set OUT=out
if not exist %OUT% mkdir %OUT%
javac -d %OUT% %SRC%\distributed\*.java
if errorlevel 1 (
  echo Erro na compilacao
  pause
  exit /b 1
)
echo Compilado com sucesso. Para iniciar, use run_all.bat
