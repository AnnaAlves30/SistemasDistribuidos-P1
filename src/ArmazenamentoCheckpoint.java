import java.io.*;
import java.nio.file.*;
import java.util.*;


// Classe que salva e carrega o estado das tarefas em arquivo.
// Usado para manter checkpoint do sistema em caso de falhas.
public class ArmazenamentoCheckpoint {
private final Path arquivo;


public ArmazenamentoCheckpoint(String caminho) {
this.arquivo = Paths.get(caminho);
}


// Salva todas as tarefas em um arquivo texto.
public synchronized void salvar(Map<String, Tarefa> tarefas) throws IOException {
try (BufferedWriter w = Files.newBufferedWriter(arquivo)) {
for (Tarefa t : tarefas.values()) {
w.write(t.getId() + "|" + t.getClienteId() + "|" + t.getDados() + "|" + t.getStatus() + "|" + t.getTrabalhadorDesignado() + "|" + t.getLamport());
w.newLine();
}
}
}


// Carrega o estado das tarefas de um arquivo texto.
public synchronized Map<String, Tarefa> carregar() throws IOException {
Map<String, Tarefa> mapa = new HashMap<>();
if (!Files.exists(arquivo)) return mapa;
List<String> linhas = Files.readAllLines(arquivo);
for (String l : linhas) {
if (l.trim().isEmpty()) continue;
String[] p = l.split("\\|");
Tarefa t = new Tarefa(p[1], p[2], Long.parseLong(p[5]));
mapa.put(p[0], t);
}
return mapa;
}
}