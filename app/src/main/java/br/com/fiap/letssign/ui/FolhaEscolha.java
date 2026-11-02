package br.com.fiap.letssign.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import br.com.fiap.letssign.R;

/** Folha que sobe de baixo (BottomSheet) com uma lista de opções para tocar. */
public final class FolhaEscolha {

    public interface AoEscolher {
        void escolheu(int posicao);
    }

    private FolhaEscolha() {
    }

    /**
     * @param secundarios texto menor de cada opção (pode ser null)
     * @param marcada     posição já escolhida, ou -1
     */
    public static void mostrar(Context context, String titulo, List<String> principais,
                               List<String> secundarios, int marcada, AoEscolher aoEscolher) {
        BottomSheetDialog folha = new BottomSheetDialog(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View raiz = inflater.inflate(R.layout.sheet_escolha, null, false);
        ((TextView) raiz.findViewById(R.id.titulo_sheet)).setText(titulo);
        LinearLayout opcoes = raiz.findViewById(R.id.opcoes_sheet);

        for (int i = 0; i < principais.size(); i++) {
            View linha = inflater.inflate(R.layout.item_opcao_sheet, opcoes, false);
            ((TextView) linha.findViewById(R.id.opcao_principal)).setText(principais.get(i));
            TextView secundario = linha.findViewById(R.id.opcao_secundaria);
            if (secundarios != null && secundarios.get(i) != null) {
                secundario.setText(secundarios.get(i));
            } else {
                secundario.setVisibility(View.GONE);
            }
            linha.findViewById(R.id.opcao_marcada).setVisibility(i == marcada ? View.VISIBLE : View.INVISIBLE);
            int posicao = i;
            linha.setOnClickListener(v -> {
                folha.dismiss();
                aoEscolher.escolheu(posicao);
            });
            opcoes.addView(linha);
        }
        folha.setContentView(raiz);
        folha.show();
    }
}
