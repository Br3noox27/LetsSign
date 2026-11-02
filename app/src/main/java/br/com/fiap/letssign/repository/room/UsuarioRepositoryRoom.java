package br.com.fiap.letssign.repository.room;

import java.util.List;
import java.util.Locale;

import br.com.fiap.letssign.data.AppDatabase;
import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.data.UsuarioDao;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.repository.LimiteDeSignatariosException;
import br.com.fiap.letssign.repository.UsuarioRepository;
import br.com.fiap.letssign.util.SenhaHash;

public class UsuarioRepositoryRoom implements UsuarioRepository {

    private final AppDatabase banco;
    private final UsuarioDao dao;

    public UsuarioRepositoryRoom(AppDatabase banco) {
        this.banco = banco;
        this.dao = banco.usuarioDao();
    }

    @Override
    public void garantirAdminPadrao() {
        if (dao.buscarPorEmail(EMAIL_ADMIN_PADRAO) != null) {
            return;
        }
        Usuario admin = new Usuario();
        admin.nomeCompleto = "Administrador LetsSign";
        admin.email = EMAIL_ADMIN_PADRAO;
        admin.telefone = "";
        admin.perfil = Perfil.ADMIN;
        admin.senhaHash = SenhaHash.gerar(SENHA_ADMIN_PADRAO);
        dao.inserir(admin);
    }

    @Override
    public int garantirSignatarios(List<Usuario> pessoas, String senha) {
        return banco.runInTransaction(() -> {
            int criados = 0;
            for (Usuario modelo : pessoas) {
                String email = normalizar(modelo.email);
                if (dao.buscarPorEmail(email) != null) {
                    continue;
                }
                if (dao.contarPorPerfil(Perfil.SIGNATARIO) >= LIMITE_SIGNATARIOS) {
                    break; // limite cheio: não passa de LIMITE_SIGNATARIOS
                }
                Usuario novo = new Usuario();
                novo.nomeCompleto = modelo.nomeCompleto;
                novo.email = email;
                novo.telefone = modelo.telefone == null ? "" : modelo.telefone;
                novo.perfil = Perfil.SIGNATARIO;
                novo.senhaHash = SenhaHash.gerar(senha);
                dao.inserir(novo);
                criados++;
            }
            return criados;
        });
    }

    @Override
    public Usuario autenticar(String email, String senha) {
        Usuario usuario = dao.buscarPorEmail(normalizar(email));
        if (usuario == null || !SenhaHash.confere(senha, usuario.senhaHash)) {
            return null;
        }
        return usuario;
    }

    @Override
    public Usuario buscarPorId(long id) {
        return dao.buscarPorId(id);
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        return dao.buscarPorEmail(normalizar(email));
    }

    @Override
    public List<Usuario> listarSignatarios() {
        return dao.listarPorPerfil(Perfil.SIGNATARIO);
    }

    @Override
    public int contarSignatarios() {
        return dao.contarPorPerfil(Perfil.SIGNATARIO);
    }

    @Override
    public boolean emailEmUso(String email, long ignorarId) {
        return dao.contarEmail(normalizar(email), ignorarId) > 0;
    }

    @Override
    public long salvarSignatario(Usuario usuario, String senhaNova) {
        usuario.email = normalizar(usuario.email);
        usuario.perfil = Perfil.SIGNATARIO;
        boolean trocarSenha = senhaNova != null && !senhaNova.isEmpty();

        if (usuario.id == 0) {
            // Conferir e inserir na mesma transação: dois cadastros juntos não passam do limite.
            return banco.runInTransaction(() -> {
                if (dao.contarPorPerfil(Perfil.SIGNATARIO) >= LIMITE_SIGNATARIOS) {
                    throw new LimiteDeSignatariosException(LIMITE_SIGNATARIOS);
                }
                usuario.senhaHash = SenhaHash.gerar(senhaNova);
                usuario.id = dao.inserir(usuario);
                return usuario.id;
            });
        }

        Usuario atual = dao.buscarPorId(usuario.id);
        if (atual == null || atual.perfil != Perfil.SIGNATARIO) {
            throw new IllegalStateException("Signatário não encontrado.");
        }
        usuario.senhaHash = trocarSenha ? SenhaHash.gerar(senhaNova) : atual.senhaHash;
        dao.atualizar(usuario);
        return usuario.id;
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
