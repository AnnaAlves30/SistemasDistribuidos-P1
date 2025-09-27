public class RelogioLamport{
    private long tempo = 0;

// Incrementa o relógio local antes de um evento local.
    public synchronized long tick() {
        return ++tempo;
    }

// Atualiza o relógio ao receber um valor de outro processo.
    public synchronized long atualizar(long recebido){
        tempo = Math.max(tempo, recebido) + 1; // Define tempo = max(local, recebido) + 1.
        return tempo();
    }

    //retorna o valor atual do relogio
    public synchronized long tempo(){
        return tempo;
    }
}

