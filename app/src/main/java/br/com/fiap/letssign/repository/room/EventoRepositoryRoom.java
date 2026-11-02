package br.com.fiap.letssign.repository.room;

import java.util.List;

import br.com.fiap.letssign.data.AppDatabase;
import br.com.fiap.letssign.data.EventoDetalhado;
import br.com.fiap.letssign.data.EventoDocumento;
import br.com.fiap.letssign.repository.EventoRepository;

public class EventoRepositoryRoom implements EventoRepository {

    private final AppDatabase banco;

    public EventoRepositoryRoom(AppDatabase banco) {
        this.banco = banco;
    }

    @Override
    public void registrar(EventoDocumento evento) {
        banco.eventoDao().inserir(evento);
    }

    @Override
    public List<EventoDetalhado> listar(long documentoId) {
        return banco.eventoDao().listar(documentoId);
    }
}
