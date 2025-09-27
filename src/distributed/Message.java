package distributed;
import java.io.Serializable;

public class Message implements Serializable {
    public enum Type { AUTH, AUTH_OK, AUTH_FAIL, SUBMIT_TASK, ASSIGN_TASK, TASK_RESULT, REGISTER_WORKER, HEARTBEAT, STATE_SYNC }
    public Type type;
    public String payload;
    public int lamport;
    public String clientId;
    public Message(Type type, String payload, int lamport){
        this.type = type; this.payload = payload; this.lamport = lamport;
    }
    public Message(){}
}
