package br.com.fiap.letssign.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Abre um PDF com o PdfRenderer do Android e transforma páginas em imagem.
 * O PdfRenderer não aceita duas threads ao mesmo tempo: usar sempre pela fila do Tarefas.
 */
public class PdfPaginas implements Closeable {

    private static final int ALTURA_MAXIMA = 4000;

    private final ParcelFileDescriptor descritor;
    private final PdfRenderer renderer;

    /** Lança IOException ou SecurityException se o arquivo for inválido, corrompido ou protegido. */
    public PdfPaginas(File arquivo) throws IOException {
        descritor = ParcelFileDescriptor.open(arquivo, ParcelFileDescriptor.MODE_READ_ONLY);
        try {
            renderer = new PdfRenderer(descritor);
        } catch (IOException | RuntimeException e) {
            descritor.close();
            throw e;
        }
    }

    public int quantidade() {
        return renderer.getPageCount();
    }

    public Bitmap renderizar(int indice, int larguraPx) {
        try (PdfRenderer.Page pagina = renderer.openPage(indice)) {
            float escala = (float) larguraPx / pagina.getWidth();
            int largura = larguraPx;
            int altura = Math.max(1, Math.round(pagina.getHeight() * escala));
            if (altura > ALTURA_MAXIMA) {
                largura = Math.max(1, Math.round(largura * (float) ALTURA_MAXIMA / altura));
                altura = ALTURA_MAXIMA;
            }
            Bitmap imagem = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888);
            imagem.eraseColor(Color.WHITE);
            pagina.render(imagem, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return imagem;
        }
    }

    /** Abre o arquivo só para contar as páginas; serve para validar o PDF. */
    public static int contarPaginas(File arquivo) throws IOException {
        try (PdfPaginas pdf = new PdfPaginas(arquivo)) {
            return pdf.quantidade();
        }
    }

    @Override
    public void close() throws IOException {
        renderer.close();
        descritor.close();
    }
}
