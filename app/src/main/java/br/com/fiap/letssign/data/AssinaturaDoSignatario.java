package br.com.fiap.letssign.data;

import br.com.fiap.letssign.dominio.PapelSignatario;

/**
 * Assinatura de uma pessoa num documento, com o caminho da imagem (PNG).
 * Só sai do banco para o Admin que criou o documento (ver DocumentoDao.listarAssinaturasDoAdmin).
 */
public class AssinaturaDoSignatario {
    public long usuarioId;
    public String nomeCompleto;
    public PapelSignatario papel;
    public Long assinadoEm;
    public String caminhoAssinatura;
}
