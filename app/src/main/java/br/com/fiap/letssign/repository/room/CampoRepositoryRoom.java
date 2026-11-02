package br.com.fiap.letssign.repository.room;

import java.util.List;

import br.com.fiap.letssign.data.AppDatabase;
import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.repository.CampoRepository;

public class CampoRepositoryRoom implements CampoRepository {

    private final AppDatabase banco;

    public CampoRepositoryRoom(AppDatabase banco) {
        this.banco = banco;
    }

    @Override
    public List<CampoAssinatura> listar(long documentoId) {
        return banco.campoDao().listar(documentoId);
    }

    @Override
    public void substituirTodos(long documentoId, List<CampoAssinatura> campos) {
        banco.runInTransaction(() -> {
            if (banco.documentoDao().buscarStatus(documentoId) != StatusDocumento.RASCUNHO) {
                throw new IllegalStateException("Documento já enviado não pode ser alterado.");
            }
            banco.campoDao().apagarDoDocumento(documentoId);
            for (CampoAssinatura c : campos) {
                c.id = 0;
                c.documentoId = documentoId;
            }
            banco.campoDao().inserir(campos);
        });
    }
}
