package br.com.fiap.letssign.dominio;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// Cópia do teste do Breno, adaptada de JUnit 5 para JUnit 4.
public class StatusDocumentoTest {

    @Test
    public void temOsQuatroStatusDoEnunciado() {
        assertEquals(4, StatusDocumento.values().length);
        assertEquals("RASCUNHO", StatusDocumento.RASCUNHO.name());
        assertEquals("ENVIADO", StatusDocumento.ENVIADO.name());
        assertEquals("PARCIAL", StatusDocumento.PARCIAL.name());
        assertEquals("CONCLUIDO", StatusDocumento.CONCLUIDO.name());
    }
}
