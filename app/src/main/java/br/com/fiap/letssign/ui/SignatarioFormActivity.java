package br.com.fiap.letssign.ui;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.dominio.Regras;
import br.com.fiap.letssign.repository.LimiteDeSignatariosException;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.repository.UsuarioRepository;
import br.com.fiap.letssign.util.Tarefas;

/** Cadastro e edição de Signatário: nome completo, e-mail, telefone e senha. */
public class SignatarioFormActivity extends TelaBase {

    public static final String EXTRA_USUARIO_ID = "usuarioId";
    private static final int SENHA_MINIMA = 6;

    private long usuarioId;
    private UsuarioRepository usuarios;
    private TextInputLayout campoNome;
    private TextInputLayout campoEmail;
    private TextInputLayout campoTelefone;
    private TextInputLayout campoSenha;
    private TextInputEditText entradaNome;
    private TextInputEditText entradaEmail;
    private TextInputEditText entradaTelefone;
    private TextInputEditText entradaSenha;
    private MaterialButton botaoSalvar;
    private TextView erroFormulario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirPerfil(Perfil.ADMIN)) {
            return;
        }
        setContentView(R.layout.activity_signatario_form);
        usuarioId = getIntent().getLongExtra(EXTRA_USUARIO_ID, 0);
        boolean edicao = usuarioId != 0;
        configurarToolbar(edicao ? R.string.titulo_editar_signatario : R.string.titulo_novo_signatario, true);
        usuarios = Repositorios.usuarios(this);

        campoNome = findViewById(R.id.campo_nome);
        campoEmail = findViewById(R.id.campo_email);
        campoTelefone = findViewById(R.id.campo_telefone);
        campoSenha = findViewById(R.id.campo_senha);
        entradaNome = findViewById(R.id.entrada_nome);
        entradaEmail = findViewById(R.id.entrada_email);
        entradaTelefone = findViewById(R.id.entrada_telefone);
        entradaSenha = findViewById(R.id.entrada_senha);
        botaoSalvar = findViewById(R.id.botao_salvar);
        erroFormulario = findViewById(R.id.erro_formulario);

        campoSenha.setHelperText(getString(edicao ? R.string.ajuda_senha_manter : R.string.ajuda_senha_nova));
        botaoSalvar.setOnClickListener(v -> salvar());

        if (edicao && savedInstanceState == null) {
            Tarefas.executar(this, () -> usuarios.buscarPorId(usuarioId), usuario -> {
                if (usuario == null || usuario.perfil != Perfil.SIGNATARIO) {
                    finish();
                    return;
                }
                entradaNome.setText(usuario.nomeCompleto);
                entradaEmail.setText(usuario.email);
                entradaTelefone.setText(usuario.telefone);
            });
        }
    }

    private void salvar() {
        String nome = LoginActivity.texto(entradaNome).replaceAll("\\s+", " ");
        String email = LoginActivity.texto(entradaEmail);
        String telefone = LoginActivity.texto(entradaTelefone).replaceAll("\\D", "");
        String senha = entradaSenha.getText() == null ? "" : entradaSenha.getText().toString();

        campoNome.setError(null);
        campoEmail.setError(null);
        campoTelefone.setError(null);
        campoSenha.setError(null);
        erroFormulario.setVisibility(android.view.View.GONE);

        boolean valido = true;
        if (nome.split(" ").length < 2) {
            campoNome.setError(getString(R.string.erro_nome));
            valido = false;
        }
        if (!Regras.emailValido(email)) {
            campoEmail.setError(getString(R.string.erro_email));
            valido = false;
        }
        if (telefone.length() < 10 || telefone.length() > 13) {
            campoTelefone.setError(getString(R.string.erro_telefone));
            valido = false;
        }
        boolean senhaObrigatoria = usuarioId == 0;
        if ((senhaObrigatoria || !senha.isEmpty()) && senha.length() < SENHA_MINIMA) {
            campoSenha.setError(getString(R.string.erro_senha));
            valido = false;
        }
        if (!valido) {
            return;
        }

        botaoSalvar.setEnabled(false);
        Tarefas.executar(this, () -> {
            if (usuarios.emailEmUso(email, usuarioId)) {
                return -1L;
            }
            Usuario u = new Usuario();
            u.id = usuarioId;
            u.nomeCompleto = nome;
            u.email = email;
            u.telefone = telefone;
            return usuarios.salvarSignatario(u, senha);
        }, id -> {
            botaoSalvar.setEnabled(true);
            if (id < 0) {
                campoEmail.setError(getString(R.string.erro_email_em_uso));
                return;
            }
            Toast.makeText(this, R.string.signatario_salvo, Toast.LENGTH_SHORT).show();
            finish();
        }, erro -> {
            botaoSalvar.setEnabled(true);
            if (erro instanceof LimiteDeSignatariosException) {
                // Erro no próprio formulário, sem fechar a tela.
                erroFormulario.setText(getString(R.string.erro_limite_signatarios, UsuarioRepository.LIMITE_SIGNATARIOS));
                erroFormulario.setVisibility(android.view.View.VISIBLE);
            } else if (erro instanceof SQLiteConstraintException) {
                campoEmail.setError(getString(R.string.erro_email_em_uso));
            } else {
                Toast.makeText(this, getString(R.string.erro_generico, Tarefas.mensagem(erro)), Toast.LENGTH_LONG).show();
            }
        });
    }
}
