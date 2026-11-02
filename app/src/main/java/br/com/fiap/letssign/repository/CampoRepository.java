package br.com.fiap.letssign.repository;

import java.util.List;

import br.com.fiap.letssign.data.CampoAssinatura;

/** Campos de assinatura posicionados no PDF. */
public interface CampoRepository {

    List<CampoAssinatura> listar(long documentoId);

    /**
     * Troca todos os campos do documento pela lista nova.
     * Lança IllegalStateException se o documento já foi enviado.
     */
    void substituirTodos(long documentoId, List<CampoAssinatura> campos);
}
