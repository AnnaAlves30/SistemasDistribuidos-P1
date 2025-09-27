package orquestrador;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import modelo.EstadoGlobal;
import util.RelogioLamport;

public class OrquestradorBackup {
    private int port;
    private RelogioLamport clock = new RelogioLamport();
    private long lastState = System.currentTimeMillis();
    public OrquestradorBackup(int port){
        this.port = port;
        log("OrquestradorBackup iniciado na porta " + port);
        // backup listens for multicast state messages (simplified)
        startMulticastListener();
        startFailoverWatcher();
    }
    private void startMulticastListener(){
        new Thread(()->{
            try(MulticastSocket ms = new MulticastSocket(7000)){
                InetAddress g = InetAddress.getByName("230.0.0.0"); ms.joinGroup(g);
                byte[] buf = new byte[1024];
                while(true){
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    ms.receive(p);
                    String s = new String(p.getData(),0,p.getLength());
                    if(s.startsWith("STATE:")){
                        lastState = System.currentTimeMillis();
                        // parse or store state (omitted: simple demo)
                        // log("STATE recv: " + s);
                    }
                }
            }catch(Exception e){ e.printStackTrace(); }
        }).start();
    }
    private void startFailoverWatcher(){
        new Thread(()->{
            while(true){
                try{ Thread.sleep(4000);
                    if(System.currentTimeMillis() - lastState > 8000){
                        log("Primary possivelmente down. Iniciando takeover local (exiting demo does not fully spawn primary behavior).");
                        // For demo we do nothing complex. In a full impl this backup would start listening on client/worker ports and assume state.
                    }
                }catch(Exception e){}
            }
        }).start();
    }
    private static void log(String s){
        String line = new Date() + " [ORQ-BK] " + s;
        System.out.println(line);
        try{ Files.createDirectories(Paths.get("logs")); Files.write(Paths.get("logs/orq_backup.log"), (line+"\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);}catch(Exception e){}
    }
    public static void main(String[] args){
        int port = 5001;
        if(args.length>0) port = Integer.parseInt(args[0]);
        new OrquestradorBackup(port);
    }
}
