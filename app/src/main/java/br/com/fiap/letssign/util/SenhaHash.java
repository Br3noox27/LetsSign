package br.com.fiap.letssign.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Senha com SHA-256 e sal aleatório. Guarda "sal:hash" (Base64).
 * Para produção o ideal é um algoritmo lento (PBKDF2/bcrypt); aqui basta para o protótipo.
 */
public final class SenhaHash {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SenhaHash() {
    }

    public static String gerar(String senha) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        Base64.Encoder base64 = Base64.getEncoder();
        return base64.encodeToString(sal) + ":" + base64.encodeToString(resumo(sal, senha));
    }

    public static boolean confere(String senha, String guardado) {
        if (senha == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split(":");
        if (partes.length != 2) {
            return false;
        }
        try {
            byte[] sal = Base64.getDecoder().decode(partes[0]);
            byte[] esperado = Base64.getDecoder().decode(partes[1]);
            return MessageDigest.isEqual(esperado, resumo(sal, senha));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] resumo(byte[] sal, String senha) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(sal);
            return sha.digest(senha.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
