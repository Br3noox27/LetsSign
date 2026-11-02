package br.com.fiap.letssign.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import java.io.File;

/**
 * Lê o PNG da assinatura e recorta só a parte desenhada (o quadro tem muito espaço em branco),
 * para a assinatura aparecer grande no cartão e dentro do campo do PDF. Rodar fora da thread da tela.
 */
public final class ImagemAssinatura {

    private ImagemAssinatura() {
    }

    /** Null se o arquivo não existe ou não abre. */
    public static Bitmap carregar(String caminho) {
        if (caminho == null || !new File(caminho).isFile()) {
            return null;
        }
        BitmapFactory.Options opcoes = new BitmapFactory.Options();
        opcoes.inSampleSize = 2; // metade do tamanho: suficiente para mostrar e gasta 4x menos memória
        Bitmap imagem = BitmapFactory.decodeFile(caminho, opcoes);
        if (imagem == null) {
            return null;
        }
        return recortar(imagem);
    }

    private static Bitmap recortar(Bitmap imagem) {
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();
        int[] linha = new int[largura];
        int esquerda = largura;
        int direita = -1;
        int topo = altura;
        int baixo = -1;
        for (int y = 0; y < altura; y++) {
            imagem.getPixels(linha, 0, largura, 0, y, largura, 1);
            for (int x = 0; x < largura; x++) {
                if (escuro(linha[x])) {
                    esquerda = Math.min(esquerda, x);
                    direita = Math.max(direita, x);
                    topo = Math.min(topo, y);
                    baixo = Math.max(baixo, y);
                }
            }
        }
        if (direita < 0) {
            return imagem; // nada desenhado
        }
        int margem = 6;
        esquerda = Math.max(0, esquerda - margem);
        topo = Math.max(0, topo - margem);
        direita = Math.min(largura - 1, direita + margem);
        baixo = Math.min(altura - 1, baixo + margem);
        return Bitmap.createBitmap(imagem, esquerda, topo, direita - esquerda + 1, baixo - topo + 1);
    }

    /** Traço da caneta: bem mais escuro que o fundo branco e a linha de base cinza-clara. */
    private static boolean escuro(int cor) {
        return Color.alpha(cor) > 0 && (Color.red(cor) + Color.green(cor) + Color.blue(cor)) < 300;
    }
}
