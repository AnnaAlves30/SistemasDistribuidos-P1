\
@echo off
rem Compila e inicia orquestrador primary, backup, 3 workers e 2 clients em janelas separadas.
set SRC=src
set OUT=out
if not exist %OUT% mkdir %OUT%
javac -d %OUT% %SRC%\distributed\*.java
if errorlevel 1 (
  echo Erro na compilacao
  pause
  exit /b 1
)
rem Start primary orchestrator
start "Orchestrador-Primary" cmd /k "java -cp %OUT% distributed.Orchestrator primary 5000 7000"
rem Start backup orchestrator
start "Orchestrador-Backup" cmd /k "java -cp %OUT% distributed.Orchestrator backup 5001 7000"
rem Start workers (3)
start "Worker-1" cmd /k "java -cp %OUT% distributed.Worker 6001 7000"
start "Worker-2" cmd /k "java -cp %OUT% distributed.Worker 6002 7000"
start "Worker-3" cmd /k "java -cp %OUT% distributed.Worker 6003 7000"
rem Start clients (2) that submit tasks
start "Client-1" cmd /k "java -cp %OUT% distributed.Client client1 secret 5000 submit taskA;sleep:5;submit taskB"
start "Client-2" cmd /k "java -cp %OUT% distributed.Client client2 secret 5000 submit taskC;sleep:2;status all"
echo All processes started.
