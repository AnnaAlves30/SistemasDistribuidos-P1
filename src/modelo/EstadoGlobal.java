package modelo;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EstadoGlobal implements Serializable {
    private Map<Integer, Tarefa> tarefas = new ConcurrentHashMap<>();
    public EstadoGlobal(){}
    public void add(Tarefa t){ tarefas.put(t.getId(), t); }
    public Tarefa get(int id){ return tarefas.get(id); }
    public Map<Integer,Tarefa> getTarefas(){ return tarefas; }
    public int size(){ return tarefas.size(); }
}
