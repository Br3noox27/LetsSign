package br.com.fiap.letssign.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Histórico do documento: criado, enviado, assinado, concluído. */
@Entity(tableName = "eventos_documento",
        foreignKeys = @ForeignKey(entity = Documento.class, parentColumns = "id", childColumns = "documentoId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("documentoId")})
public class EventoDocumento {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long documentoId;

    @NonNull
    public TipoEvento tipo = TipoEvento.CRIADO;

    public long dataHora;

    /** Quem fez a ação. */
    public long usuarioId;

    public EventoDocumento() {
    }

    @androidx.room.Ignore
    public EventoDocumento(long documentoId, @NonNull TipoEvento tipo, long dataHora, long usuarioId) {
        this.documentoId = documentoId;
        this.tipo = tipo;
        this.dataHora = dataHora;
        this.usuarioId = usuarioId;
    }
}
