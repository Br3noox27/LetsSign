package br.com.fiap.letssign.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Lugar da assinatura no PDF. Posição e tamanho são PROPORÇÃO da página (0 a 1),
 * por isso o campo fica no mesmo lugar em qualquer tela.
 */
@Entity(tableName = "campos_assinatura",
        foreignKeys = {
                @ForeignKey(entity = Documento.class, parentColumns = "id", childColumns = "documentoId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Usuario.class, parentColumns = "id", childColumns = "signatarioId")
        },
        indices = {@Index("documentoId"), @Index("signatarioId")})
public class CampoAssinatura {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long documentoId;

    /** Id do usuário (signatário) dono do campo. */
    public long signatarioId;

    /** Página começando em 0. */
    public int pagina;

    public double x;
    public double y;
    public double largura;
    public double altura;

    public CampoAssinatura copia() {
        CampoAssinatura c = new CampoAssinatura();
        c.id = id;
        c.documentoId = documentoId;
        c.signatarioId = signatarioId;
        c.pagina = pagina;
        c.x = x;
        c.y = y;
        c.largura = largura;
        c.altura = altura;
        return c;
    }
}
