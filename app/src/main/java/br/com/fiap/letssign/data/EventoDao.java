package br.com.fiap.letssign.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventoDao {

    @Insert
    long inserir(EventoDocumento evento);

    @Query("SELECT e.tipo, e.dataHora, u.nomeCompleto AS nomeUsuario "
            + "FROM eventos_documento e LEFT JOIN usuarios u ON u.id = e.usuarioId "
            + "WHERE e.documentoId = :documentoId ORDER BY e.dataHora, e.id")
    List<EventoDetalhado> listar(long documentoId);
}
