package br.com.fiap.letssign.util;

import android.app.Activity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;

import br.com.fiap.letssign.R;

/**
 * Junta o PdfPageView com os botões de página (layout include_visualizador_pdf).
 * Usado na tela de posicionar campos, na revisão e no detalhe.
 */
public class VisualizadorPdf {

    public interface AoTrocarPagina {
        void aoTrocar(int pagina, int total);
    }

    /** Largura em pixels usada para desenhar a página. */
    private static final int LARGURA_RENDER = 1200;

    private final Activity tela;
    private final PdfPageView vista;
    private final TextView rotulo;
    private final MaterialButton anterior;
    private final MaterialButton proxima;
    private final ProgressBar carregando;

    private PdfPaginas pdf;
    private int atual;
    private int total;
    private AoTrocarPagina aoTrocarPagina;

    public VisualizadorPdf(Activity tela, View raiz) {
        this.tela = tela;
        vista = raiz.findViewById(R.id.vista_pdf);
        rotulo = raiz.findViewById(R.id.texto_pagina);
        anterior = raiz.findViewById(R.id.botao_pagina_anterior);
        proxima = raiz.findViewById(R.id.botao_pagina_proxima);
        carregando = raiz.findViewById(R.id.carregando_pdf);
        anterior.setOnClickListener(v -> mostrar(atual - 1));
        proxima.setOnClickListener(v -> mostrar(atual + 1));
        atualizarBotoes();
    }

    public PdfPageView vista() {
        return vista;
    }

    public int paginaAtual() {
        return atual;
    }

    public int totalPaginas() {
        return total;
    }

    public void setAoTrocarPagina(AoTrocarPagina ouvinte) {
        aoTrocarPagina = ouvinte;
    }

    /** Abre o PDF e mostra a página pedida. Em erro, chama falha (arquivo sumiu ou corrompeu). */
    public void abrir(String caminho, int paginaInicial, Tarefas.Resposta<Exception> falha) {
        carregando.setVisibility(View.VISIBLE);
        Tarefas.executar(tela, () -> {
            if (caminho == null) {
                throw new IOException("Documento sem PDF.");
            }
            PdfPaginas aberto = new PdfPaginas(new File(caminho));
            if (aberto.quantidade() == 0) {
                aberto.close();
                throw new IOException("O PDF não tem páginas.");
            }
            return aberto;
        }, aberto -> {
            fecharPdf();
            pdf = aberto;
            total = aberto.quantidade();
            mostrar(Math.max(0, Math.min(paginaInicial, total - 1)));
        }, erro -> {
            carregando.setVisibility(View.GONE);
            falha.receber(erro);
        });
    }

    public void mostrar(int indice) {
        PdfPaginas aberto = pdf;
        if (aberto == null || indice < 0 || indice >= total) {
            return;
        }
        atual = indice;
        atualizarBotoes();
        carregando.setVisibility(View.VISIBLE);
        Tarefas.executar(tela, () -> aberto.renderizar(indice, LARGURA_RENDER), imagem -> {
            carregando.setVisibility(View.GONE);
            if (indice == atual) {
                vista.setPagina(imagem, indice);
            }
        }, erro -> {
            carregando.setVisibility(View.GONE);
            rotulo.setText(tela.getString(R.string.erro_pagina));
        });
        if (aoTrocarPagina != null) {
            aoTrocarPagina.aoTrocar(indice, total);
        }
    }

    /** Chamar no onDestroy da tela. */
    public void fechar() {
        fecharPdf();
    }

    private void fecharPdf() {
        PdfPaginas antigo = pdf;
        pdf = null;
        if (antigo != null) {
            // Entra na fila depois de qualquer página que ainda esteja sendo desenhada.
            Tarefas.emSegundoPlano(() -> {
                try {
                    antigo.close();
                } catch (IOException | RuntimeException ignorado) {
                    // Nada a fazer: o arquivo já está fechado.
                }
            });
        }
    }

    private void atualizarBotoes() {
        anterior.setEnabled(atual > 0);
        proxima.setEnabled(atual < total - 1);
        if (total > 0) {
            rotulo.setText(tela.getString(R.string.pagina_x_de_y, atual + 1, total));
        } else {
            rotulo.setText("");
        }
    }
}
