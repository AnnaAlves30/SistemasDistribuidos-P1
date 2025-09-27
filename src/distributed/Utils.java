package distributed;
import java.io.*;
import java.net.*;
import java.util.*;

public class Utils {
    public static String simpleToken(String user, String pass){
        return Base64.getEncoder().encodeToString((user+":"+pass).getBytes());
    }
    public static void sleep(long ms){
        try{ Thread.sleep(ms);}catch(Exception e){}
    }
}
