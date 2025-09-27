package worker;

import java.io.*;
import java.net.*;
import java.util.*;
import modelo.Tarefa;

public class Worker {
    private int port;
    private String orqHost = "127.0.0.1";
    private int orqPort = 5000;
    public Worker(int port){
        this.port = port;
        startServer();
        register();
    }
    private void startServer(){
        new Thread(()->{
            try(ServerSocket ss = new ServerSocket(port)){
                log("Worker ouvindo em " + port);
                while(true){
                    Socket s = ss.accept();
                    new Thread(()-> handleConn(s)).start();
                }
            }catch(Exception e){ e.printStackTrace(); }
        }).start();
    }
    private void handleConn(Socket s){
        try(ObjectInputStream in = new ObjectInputStream(s.getInputStream())){
            Object o = in.readObject();
            if(o instanceof String){
                String m = (String)o;
                if(m.startsWith("ASSIGN:")){
                    String[] p = m.split(":",3);
                    int id = Integer.parseInt(p[1]); String desc = p[2];
                    log("Recebeu tarefa " + id + ": " + desc);
                    // simula trabalho
                    Thread.sleep(2000 + new Random().nextInt(3000));
                    String res = "processed("+desc+")";
                    // envia resultado ao orchestrator
                    try(Socket s2 = new Socket(orqHost, orqPort)){
                        ObjectOutputStream out = new ObjectOutputStream(s2.getOutputStream());
                        out.writeObject("RESULT:"+id+":"+res); out.flush(); s2.close();
                    }catch(Exception e){}
                }
            }
        }catch(Exception e){}
    }
    private void register(){
        new Thread(()->{
            while(true){
                try(Socket s = new Socket(orqHost, orqPort)){
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    out.writeObject("REGISTER_WORKER:"+port); out.flush();
                    // send periodic heartbeats via same socket
                    while(true){
                        Thread.sleep(3000);
                        out.writeObject("HB"); out.flush();
                    }
                }catch(Exception e){
                    log("Falha ao registrar no Orquestrador, tentando novamente em 2s"); try{ Thread.sleep(2000);}catch(Exception ex){}
                }
            }
        }).start();
    }
    private static void log(String s){
        String line = new Date() + " [WORKER] " + s;
        System.out.println(line);
        try{ java.nio.file.Files.createDirectories(java.nio.file.Paths.get("logs")); java.nio.file.Files.write(java.nio.file.Paths.get("logs/worker.log"), (line+"\n").getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);}catch(Exception e){}
    }
    public static void main(String[] args){
        int p = 6001;
        if(args.length>0) p = Integer.parseInt(args[0]);
        new Worker(p);
    }
}
