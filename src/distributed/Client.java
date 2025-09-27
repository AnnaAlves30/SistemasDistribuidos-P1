package distributed;
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private String user; private String pass; private String orqHost = "127.0.0.1"; private int orqPort = 5000;
    private LamportClock clock = new LamportClock();
    public Client(String user, String pass, int port){
        this.user = user; this.pass = pass; this.orqPort = port;
        // connect, auth, then read commands from args or console
        try(Socket s = new Socket(orqHost, orqPort)){
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            Message auth = new Message(Message.Type.AUTH, Utils.simpleToken(user,pass), clock.tick());
            out.writeObject(auth); out.flush();
            Object o = in.readObject();
            if(o instanceof Message && ((Message)o).type == Message.Type.AUTH_OK){
                System.out.println("Authenticated as " + user);
                // if there are commands provided in args, execute them (we allow semicolon separated)
                // otherwise interactive console
                String cmds = System.getProperty("client.cmds");
                if(cmds!=null){
                    for(String c : cmds.split(";")){
                        processCommand(c.trim(), out, in);
                        Thread.sleep(500);
                    }
                } else {
                    Scanner sc = new Scanner(System.in);
                    while(true){
                        System.out.print("> ");
                        String line = sc.nextLine();
                        processCommand(line, out, in);
                    }
                }
            } else {
                System.out.println("Auth failed"); s.close();
            }
        }catch(Exception e){ e.printStackTrace(); }
    }
    private void processCommand(String line, ObjectOutputStream out, ObjectInputStream in) throws Exception {
        if(line.startsWith("submit ")){
            String task = line.substring(7);
            Message m = new Message(Message.Type.SUBMIT_TASK, task, clock.tick());
            out.writeObject(m); out.flush();
            Object o = in.readObject();
            if(o instanceof Message && ((Message)o).type == Message.Type.SUBMIT_TASK){
                System.out.println("Submitted task id=" + ((Message)o).payload);
            }
        } else if(line.startsWith("status")){
            System.out.println("Status requests not yet implemented in client demo.");
        } else if(line.startsWith("sleep:")){
            int ms = Integer.parseInt(line.split(":")[1]) * 1000;
            Thread.sleep(ms);
        }
    }
    public static void main(String[] args){
        String user = "client1"; String pass = "secret"; int port = 5000;
        if(args.length>0) user = args[0];
        if(args.length>1) pass = args[1];
        if(args.length>2) port = Integer.parseInt(args[2]);
        // optionally args[3]... are commands; we pass them via system property
        if(args.length>3){
            StringBuilder sb = new StringBuilder();
            for(int i=3;i<args.length;i++){
                if(i>3) sb.append(";");
                sb.append(args[i]);
            }
            System.setProperty("client.cmds", sb.toString());
        }
        new Client(user, pass, port);
    }
}
