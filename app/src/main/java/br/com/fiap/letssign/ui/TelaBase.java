package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.widget.Toast;

import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.util.Sessao;
import br.com.fiap.letssign.util.Tema;

/** O que toda tela logada tem: sessão, conferência de perfil e barra do topo. */
public abstract class TelaBase extends AppCompatActivity {

    protected Sessao sessao;

    /**
     * Confere se quem está logado tem o perfil pedido. Se não tiver, volta para o login.
     * Chamar no começo do onCreate e parar se devolver false.
     */
    protected boolean exigirPerfil(Perfil perfil) {
        sessao = new Sessao(this);
        if (sessao.logado() && sessao.perfil() == perfil) {
            return true;
        }
        Toast.makeText(this, R.string.sem_acesso, Toast.LENGTH_SHORT).show();
        irParaLogin();
        return false;
    }

    protected void configurarToolbar(@StringRes int titulo, boolean comVoltar) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(titulo);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(comVoltar);
        }
    }

    /** Botões da barra do topo comuns: tema claro/escuro e sair. */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_tema) {
            Tema.alternar(this);
            return true;
        }
        if (item.getItemId() == R.id.item_sair) {
            sair();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    protected void sair() {
        new Sessao(this).sair();
        irParaLogin();
    }

    private void irParaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
