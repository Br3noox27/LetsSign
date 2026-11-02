package br.com.fiap.letssign.repository;

import java.util.List;

import br.com.fiap.letssign.data.EventoDetalhado;
import br.com.fiap.letssign.data.EventoDocumento;

/** Histórico do documento (criação, envio, assinaturas). */
public interface EventoRepository {

    void registrar(EventoDocumento evento);

    List<EventoDetalhado> listar(long documentoId);
}
