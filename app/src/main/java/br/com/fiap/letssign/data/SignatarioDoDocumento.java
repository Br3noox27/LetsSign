package br.com.fiap.letssign.data;

import br.com.fiap.letssign.dominio.PapelSignatario;

/** Signatário de um documento com os dados da pessoa (junção de usuarios + documento_signatarios). */
public class SignatarioDoDocumento {
    public long usuarioId;
    public String nomeCompleto;
    public String email;
    public PapelSignatario papel;
    public boolean obrigatorio;
    public Long assinadoEm;
}
