# Plataforma Distribuída de Processamento Colaborativo de Tarefas (Refatorado)

Projeto refatorado preparado para **entrega acadêmica** na disciplina de **Sistemas Distribuídos**.  
O sistema simula uma arquitetura distribuída para **processamento colaborativo de tarefas**, utilizando clientes, workers e orquestradores, com suporte a **relógio de Lamport** para controle de eventos distribuídos.

---

## 📂 Estrutura de Pacotes

- **cliente** → Representa os usuários que submetem tarefas para processamento.  
- **orquestrador** → Coordena os workers, recebe tarefas dos clientes e distribui entre os nós disponíveis.  
- **worker** → Executa as tarefas recebidas do orquestrador e reporta resultados.  
- **modelo** → Contém classes de domínio, como `Tarefa`, `Usuario` e `EstadoGlobal`.  
- **util** → Ferramentas de apoio, incluindo implementação do **Relógio de Lamport**.

---

## ⚙️ Execução

O projeto já inclui scripts para facilitar a compilação e execução:

### 🔹 Windows  
```bash
run_all.bat
```

### 🔹 Linux / MacOS  
```bash
chmod +x compile_and_run.sh
./compile_and_run.sh
```

Esses scripts compilam automaticamente os arquivos Java e iniciam os componentes do sistema (orquestrador principal, backup, workers e cliente).

---

## 📜 Logs

Durante a execução, o sistema gera arquivos de log em `logs/`, contendo:  
- **orq_principal.log** → eventos do orquestrador principal  
- **orq_backup.log** → eventos do orquestrador de backup  
- **worker.log** → processamento realizado pelos workers  

Esses registros podem ser usados para validar o comportamento distribuído do sistema.