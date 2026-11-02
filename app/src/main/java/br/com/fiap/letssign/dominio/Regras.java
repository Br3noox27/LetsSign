package br.com.fiap.letssign.dominio;

public class Regras {
    public static boolean campoDentroDaPagina(double x, double y, double largura, double altura) {
        return x >= 0
            && y >= 0
            && x + largura <= 1 
            && y + altura <= 1;

    }

    public static StatusDocumento statusDoDocumento(boolean enviado, int totalSignatarios, int assinaturas) {
        if (!enviado) {
            return StatusDocumento.RASCUNHO;
        }
        if (assinaturas == 0) {
            return StatusDocumento.ENVIADO;
        }
        if ( assinaturas < totalSignatarios) {
            return StatusDocumento.PARCIAL;
        }
        return StatusDocumento.CONCLUIDO;
    }

    // Devolve a lista de problemas encontrados; lista vazia = pode enviar.
    public static java.util.List<String> validarEnvio(String titulo, boolean temPdf, java.util.List<String> emailsSignatarios) {
        java.util.List<String> erros = new java.util.ArrayList<>();
        if (titulo == null || titulo.trim().isEmpty()) {
            erros.add("Informe o título do documento.");
        }
        if (!temPdf) {
            erros.add("Escolha o arquivo PDF.");
        }
        if (emailsSignatarios == null || emailsSignatarios.isEmpty()) {
            erros.add("Adicione pelo menos 1 signatário.");
        } else {
            for (String email : emailsSignatarios) {
                if (!emailValido(email)) {
                    erros.add("E-mail inválido: " + email);
                }
            }
        }
        return erros;
    }

    // Formato simples: algo@dominio.ext, sem espaços.
    public static boolean emailValido(String email) {
        return email != null && email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

}


