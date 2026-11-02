package br.com.fiap.letssign.data;

import androidx.room.TypeConverter;

import br.com.fiap.letssign.dominio.PapelSignatario;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.dominio.StatusDocumento;

/** Ensina o Room a gravar os enums como texto (o nome do valor). */
public class Conversores {

    @TypeConverter
    public static String dePerfil(Perfil valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static Perfil paraPerfil(String texto) {
        return texto == null ? null : Perfil.valueOf(texto);
    }

    @TypeConverter
    public static String deStatus(StatusDocumento valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static StatusDocumento paraStatus(String texto) {
        return texto == null ? null : StatusDocumento.valueOf(texto);
    }

    @TypeConverter
    public static String dePapel(PapelSignatario valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static PapelSignatario paraPapel(String texto) {
        return texto == null ? null : PapelSignatario.valueOf(texto);
    }

    @TypeConverter
    public static String deTipoEvento(TipoEvento valor) {
        return valor == null ? null : valor.name();
    }

    @TypeConverter
    public static TipoEvento paraTipoEvento(String texto) {
        return texto == null ? null : TipoEvento.valueOf(texto);
    }
}
