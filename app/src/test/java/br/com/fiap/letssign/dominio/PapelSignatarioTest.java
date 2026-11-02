package br.com.fiap.letssign.dominio;

import static org.junit.Assert.assertEquals;  // (A) a ferramenta que compara
import org.junit.Test;

// Cópia do teste do Breno, adaptada de JUnit 5 para JUnit 4.
public class PapelSignatarioTest {

    @Test
    public void temOsTresTurnos() {
        assertEquals(3, PapelSignatario.values().length);
        assertEquals("CONTRATANTE", PapelSignatario.CONTRATANTE.name());
        assertEquals("CONTRATADO", PapelSignatario.CONTRATADO.name());
        assertEquals("TESTEMUNHA", PapelSignatario.TESTEMUNHA.name());

    }
}
