package distributed;
import java.io.*;
import java.net.*;
import java.util.*;

public class Worker {
    private String orchestratorHost = "127.0.0.1";
    private int orchestratorPort = 5000; // default
    private int myPort;
    private LamportClock clock = new LamportClock();
    public Worker(int myPort, int multicastPort){
        this.myPort = myPort;
        this.orchestratorPort = multicastPort==7000?5000:orchestratorPort;
        startServer();
        registerWithOrchestrator();
    }
    private void startServer(){
        new Thread(()->{
            try(ServerSocket ss = new ServerSocket(myPort)){
                log("Worker listening on " + myPort);
                while(true){
                    Socket s = ss.accept();
                    new Thread(()-> handleTaskConn(s)).start();
                }
            }catch(Exception e){ e.printStackTrace(); }
        }).start();
    }
    private void handleTaskConn(Socket s){
        try(ObjectInputStream in = new ObjectInputStream(s.getInputStream())){
            Object o = in.readObject();
            if(o instanceof Message){
                Message m = (Message)o;
                if(m.type == Message.Type.ASSIGN_TASK){
                    clock.onReceive(m.lamport);
                    String[] parts = m.payload.split(":",2);
                    int id = Integer.parseInt(parts[0]); String desc = parts[1];
                    log("Received task " + id + ": " + desc);
                    // simulate work
                    Thread.sleep(2000 + new Random().nextInt(3000));
                    String result = "processed("+desc+")";
                    // send result back to orchestrator via new connection
                    try(Socket os = new Socket("127.0.0.1", 5000)){
                        ObjectOutputStream out = new ObjectOutputStream(os.getOutputStream());
                        Message res = new Message(Message.Type.TASK_RESULT, id+":"+result, clock.tick());
                        out.writeObject(res); out.flush();
                    }catch(Exception e){}
                }
            }
        }catch(Exception e){}
    }
    private void registerWithOrchestrator(){
        new Thread(()->{
            while(true){
                try(Socket s = new Socket("127.0.0.1", orchestratorPort)){
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    Message reg = new Message(Message.Type.REGISTER_WORKER, String.valueOf(myPort), clock.tick());
                    out.writeObject(reg); out.flush();
                    // send periodic heartbeats on the same socket is not done; instead open heartbeat msgs
                    while(true){
                        Thread.sleep(3000);
                        Message hb = new Message(Message.Type.HEARTBEAT, "", clock.tick());
                        out.writeObject(hb); out.flush();
                    }
                }catch(Exception e){
                    log("Cannot contact orchestrator, retrying in 2s");
                    try{ Thread.sleep(2000);}catch(Exception ex){}
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
        int myPort = 6001; int multicastPort = 7000;
        if(args.length>0) myPort = Integer.parseInt(args[0]);
        if(args.length>1) multicastPort = Integer.parseInt(args[1]);
        new Worker(myPort, multicastPort);
    }
}
