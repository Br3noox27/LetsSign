package br.com.fiap.letssign.ui;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;

import br.com.fiap.letssign.R;

/** Controla o bloco include_vazio: ícone, texto curto e um botão opcional. */
public class EstadoVazio {

    private final View raiz;

    public EstadoVazio(View tela) {
        raiz = tela.findViewById(R.id.raiz_vazio);
    }

    public void configurar(@DrawableRes int icone, @StringRes int titulo, @StringRes int detalhe,
                           @StringRes int textoBotao, View.OnClickListener aoTocar) {
        ((ImageView) raiz.findViewById(R.id.icone_vazio)).setImageResource(icone);
        ((TextView) raiz.findViewById(R.id.texto_vazio)).setText(titulo);
        ((TextView) raiz.findViewById(R.id.detalhe_vazio)).setText(detalhe);
        MaterialButton botao = raiz.findViewById(R.id.botao_vazio);
        if (aoTocar != null) {
            botao.setText(textoBotao);
            botao.setOnClickListener(aoTocar);
            botao.setVisibility(View.VISIBLE);
        } else {
            botao.setVisibility(View.GONE);
        }
    }

    public void mostrar(boolean vazio) {
        raiz.setVisibility(vazio ? View.VISIBLE : View.GONE);
    }
}
