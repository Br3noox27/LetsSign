package br.com.fiap.letssign.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import br.com.fiap.letssign.dominio.PapelSignatario;

/** Quem assina qual documento, com qual papel. */
@Entity(tableName = "documento_signatarios",
        primaryKeys = {"documentoId", "usuarioId"},
        foreignKeys = {
                @ForeignKey(entity = Documento.class, parentColumns = "id", childColumns = "documentoId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Usuario.class, parentColumns = "id", childColumns = "usuarioId")
        },
        indices = {@Index("usuarioId")})
public class DocumentoSignatario {

    public long documentoId;

    public long usuarioId;

    @NonNull
    public PapelSignatario papel = PapelSignatario.CONTRATANTE;

    public boolean obrigatorio = true;

    /** Nulo enquanto a assinatura estiver pendente. */
    public Long assinadoEm;

    /** Imagem (PNG) da assinatura desenhada, na pasta do app. */
    public String caminhoAssinatura;

    public DocumentoSignatario() {
    }

    @androidx.room.Ignore
    public DocumentoSignatario(long documentoId, long usuarioId, @NonNull PapelSignatario papel) {
        this.documentoId = documentoId;
        this.usuarioId = usuarioId;
        this.papel = papel;
    }
}
