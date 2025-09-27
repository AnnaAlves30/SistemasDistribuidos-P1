@echo off
setlocal enabledelayedexpansion

set SRC=src
set OUT=bin

if not exist "%OUT%" mkdir "%OUT%"

echo ===============================
echo Compilando todos os arquivos Java...
echo ===============================

rem Criar lista de arquivos Java
set FILES=
for /R "%SRC%" %%f in (*.java) do (
    set FILES=!FILES! "%%f"
)

rem Compilar todos os arquivos de uma só vez
javac -d "%OUT%" -cp "%OUT%" %FILES%
if errorlevel 1 (
    echo Erro na compilacao
    pause
    exit /b 1
)

echo ===============================
echo Compilacao concluida com sucesso!
echo ===============================

rem ==== INICIAR PROCESSOS ====
echo Iniciando processos...

start "Orq-Principal" cmd /k "java -cp %OUT% orquestrador.OrquestradorPrincipal 5000"
timeout /t 1 >nul

start "Orq-Backup" cmd /k "java -cp %OUT% orquestrador.OrquestradorBackup 5001"
timeout /t 1 >nul

start "Worker-1" cmd /k "java -cp %OUT% worker.Worker 6001"
start "Worker-2" cmd /k "java -cp %OUT% worker.Worker 6002"
start "Worker-3" cmd /k "java -cp %OUT% worker.Worker 6003"
timeout /t 1 >nul

start "Client-1" cmd /k "java -cp %OUT% cliente.Cliente client1 secret 5000 submit:taskA sleep:5 submit:taskB"
start "Client-2" cmd /k "java -cp %OUT% cliente.Cliente client2 secret 5000 submit:taskC"

echo ===============================
echo Todos os processos iniciados!
echo ===============================
pause
