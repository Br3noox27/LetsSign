package br.com.fiap.letssign.util;

import android.content.Context;
import android.content.SharedPreferences;

import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.dominio.Perfil;

/** Quem está logado. Fica salvo até a pessoa tocar em Sair. */
public final class Sessao {

    private static final String ARQUIVO = "sessao";
    private static final String CHAVE_ID = "usuarioId";
    private static final String CHAVE_NOME = "nome";
    private static final String CHAVE_PERFIL = "perfil";

    private final SharedPreferences preferencias;

    public Sessao(Context context) {
        preferencias = context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public void entrar(Usuario usuario) {
        preferencias.edit()
                .putLong(CHAVE_ID, usuario.id)
                .putString(CHAVE_NOME, usuario.nomeCompleto)
                .putString(CHAVE_PERFIL, usuario.perfil.name())
                .apply();
    }

    public void sair() {
        preferencias.edit().clear().apply();
    }

    public boolean logado() {
        return usuarioId() > 0 && perfil() != null;
    }

    public long usuarioId() {
        return preferencias.getLong(CHAVE_ID, 0);
    }

    public String nome() {
        return preferencias.getString(CHAVE_NOME, "");
    }

    public Perfil perfil() {
        String valor = preferencias.getString(CHAVE_PERFIL, null);
        if (valor == null) {
            return null;
        }
        try {
            return Perfil.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
