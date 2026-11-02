package br.com.fiap.letssign.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import br.com.fiap.letssign.dominio.StatusDocumento;

/**
 * Documento para assinatura. O PDF fica na pasta de arquivos do app;
 * aqui ficam o nome, o caminho, as datas e o status.
 */
@Entity(tableName = "documentos",
        foreignKeys = @ForeignKey(entity = Usuario.class, parentColumns = "id", childColumns = "criadoPor"),
        indices = {@Index("criadoPor")})
public class Documento {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String titulo = "";

    /** Nome original do arquivo escolhido no celular. */
    public String nomeArquivo;

    /** Caminho da cópia do PDF dentro da pasta do app. */
    public String caminhoPdf;

    public int quantidadePaginas;

    @NonNull
    public StatusDocumento status = StatusDocumento.RASCUNHO;

    public int versao = 1;

    /** Preenchido quando este documento é uma nova versão de outro (fase futura). */
    public Long documentoOrigemId;

    public long criadoPor;

    public long criadoEm;

    public Long enviadoEm;

    public Long concluidoEm;
}
