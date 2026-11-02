package br.com.fiap.letssign.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.com.fiap.letssign.dominio.Perfil;

@Dao
public interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long inserir(Usuario usuario);

    @Update
    int atualizar(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    Usuario buscarPorEmail(String email);

    @Query("SELECT * FROM usuarios WHERE id = :id")
    Usuario buscarPorId(long id);

    @Query("SELECT * FROM usuarios WHERE perfil = :perfil ORDER BY nomeCompleto COLLATE NOCASE")
    List<Usuario> listarPorPerfil(Perfil perfil);

    @Query("SELECT COUNT(*) FROM usuarios WHERE perfil = :perfil")
    int contarPorPerfil(Perfil perfil);

    @Query("SELECT COUNT(*) FROM usuarios WHERE email = :email AND id != :ignorarId")
    int contarEmail(String email, long ignorarId);
}
