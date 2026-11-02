package br.com.fiap.letssign.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/** Fechar "Novo documento" sem salvar não pode deixar a cópia do PDF esquecida na pasta do app. */
public class ArquivosPdfTest {

    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    @Test
    public void copiaNuncaGravadaEApagada() throws Exception {
        File copia = pasta.newFile("copia.pdf");
        assertTrue(ArquivosPdf.apagarCopiaNaoGravada(copia.getAbsolutePath(), null));
        assertFalse(copia.exists());
    }

    @Test
    public void copiaTrocadaEApagadaMasAGravadaFica() throws Exception {
        File gravada = pasta.newFile("gravada.pdf");
        File nova = pasta.newFile("nova.pdf");
        assertTrue(ArquivosPdf.apagarCopiaNaoGravada(nova.getAbsolutePath(), gravada.getAbsolutePath()));
        assertFalse(nova.exists());
        assertTrue(gravada.exists());
    }

    @Test
    public void copiaQueJaEstaNoBancoNaoEApagada() throws Exception {
        File gravada = pasta.newFile("gravada.pdf");
        assertFalse(ArquivosPdf.apagarCopiaNaoGravada(gravada.getAbsolutePath(), gravada.getAbsolutePath()));
        assertTrue(gravada.exists());
    }

    @Test
    public void semPdfNaTelaNaoFazNada() {
        assertFalse(ArquivosPdf.apagarCopiaNaoGravada(null, null));
        assertFalse(ArquivosPdf.apagarCopiaNaoGravada(new File(pasta.getRoot(), "sumiu.pdf").getAbsolutePath(), null));
    }
}
