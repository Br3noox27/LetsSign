package br.com.fiap.letssign.util;

import java.io.File;

/** Cuida das cópias de PDF na pasta do app. Java puro (testável sem Android). */
public final class ArquivosPdf {

    private ArquivosPdf() {
    }

    /**
     * Apaga a cópia escolhida na tela se ela NÃO é a que está gravada no banco
     * (ex.: a pessoa escolheu um PDF e fechou "Novo documento" sem salvar).
     * Devolve true se apagou um arquivo.
     */
    public static boolean apagarCopiaNaoGravada(String caminhoNaTela, String caminhoGravado) {
        if (caminhoNaTela == null || caminhoNaTela.equals(caminhoGravado)) {
            return false;
        }
        File arquivo = new File(caminhoNaTela);
        return arquivo.isFile() && arquivo.delete();
    }
}
