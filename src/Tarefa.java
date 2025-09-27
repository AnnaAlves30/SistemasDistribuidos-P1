import java.util.UUID;

// Classe que representa uma tarefa submetida por um cliente.
public class Tarefa {
    public enum Status{ PENDENTE, EM_ANDAMENTO, CONCLUIDA, FALHOU } // Estados possíveis da tarefa.

    private String id;
    private String clienteId;
    private String descricao;
    private Status status;
    private String trabalhadorDesignado;
    private long lamport;

    // Construtor da tarefa.
    public Tarefa(String clienteId, String descricao, long lamport) {
        this.id = UUID.randomUUID().toString();
        this.clienteId = clienteId;
        this.descricao = descricao;
        this.status = Status.PENDENTE;
        this.trabalhadorDesignado = null;
        this.lamport = lamport;
    }

    public String getId() {
        return id;
    }
    public String getClienteId() {
        return clienteId;
    }
    public String getDescricao() {
        return descricao;
    }
    public synchronized Status getStatus() {
        return status;
    }
    public synchronized void setStatus(Status status) {
        this.status = status;
    }
    public synchronized String getTrabalhadorDesignado() {
        return trabalhadorDesignado;
    }
    public synchronized void setTrabalhadorDesignado(String trabalhadorDesignado) {
        this.trabalhadorDesignado = trabalhadorDesignado;
    }
    public synchronized long getLamport() {
        return lamport;
    }
    public synchronized void setLamport(long lamport) {
        this.lamport = lamport;
    }
}

