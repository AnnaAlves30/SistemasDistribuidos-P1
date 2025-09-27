package cliente;

import java.io.*;
import java.net.*;
import java.util.*;

public class Cliente {
    private String user; private String pass; private String orqHost = "127.0.0.1"; private int orqPort = 5000;
    public Cliente(String user, String pass, int port, String[] cmds){
        this.user = user; this.pass = pass; this.orqPort = port;
        try(Socket s = new Socket(orqHost, orqPort)){
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            out.writeObject("AUTH:"+user+":"+pass); out.flush();
            Object o = in.readObject();
            if(o instanceof String && ((String)o).equals("AUTH_OK")){
                System.out.println("Autenticado como " + user);
                if(cmds!=null && cmds.length>0){
                    for(String c: cmds){
                        process(c, out, in);
                        Thread.sleep(500);
                    }
                } else {
                    Scanner sc = new Scanner(System.in);
                    while(true){
                        System.out.print("> "); String line = sc.nextLine();
                        process(line, out, in);
                    }
                }
            } else { System.out.println("Falha de autenticacao"); }
        }catch(Exception e){ e.printStackTrace(); }
    }
    private void process(String cmd, ObjectOutputStream out, ObjectInputStream in) throws Exception {
        if(cmd.startsWith("submit:") || cmd.startsWith("submit ")){
            String task = cmd.contains(":")? cmd.split(":",2)[1] : cmd.substring(7);
            out.writeObject("SUBMIT:"+task); out.flush();
            Object r = in.readObject();
            if(r instanceof String && ((String)r).startsWith("SUBMITTED:")){
                System.out.println("Tarefa submetida id=" + ((String)r).split(":")[1]);
            }
        } else if(cmd.startsWith("sleep:")){
            int s = Integer.parseInt(cmd.split(":")[1]); Thread.sleep(s*1000);
        } else {
            System.out.println("Comando desconhecido: " + cmd);
        }
    }
    public static void main(String[] args){
        String u = "client1"; String p = "secret"; int port = 5000;
        List<String> cmds = new ArrayList<>();
        if(args.length>0) u = args[0];
        if(args.length>1) p = args[1];
        if(args.length>2) port = Integer.parseInt(args[2]);
        if(args.length>3){
            for(int i=3;i<args.length;i++) cmds.add(args[i]);
        }
        new Cliente(u,p,port, cmds.toArray(new String[0]));
    }
}
