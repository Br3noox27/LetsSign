package br.com.fiap.letssign.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.dominio.Perfil;

/**
 * Integrantes do grupo, cadastrados como Signatários para a demonstração.
 * Os e-mails são de exemplo (não são os institucionais).
 */
public final class SignatariosDoGrupo {

    public static final String SENHA = "123456";

    private SignatariosDoGrupo() {
    }

    public static List<Usuario> lista() {
        return Collections.unmodifiableList(Arrays.asList(
                pessoa("Vitor Silva", "vitor.fiap@exemplo.com"),
                pessoa("Breno Silva", "breno.fiap@exemplo.com"),
                pessoa("Julia Azevedo", "julia.fiap@exemplo.com"),
                pessoa("Yuri Luna", "yuri.fiap@exemplo.com")));
    }

    private static Usuario pessoa(String nome, String email) {
        Usuario u = new Usuario();
        u.nomeCompleto = nome;
        u.email = email;
        u.telefone = "";
        u.perfil = Perfil.SIGNATARIO;
        return u;
    }
}
