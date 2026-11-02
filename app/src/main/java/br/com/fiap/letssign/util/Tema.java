package br.com.fiap.letssign.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Claro ou escuro. O app sempre começa no CLARO (preto e branco, fundo branco),
 * a não ser que a pessoa tenha escolhido o escuro no botão da barra do topo.
 */
public final class Tema {

    private static final String ARQUIVO = "preferencias";
    private static final String CHAVE_ESCURO = "temaEscuro";

    private Tema() {
    }

    /** Chamar ao abrir o app (LetsSignApp). */
    public static void aplicarSalvo(Context context) {
        AppCompatDelegate.setDefaultNightMode(escuro(context)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Troca o tema, guarda a escolha e redesenha as telas abertas. */
    public static void alternar(Context context) {
        boolean novo = !escuro(context);
        preferencias(context).edit().putBoolean(CHAVE_ESCURO, novo).apply();
        AppCompatDelegate.setDefaultNightMode(novo
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static boolean escuro(Context context) {
        return preferencias(context).getBoolean(CHAVE_ESCURO, false);
    }

    private static SharedPreferences preferencias(Context context) {
        return context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }
}
