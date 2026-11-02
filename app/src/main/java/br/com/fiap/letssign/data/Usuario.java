package br.com.fiap.letssign.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import br.com.fiap.letssign.dominio.Perfil;

/** Pessoa que entra no app: Admin ou Signatário. O e-mail é a credencial e não se repete. */
@Entity(tableName = "usuarios", indices = {@Index(value = "email", unique = true)})
public class Usuario {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nomeCompleto = "";

    @NonNull
    public String email = "";

    public String telefone;

    /** "sal:hash" em Base64. A senha em texto nunca é gravada. */
    @NonNull
    public String senhaHash = "";

    @NonNull
    public Perfil perfil = Perfil.SIGNATARIO;
}
