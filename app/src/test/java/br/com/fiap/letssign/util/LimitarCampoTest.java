package br.com.fiap.letssign.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import br.com.fiap.letssign.dominio.Regras;

/** O arrasto do campo nunca deixa o campo sair da página (confere com a regra do Breno). */
public class LimitarCampoTest {

    @Test
    public void valorDentroNaoMuda() {
        assertEquals(0.2, PdfPageView.limitar(0.2, 0.3), 1e-12);
    }

    @Test
    public void negativoVaiParaZero() {
        assertEquals(0.0, PdfPageView.limitar(-0.4, 0.3), 1e-12);
    }

    @Test
    public void passarDaBordaEncostaNaBorda() {
        double x = PdfPageView.limitar(0.95, 0.3);
        assertTrue(Regras.campoDentroDaPagina(x, 0, 0.3, 0.1));
        assertEquals(0.7, x, 1e-5);
    }

    @Test
    public void qualquerPosicaoArrastadaFicaDentro() {
        for (int i = -50; i <= 150; i++) {
            double tentativa = i / 100.0;
            double x = PdfPageView.limitar(tentativa, 0.3);
            double y = PdfPageView.limitar(tentativa, 0.07);
            assertTrue("falhou em " + tentativa, Regras.campoDentroDaPagina(x, y, 0.3, 0.07));
        }
    }
}
