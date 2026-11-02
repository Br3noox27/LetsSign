package br.com.fiap.letssign.dominio;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// Cópia do teste do Breno, adaptada de JUnit 5 para JUnit 4.
public class PerfilTest {

    @Test
    public void temOsDoisPerfisDoEnunciado() {
        assertEquals(2, Perfil.values().length);
        assertEquals("ADMIN", Perfil.ADMIN.name());
        assertEquals("SIGNATARIO", Perfil.SIGNATARIO.name());
    }
}
