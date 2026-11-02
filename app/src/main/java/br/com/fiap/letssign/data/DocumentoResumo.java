package br.com.fiap.letssign.data;

import br.com.fiap.letssign.dominio.PapelSignatario;
import br.com.fiap.letssign.dominio.StatusDocumento;

/** Linha das listas de documentos (Admin e Signatário), já com as contagens. */
public class DocumentoResumo {
    public long id;
    public String titulo;
    public String nomeArquivo;
    public int versao;
    public StatusDocumento status;
    public long criadoEm;
    public Long enviadoEm;
    public Long concluidoEm;
    public int totalSignatarios;
    public int assinaturas;

    /** Só na lista do Signatário: o papel dele e quando ele assinou. */
    public PapelSignatario papel;
    public Long assinadoEm;
}
