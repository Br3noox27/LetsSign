package br.com.fiap.letssign.repository;

import android.content.Context;

import br.com.fiap.letssign.data.AppDatabase;
import br.com.fiap.letssign.repository.room.CampoRepositoryRoom;
import br.com.fiap.letssign.repository.room.DocumentoRepositoryRoom;
import br.com.fiap.letssign.repository.room.EventoRepositoryRoom;
import br.com.fiap.letssign.repository.room.UsuarioRepositoryRoom;
import br.com.fiap.letssign.servico.EnvioDocumentoService;

/**
 * Único lugar que sabe qual implementação usar. Para trocar o Room por uma API REST,
 * basta devolver outra implementação aqui; as telas não mudam.
 */
public final class Repositorios {

    private Repositorios() {
    }

    public static UsuarioRepository usuarios(Context context) {
        return new UsuarioRepositoryRoom(AppDatabase.get(context));
    }

    public static DocumentoRepository documentos(Context context) {
        return new DocumentoRepositoryRoom(AppDatabase.get(context));
    }

    public static CampoRepository campos(Context context) {
        return new CampoRepositoryRoom(AppDatabase.get(context));
    }

    public static EventoRepository eventos(Context context) {
        return new EventoRepositoryRoom(AppDatabase.get(context));
    }

    public static EnvioDocumentoService envio(Context context) {
        return new EnvioDocumentoService(documentos(context), campos(context));
    }
}
