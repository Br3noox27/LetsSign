package br.com.fiap.letssign.repository;

import java.util.List;

import br.com.fiap.letssign.data.Usuario;

/**
 * Acesso aos usuários. Hoje a implementação é Room (banco local);
 * na Sprint 2 pode virar uma implementação que chama a API REST.
 * Os métodos são bloqueantes: chamar fora da thread principal.
 */
public interface UsuarioRepository {

    String EMAIL_ADMIN_PADRAO = "admin@letssign.com";
    String SENHA_ADMIN_PADRAO = "admin123";

    /** Máximo de usuários com perfil SIGNATARIO no app (o único lugar onde esse número aparece). */
    int LIMITE_SIGNATARIOS = 10;

    /** Cria o Admin de teste se ele ainda não existir. */
    void garantirAdminPadrao();

    /**
     * Cadastra como SIGNATARIO quem ainda não existe (confere pelo e-mail). Pode ser chamado
     * em toda abertura do app: quem já existe não é duplicado nem alterado. Respeita o
     * LIMITE_SIGNATARIOS (com o limite cheio, ninguém novo entra). Devolve quantos foram criados.
     */
    int garantirSignatarios(List<Usuario> pessoas, String senha);

    /** Devolve o usuário se e-mail e senha conferem; senão, null. */
    Usuario autenticar(String email, String senha);

    Usuario buscarPorId(long id);

    /** Null se não existe ninguém com esse e-mail. */
    Usuario buscarPorEmail(String email);

    List<Usuario> listarSignatarios();

    int contarSignatarios();

    boolean emailEmUso(String email, long ignorarId);

    /**
     * Insere (id == 0) ou atualiza um signatário. Se senhaNova vier vazia numa edição,
     * a senha atual é mantida. Devolve o id.
     *
     * @throws LimiteDeSignatariosException ao cadastrar um novo com o limite já cheio
     */
    long salvarSignatario(Usuario usuario, String senhaNova);
}
