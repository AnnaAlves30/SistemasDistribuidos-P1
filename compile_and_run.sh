#!/bin/bash
SRC=src
OUT=bin
mkdir -p $OUT
javac -d $OUT $(find $SRC -name "*.java")
if [ $? -ne 0 ]; then
  echo "Erro na compilacao"
  exit 1
fi
# Start orchestrator principal, backup, 3 workers, 2 clients (in background)
java -cp $OUT orquestrador.OrquestradorPrincipal 5000 &
sleep 1
java -cp $OUT orquestrador.OrquestradorBackup 5001 &
sleep 1
java -cp $OUT worker.Worker 6001 &
java -cp $OUT worker.Worker 6002 &
java -cp $OUT worker.Worker 6003 &
sleep 1
java -cp $OUT cliente.Cliente client1 secret 5000 submit:taskA sleep:5 submit:taskB &
java -cp $OUT cliente.Cliente client2 secret 5000 submit:taskC &
echo "Todos os processos iniciados (background). Use 'jobs' para ver os processos."
