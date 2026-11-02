package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.repository.UsuarioRepository;
import br.com.fiap.letssign.util.Sessao;
import br.com.fiap.letssign.util.Tarefas;
import br.com.fiap.letssign.util.Tema;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout campoEmail;
    private TextInputLayout campoSenha;
    private TextInputEditText entradaEmail;
    private TextInputEditText entradaSenha;
    private MaterialButton botaoEntrar;
    private LinearProgressIndicator carregando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sessao sessao = new Sessao(this);
        if (sessao.logado()) {
            abrirInicio(sessao.perfil());
            return;
        }
        setContentView(R.layout.activity_login);

        campoEmail = findViewById(R.id.campo_email);
        campoSenha = findViewById(R.id.campo_senha);
        entradaEmail = findViewById(R.id.entrada_email);
        entradaSenha = findViewById(R.id.entrada_senha);
        botaoEntrar = findViewById(R.id.botao_entrar);
        carregando = findViewById(R.id.carregando);

        botaoEntrar.setOnClickListener(v -> entrar());
        findViewById(R.id.botao_tema).setOnClickListener(v -> Tema.alternar(this));
        entradaSenha.setOnEditorActionListener((v, acao, evento) -> {
            if (acao == EditorInfo.IME_ACTION_DONE) {
                entrar();
                return true;
            }
            return false;
        });
    }

    private void entrar() {
        String email = texto(entradaEmail);
        String senha = entradaSenha.getText() == null ? "" : entradaSenha.getText().toString();
        campoEmail.setError(null);
        campoSenha.setError(null);
        if (email.isEmpty()) {
            campoEmail.setError(getString(R.string.erro_preencha_email));
            return;
        }
        if (senha.isEmpty()) {
            campoSenha.setError(getString(R.string.erro_preencha_senha));
            return;
        }

        definirCarregando(true);
        UsuarioRepository usuarios = Repositorios.usuarios(this);
        Tarefas.executar(this, () -> usuarios.autenticar(email, senha), usuario -> {
            definirCarregando(false);
            if (usuario == null) {
                campoSenha.setError(getString(R.string.erro_login));
                return;
            }
            new Sessao(this).entrar(usuario);
            abrirInicio(usuario.perfil);
        }, erro -> {
            definirCarregando(false);
            campoSenha.setError(getString(R.string.erro_generico, Tarefas.mensagem(erro)));
        });
    }

    private void abrirInicio(Perfil perfil) {
        Class<?> destino = perfil == Perfil.ADMIN ? AdminHomeActivity.class : SignatarioHomeActivity.class;
        startActivity(new Intent(this, destino));
        finish();
    }

    private void definirCarregando(boolean ativo) {
        carregando.setVisibility(ativo ? View.VISIBLE : View.INVISIBLE);
        botaoEntrar.setEnabled(!ativo);
    }

    static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
