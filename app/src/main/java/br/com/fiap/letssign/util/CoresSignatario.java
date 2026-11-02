package br.com.fiap.letssign.util;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import br.com.fiap.letssign.R;

/**
 * Uma cor para cada signatário, na ordem da lista (c0 a c3, como no protótipo).
 * É a única cor do app além dos status.
 */
public final class CoresSignatario {

    private static final int[] CORES = {R.color.campo_c0, R.color.campo_c1, R.color.campo_c2, R.color.campo_c3};

    /** Na página do PDF o fundo é sempre branco: usa sempre a versão clara das cores. */
    private static final int[] CORES_NA_PAGINA = {
            Color.parseColor("#0B0B0C"),
            Color.parseColor("#2563EB"),
            Color.parseColor("#D97706"),
            Color.parseColor("#7C3AED"),
    };

    private CoresSignatario() {
    }

    /** Cor do tema atual (claro ou escuro), para chips e barras na tela. */
    public static int cor(Context context, int posicao) {
        return ContextCompat.getColor(context, CORES[Math.abs(posicao) % CORES.length]);
    }

    /** Cor do retângulo desenhado em cima da página branca do PDF. */
    public static int corNaPagina(int posicao) {
        return CORES_NA_PAGINA[Math.abs(posicao) % CORES_NA_PAGINA.length];
    }
}
