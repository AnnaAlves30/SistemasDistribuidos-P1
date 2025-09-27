package modelo;

import java.io.Serializable;

public class Tarefa implements Serializable {
    private int id;
    private String descricao;
    private String status; // PENDING, ASSIGNED, COMPLETED
    private String resultado;
    public Tarefa() {}
    public Tarefa(int id, String descricao){
        this.id = id; this.descricao = descricao; this.status = "PENDING";
    }
    public int getId(){ return id; }
    public String getDescricao(){ return descricao; }
    public String getStatus(){ return status; }
    public void setStatus(String s){ status = s; }
    public void setResultado(String r){ resultado = r; }
    public String getResultado(){ return resultado; }
    public String toString(){ return "Tarefa["+id+"] desc="+descricao+" status="+status; }
}
