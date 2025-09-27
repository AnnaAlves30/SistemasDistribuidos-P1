package modelo;

import java.io.Serializable;

public class Usuario implements Serializable {
    private String username;
    private String senha;
    public Usuario(){}
    public Usuario(String u, String s){ username = u; senha = s; }
    public String getUsername(){ return username; }
    public String getSenha(){ return senha; }
}
