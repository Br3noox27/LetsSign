package br.com.fiap.letssign.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SenhaHashTest {

    @Test
    public void senhaCertaConfere() {
        String guardado = SenhaHash.gerar("admin123");
        assertTrue(SenhaHash.confere("admin123", guardado));
    }

    @Test
    public void senhaErradaNaoConfere() {
        String guardado = SenhaHash.gerar("admin123");
        assertFalse(SenhaHash.confere("admin124", guardado));
        assertFalse(SenhaHash.confere("", guardado));
        assertFalse(SenhaHash.confere(null, guardado));
    }

    @Test
    public void naoGuardaASenhaEmTexto() {
        String guardado = SenhaHash.gerar("admin123");
        assertFalse(guardado.contains("admin123"));
    }

    @Test
    public void mesmoTextoGeraHashDiferente() {
        // O sal aleatório faz duas senhas iguais terem hashes diferentes.
        assertNotEquals(SenhaHash.gerar("admin123"), SenhaHash.gerar("admin123"));
    }

    @Test
    public void valorGuardadoEstragadoNaoConfere() {
        assertFalse(SenhaHash.confere("admin123", "sem-separador"));
        assertFalse(SenhaHash.confere("admin123", "@@@:###"));
    }
}
