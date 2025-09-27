import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

public class ServicoAutenticacao{
    // Mantém usuários fixos e gera tokens de sessão para login.
    private Map<String, String> usuarios = new HashMap<>();
    private Map<String, String> tokens = new HashMap<>();

    // Construtor: cria alguns usuários de teste.
    public ServicoAutenticacao() {
        usuarios.put("cliente1", "senha1");
        usuarios.put("cliente2", "senha2");
        usuarios.put("trabalhador1", "senha3");
        usuarios.put("trabalhador2", "senha4");
    }

    // Método de login: se credenciais forem válidas, gera e retorna um token.
    public synchronized String login(String usuario, String senha) {
        if (usuarios.containsKey(usuario) && usuarios.get(usuario).equals(senha)) {
            String token = UUID.randomUUID().toString();
            tokens.put(token, usuario);
            return token;
        }
        return null;
    }

    // Retorna o nome de usuário associado a um token, usando Optional para evitar retorno nulo.
    public synchronized Optional<String> usuarioPorToken(String token) {
        return Optional.ofNullable(tokens.get(token));
    }


}