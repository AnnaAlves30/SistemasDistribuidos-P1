import java.util.*;


// Classe responsável por escolher qual trabalhador vai executar a próxima tarefa.
// Suporta políticas: ROUND_ROBIN, MENOR_CARGA e ALEATORIO.
public class BalanceadorCarga {
public enum Politica { ROUND_ROBIN, MENOR_CARGA, ALEATORIO }


private Politica politica;
private int indice = 0;
private Random rnd = new Random();


public BalanceadorCarga(Politica politica) {
this.politica = politica;
}


// Escolhe o próximo trabalhador de acordo com a política definida.
public synchronized String escolherTrabalhador(Map<String, Orquestrador.InfoTrabalhador> trabalhadores) {
if (trabalhadores.isEmpty()) return null;
List<String> ids = new ArrayList<>(trabalhadores.keySet());
switch (politica) {
case ROUND_ROBIN:
String escolhido = ids.get(indice % ids.size());
indice++;
return escolhido;
case MENOR_CARGA:
return ids.stream().min(Comparator.comparingInt(w -> trabalhadores.get(w).carga)).orElse(ids.get(0));
case ALEATORIO:
return ids.get(rnd.nextInt(ids.size()));
default:
return ids.get(0);
}
}
}