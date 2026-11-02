package br.com.fiap.letssign.ui;

import android.app.Activity;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import br.com.fiap.letssign.R;

/** Marca o passo atual do fluxo (1 Dados, 2 Campos, 3 Revisão), como no protótipo. */
final class Passos {

    private static final int[] IDS = {R.id.passo_1, R.id.passo_2, R.id.passo_3};

    private Passos() {
    }

    static void marcar(Activity tela, int atual) {
        for (int i = 0; i < IDS.length; i++) {
            TextView passo = tela.findViewById(IDS[i]);
            boolean ativo = i + 1 == atual;
            passo.setBackgroundResource(ativo ? R.drawable.fundo_passo_ativo : R.drawable.fundo_passo);
            passo.setTextColor(ContextCompat.getColor(tela, ativo ? R.color.cor_sobre_primaria : R.color.cor_secundario));
        }
    }
}
