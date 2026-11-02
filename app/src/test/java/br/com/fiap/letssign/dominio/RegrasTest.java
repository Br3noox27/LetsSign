package br.com.fiap.letssign.dominio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

// Cópia do teste do Breno (fase3-java-framework), adaptada de JUnit 5 para JUnit 4.
public class RegrasTest {

    @Test
    public void campoDentroDaPaginaConfereOsQuatroCasos() {
        assertTrue(Regras.campoDentroDaPagina(0.1, 0.1, 0.3, 0.1));      // caso 1
        assertFalse(Regras.campoDentroDaPagina(0.8, 0.1, 0.3, 0.1));     // caso 2
        assertFalse(Regras.campoDentroDaPagina(-0.1, 0.5, 0.2, 0.1));     // caso 3
        assertTrue(Regras.campoDentroDaPagina(0.5, 0.5, 0.5, 0.5));      // caso 4
    }

    @Test
    public void statusDoDocumentoVemDasContagens() {

        assertEquals(StatusDocumento.RASCUNHO, Regras.statusDoDocumento(false, 2, 0));  // caso 1
        assertEquals(StatusDocumento.ENVIADO, Regras.statusDoDocumento(true, 2, 0));   // caso 2
        assertEquals(StatusDocumento.PARCIAL, Regras.statusDoDocumento(true, 2, 1));   // caso 3
        assertEquals(StatusDocumento.CONCLUIDO, Regras.statusDoDocumento(true, 2, 2));   // caso 4
        assertEquals(StatusDocumento.CONCLUIDO, Regras.statusDoDocumento(true, 1, 1));   // caso 5
    }

}
