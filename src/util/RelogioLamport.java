package util;

import java.util.concurrent.atomic.AtomicInteger;

public class RelogioLamport {
    private AtomicInteger time = new AtomicInteger(0);
    public int tick(){ return time.incrementAndGet(); }
    public int onReceive(int other){
        int updated = Math.max(time.get(), other) + 1;
        time.set(updated);
        return updated;
    }
    public int get(){ return time.get(); }
}
