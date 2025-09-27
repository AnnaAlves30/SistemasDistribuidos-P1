package distributed;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.*;

public class Orchestrator {
    private static final int MULTICAST_PORT = 7000;
    private static final String MULTICAST_GROUP = "230.0.0.0";

    private int port;
    private boolean isPrimary;
    private ServerSocket serverSocket;
    private Map<String,String> users = new ConcurrentHashMap<>();
    private Map<Integer, TaskInfo> tasks = new ConcurrentHashMap<>();
    private Map<Integer,String> taskOwners = new ConcurrentHashMap<>();
    private Map<Integer,Integer> taskAssignedWorker = new ConcurrentHashMap<>();
    private List<WorkerInfo> workers = Collections.synchronizedList(new ArrayList<>());
    private AtomicInteger taskIdSeq = new AtomicInteger(1);
    private LamportClock clock = new LamportClock();
    private volatile long lastMulticast = System.currentTimeMillis();

    private ExecutorService pool = Executors.newCachedThreadPool();

    static class WorkerInfo { String host; int port; int lastHb; Socket socket; }
    static class TaskInfo { int id; String desc; int lamport; String status; }

    public Orchestrator(boolean primary, int port){
        this.isPrimary = primary; this.port = port;
        // simple users
        users.put("client1","secret"); users.put("client2","secret");
        try{
            serverSocket = new ServerSocket(port);
            log("Orquestrador started on port " + port + " as " + (isPrimary?"PRIMARY":"BACKUP"));
            startMulticastListener();
            startHeartbeatChecker();
            acceptLoop();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void acceptLoop() throws IOException {
        while(true){
            Socket s = serverSocket.accept();
            pool.submit(() -> handleConnection(s));
        }
    }

    private void handleConnection(Socket s){
        try(ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())){
            Object o = in.readObject();
            if(o instanceof Message){
                Message m = (Message)o;
                if(m.type == Message.Type.AUTH){
                    String[] parts = new String(Base64.getDecoder().decode(m.payload)).split(":");
                    String user = parts[0], pass = parts[1];
                    if(users.containsKey(user) && users.get(user).equals(pass)){
                        Message ok = new Message(Message.Type.AUTH_OK, Utils.simpleToken(user,pass), clock.tick());
                        out.writeObject(ok);
                        out.flush();
                        log("Client authenticated: " + user);
                        clientSession(user, in, out);
                    } else {
                        out.writeObject(new Message(Message.Type.AUTH_FAIL, "", clock.tick()));
                        out.flush();
                        s.close();
                    }
                } else if(m.type == Message.Type.REGISTER_WORKER){
                    // worker registration: payload = port
                    int workerPort = Integer.parseInt(m.payload);
                    WorkerInfo wi = new WorkerInfo();
                    wi.host = s.getInetAddress().getHostAddress();
                    wi.port = workerPort;
                    wi.socket = s;
                    wi.lastHb = (int)System.currentTimeMillis();
                    workers.add(wi);
                    log("Worker registered: " + wi.host + ":" + wi.port);
                    // keep listening to this worker for heartbeats
                    workerSession(wi, in, out);
                }
            }
        }catch(Exception e){
            //e.printStackTrace();
        }
    }

    private void clientSession(String user, ObjectInputStream in, ObjectOutputStream out){
        try{
            while(true){
                Object o = in.readObject();
                if(!(o instanceof Message)) break;
                Message m = (Message)o;
                clock.onReceive(m.lamport);
                if(m.type == Message.Type.SUBMIT_TASK){
                    int id = taskIdSeq.getAndIncrement();
                    TaskInfo t = new TaskInfo(); t.id = id; t.desc = m.payload; t.lamport = clock.tick(); t.status = "PENDING";
                    tasks.put(id, t);
                    taskOwners.put(id, user);
                    log("Task submitted id="+id+" desc="+t.desc);
                    assignTask(t);
                    // reply with id
                    out.writeObject(new Message(Message.Type.SUBMIT_TASK, String.valueOf(id), clock.get()));
                    out.flush();
                } else if(m.type == Message.Type.TASK_RESULT){
                    int id = Integer.parseInt(m.payload.split(":")[0]);
                    String res = m.payload.split(":",2)[1];
                    TaskInfo t = tasks.get(id);
                    if(t!=null){
                        t.status = "COMPLETED";
                        log("Task "+id+" completed with result: " + res);
                    }
                } else if(m.type == Message.Type.HEARTBEAT){
                    // ignore for client
                }
            }
        }catch(Exception e){ /*client disconnected*/ }
    }

    private void workerSession(WorkerInfo wi, ObjectInputStream in, ObjectOutputStream out){
        try{
            while(true){
                Object o = in.readObject();
                if(!(o instanceof Message)) break;
                Message m = (Message)o;
                if(m.type == Message.Type.HEARTBEAT){
                    wi.lastHb = (int)System.currentTimeMillis();
                    //log("HB from worker " + wi.host+":"+wi.port);
                } else if(m.type == Message.Type.TASK_RESULT){
                    int id = Integer.parseInt(m.payload.split(":")[0]);
                    String res = m.payload.split(":",2)[1];
                    TaskInfo t = tasks.get(id);
                    if(t!=null){
                        t.status = "COMPLETED";
                        log("Task "+id+" completed by worker " + wi.host + ":" + res);
                    }
                }
            }
        }catch(Exception e){ /*worker disconnected*/ }
    }

    private void assignTask(TaskInfo t){
        // round-robin among active workers
        if(workers.isEmpty()){
            log("No workers available to assign task " + t.id);
            return;
        }
        // choose worker with least assigned tasks (simple)
        WorkerInfo chosen = null; int min = Integer.MAX_VALUE;
        for(WorkerInfo w: workers){
            int count = 0;
            for(Integer v: taskAssignedWorker.values()) if(v==w.port) count++;
            if(count < min){ min = count; chosen = w; }
        }
        if(chosen!=null){
            try{
                Socket ws = new Socket(chosen.host, chosen.port);
                ObjectOutputStream out = new ObjectOutputStream(ws.getOutputStream());
                Message assign = new Message(Message.Type.ASSIGN_TASK, t.id+":"+t.desc, clock.tick());
                out.writeObject(assign); out.flush(); ws.close();
                taskAssignedWorker.put(t.id, chosen.port);
                t.status = "ASSIGNED";
                log("Assigned task " + t.id + " to worker " + chosen.host + ":" + chosen.port);
            }catch(Exception e){
                log("Failed to send task to worker " + chosen.host + ":" + chosen.port);
                workers.remove(chosen); // simulate failure
            }
        }
    }

    private void startMulticastListener(){
        // primary sends multicast periodically; backup listens. We'll implement both sending (if primary) and listening (both)
        new Thread(()->{
            try(MulticastSocket ms = new MulticastSocket(MULTICAST_PORT)){
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                ms.joinGroup(group);
                if(isPrimary){
                    // send state every 2s
                    while(true){
                        String state = "STATE:" + tasks.size() + ":" + clock.tick();
                        byte[] b = state.getBytes();
                        DatagramPacket p = new DatagramPacket(b, b.length, group, MULTICAST_PORT);
                        ms.send(p);
                        lastMulticast = System.currentTimeMillis();
                        Thread.sleep(2000);
                    }
                } else {
                    byte[] buf = new byte[1024];
                    while(true){
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        ms.receive(p);
                        String s = new String(p.getData(),0,p.getLength());
                        if(s.startsWith("STATE:")){
                            lastMulticast = System.currentTimeMillis();
                            // simple parse
                            //log("Backup received state: " + s);
                        }
                    }
                }
            }catch(Exception e){ e.printStackTrace(); }
        }).start();
    }

    private void startHeartbeatChecker(){
        new Thread(()->{
            while(true){
                try{
                    Thread.sleep(3000);
                    long now = System.currentTimeMillis();
                    // check workers lastHb
                    List<WorkerInfo> dead = new ArrayList<>();
                    for(WorkerInfo w: workers){
                        if(now - w.lastHb > 8000){
                            dead.add(w);
                        }
                    }
                    for(WorkerInfo d: dead){
                        log("Worker dead: " + d.host +":"+d.port);
                        // reassign tasks assigned to this worker
                        for(Map.Entry<Integer,Integer> e: new HashMap<>(taskAssignedWorker).entrySet()){
                            if(e.getValue() == d.port){
                                taskAssignedWorker.remove(e.getKey());
                                TaskInfo t = tasks.get(e.getKey());
                                if(t!=null && !t.status.equals("COMPLETED")){
                                    t.status = "PENDING";
                                    assignTask(t);
                                    log("Reassigned task " + t.id);
                                }
                            }
                        }
                        workers.remove(d);
                    }
                    // if backup and haven't heard multicast for a while, try to assume primary role (very simplified)
                    if(!isPrimary && now - lastMulticast > 8000){
                        log("No multicast from primary -> initiating takeover (becoming primary)"); 
                        isPrimary = true;
                        // start sending multicasts (handled by thread)
                        startMulticastListener();
                    }
                }catch(Exception e){}
            }
        }).start();
    }

    private static void log(String s){
        String line = new Date() + " [ORQ] " + s;
        System.out.println(line);
        try{ Files.createDirectories(Paths.get("logs")); Files.write(Paths.get("logs/orchestrator.log"), (line+"\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND); }catch(Exception e){}
    }

    public static void main(String[] args){
        boolean primary = true; int port = 5000;
        if(args.length>0 && args[0].equalsIgnoreCase("backup")) primary = false;
        if(args.length>1) port = Integer.parseInt(args[1]);
        new Orchestrator(primary, port);
    }
}
