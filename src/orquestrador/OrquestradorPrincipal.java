package orquestrador;

import modelo.*;
import util.RelogioLamport;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class OrquestradorPrincipal {
    private ServerSocket server;
    private int port;
    private EstadoGlobal estado = new EstadoGlobal();
    private Map<Integer,Integer> tarefaParaWorker = new ConcurrentHashMap<>();
    private List<WorkerInfo> workers = Collections.synchronizedList(new ArrayList<>());
    private AtomicInteger seq = new AtomicInteger(1);
    private RelogioLamport clock = new RelogioLamport();

    static class WorkerInfo { String host; int port; long lastHb; }

    public OrquestradorPrincipal(int port){
        this.port = port;
        try{
            server = new ServerSocket(port);
            log("OrquestradorPrincipal iniciado na porta " + port);
            startHeartbeatChecker();
            acceptLoop();
        }catch(Exception e){ e.printStackTrace(); }
    }

    private void acceptLoop(){
        ExecutorService pool = Executors.newCachedThreadPool();
        while(true){
            try{
                Socket s = server.accept();
                pool.submit(()-> handleConnection(s));
            }catch(Exception e){ e.printStackTrace(); }
        }
    }

    private void handleConnection(Socket s){
        try(ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())){
            Object o = in.readObject();
            if(o instanceof String){
                String msg = (String)o;
                if(msg.startsWith("AUTH:")){
                    String[] p = msg.substring(5).split(":");
                    String user = p[0], pass = p[1];
                    if(auth(user, pass)){
                        out.writeObject("AUTH_OK"); out.flush();
                        clientSession(user, in, out);
                    } else {
                        out.writeObject("AUTH_FAIL"); out.flush(); s.close();
                    }
                } else if(msg.startsWith("REGISTER_WORKER:")){
                    int wport = Integer.parseInt(msg.split(":")[1]);
                    WorkerInfo wi = new WorkerInfo();
                    wi.host = s.getInetAddress().getHostAddress();
                    wi.port = wport; wi.lastHb = System.currentTimeMillis();
                    workers.add(wi);
                    log("Worker registrado: " + wi.host +":"+wi.port);
                    // keep a thread reading heartbeats from this socket
                    workerSession(wi, in);
                }
            }
        }catch(Exception e){ /* connection closed */ }
    }

    private void clientSession(String user, ObjectInputStream in, ObjectOutputStream out){
        try{
            while(true){
                Object o = in.readObject();
                if(!(o instanceof String)) break;
                String cmd = (String)o;
                if(cmd.startsWith("SUBMIT:")){
                    String desc = cmd.substring(7);
                    int id = seq.getAndIncrement();
                    Tarefa t = new Tarefa(id, desc);
                    estado.add(t);
                    log("Tarefa submetida id="+id+" desc="+desc);
                    assignTask(t);
                    out.writeObject("SUBMITTED:"+id); out.flush();
                } else if(cmd.startsWith("RESULT:")){
                    // RESULT:id:res
                    String[] parts = cmd.split(":",3);
                    int id = Integer.parseInt(parts[1]);
                    String res = parts[2];
                    Tarefa t = estado.get(id);
                    if(t!=null){ t.setStatus("COMPLETED"); t.setResultado(res); log("Tarefa "+id+" concluida: " + res); }
                }
            }
        }catch(Exception e){}
    }

    private void workerSession(WorkerInfo wi, ObjectInputStream in){
        new Thread(()->{
            try{
                while(true){
                    Object o = in.readObject();
                    if(!(o instanceof String)) break;
                    String m = (String)o;
                    if(m.equals("HB")){
                        wi.lastHb = System.currentTimeMillis();
                    }
                }
            }catch(Exception e){}
        }).start();
    }

    private void assignTask(Tarefa t){
        if(workers.isEmpty()){ log("Sem workers para atribuir tarefa " + t.getId()); return; }
        WorkerInfo chosen = null; int min = Integer.MAX_VALUE;
        for(WorkerInfo w: workers){
            int count = 0;
            for(Integer p: tarefaParaWorker.values()) if(p==w.port) count++;
            if(count < min){ min = count; chosen = w; }
        }
        if(chosen!=null){
            try(Socket s = new Socket(chosen.host, chosen.port)){
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                out.writeObject("ASSIGN:"+t.getId()+":"+t.getDescricao()); out.flush(); s.close();
                tarefaParaWorker.put(t.getId(), chosen.port); t.setStatus("ASSIGNED");
                log("Tarefa " + t.getId() + " atribuida ao worker " + chosen.host+":"+chosen.port);
            }catch(Exception e){ log("Falha ao enviar para worker, removendo: " + chosen.port); workers.remove(chosen); }
        }
    }

    private void startHeartbeatChecker(){
        new Thread(()->{
            while(true){
                try{ Thread.sleep(3000);
                    long now = System.currentTimeMillis();
                    List<WorkerInfo> dead = new ArrayList<>();
                    for(WorkerInfo w: new ArrayList<>(workers)){
                        if(now - w.lastHb > 8000) dead.add(w);
                    }
                    for(WorkerInfo d: dead){
                        log("Worker morto: " + d.host+":"+d.port); workers.remove(d);
                        // reassign tasks
                        for(Map.Entry<Integer,Integer> e: new HashMap<>(tarefaParaWorker).entrySet()){
                            if(e.getValue()==d.port){
                                tarefaParaWorker.remove(e.getKey());
                                Tarefa t = estado.get(e.getKey());
                                if(t!=null && !t.getStatus().equals("COMPLETED")){
                                    t.setStatus("PENDING"); assignTask(t); log("Reatribuida tarefa " + t.getId());
                                }
                            }
                        }
                    }
                }catch(Exception e){}
            }
        }).start();
    }

    private boolean auth(String u, String p){
        // demo static auth
        return (u.equals("client1") && p.equals("secret")) || (u.equals("client2") && p.equals("secret"));
    }

    private static void log(String s){
        String line = new Date() + " [ORQ-PRIM] " + s;
        System.out.println(line);
        try{ Files.createDirectories(Paths.get("logs")); Files.write(Paths.get("logs/orq_principal.log"), (line+"\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);}catch(Exception e){}
    }

    public static void main(String[] args){
        int port = 5000;
        if(args.length>0) port = Integer.parseInt(args[0]);
        new OrquestradorPrincipal(port);
    }
}
