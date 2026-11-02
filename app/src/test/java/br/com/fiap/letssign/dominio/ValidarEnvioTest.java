package br.com.fiap.letssign.dominio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

// Testes da regra validarEnvio: título, PDF e signatários antes de enviar.
public class ValidarEnvioTest {

    private static final List<String> UM_EMAIL = Collections.singletonList("ana@empresa.com");

    @Test
    public void tudoCertoNaoTemErro() {
        assertTrue(Regras.validarEnvio("Contrato de serviço", true, UM_EMAIL).isEmpty());
    }

    @Test
    public void semSignatarioNaoEnvia() {
        List<String> erros = Regras.validarEnvio("Contrato", true, Collections.emptyList());
        assertEquals(1, erros.size());
        assertTrue(erros.get(0).contains("signatário"));
    }

    @Test
    public void listaNulaContaComoSemSignatario() {
        assertEquals(1, Regras.validarEnvio("Contrato", true, null).size());
    }

    @Test
    public void tituloEmBrancoNaoEnvia() {
        assertEquals(1, Regras.validarEnvio("   ", true, UM_EMAIL).size());
        assertEquals(1, Regras.validarEnvio(null, true, UM_EMAIL).size());
    }

    @Test
    public void semPdfNaoEnvia() {
        List<String> erros = Regras.validarEnvio("Contrato", false, UM_EMAIL);
        assertEquals(1, erros.size());
        assertTrue(erros.get(0).contains("PDF"));
    }

    @Test
    public void emailInvalidoNaoEnvia() {
        List<String> erros = Regras.validarEnvio("Contrato", true,
                Arrays.asList("ana@empresa.com", "joao.empresa.com"));
        assertEquals(1, erros.size());
        assertTrue(erros.get(0).contains("joao.empresa.com"));
    }

    @Test
    public void juntaTodosOsErros() {
        assertEquals(3, Regras.validarEnvio("", false, Collections.emptyList()).size());
    }

    @Test
    public void formatoDeEmail() {
        assertTrue(Regras.emailValido("admin@letssign.com"));
        assertFalse(Regras.emailValido("admin@letssign"));
        assertFalse(Regras.emailValido("admin letssign@x.com"));
        assertFalse(Regras.emailValido("@letssign.com"));
        assertFalse(Regras.emailValido(""));
        assertFalse(Regras.emailValido(null));
    }
}
