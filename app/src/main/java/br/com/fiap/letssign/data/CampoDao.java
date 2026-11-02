package br.com.fiap.letssign.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CampoDao {

    @Query("SELECT * FROM campos_assinatura WHERE documentoId = :documentoId ORDER BY pagina, id")
    List<CampoAssinatura> listar(long documentoId);

    @Insert
    void inserir(List<CampoAssinatura> campos);

    @Query("DELETE FROM campos_assinatura WHERE documentoId = :documentoId")
    void apagarDoDocumento(long documentoId);

    /** Apaga os campos de quem saiu da lista de signatários. */
    @Query("DELETE FROM campos_assinatura WHERE documentoId = :documentoId AND signatarioId NOT IN (:mantidos)")
    void apagarDeQuemSaiu(long documentoId, List<Long> mantidos);
}
