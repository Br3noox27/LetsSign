package br.com.fiap.letssign.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Usuario.class, Documento.class, DocumentoSignatario.class,
        CampoAssinatura.class, EventoDocumento.class},
        version = 1, exportSchema = false)
@TypeConverters(Conversores.class)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instancia;

    public abstract UsuarioDao usuarioDao();

    public abstract DocumentoDao documentoDao();

    public abstract CampoDao campoDao();

    public abstract EventoDao eventoDao();

    public static AppDatabase get(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "letssign.db").build();
                }
            }
        }
        return instancia;
    }
}
