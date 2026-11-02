package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.repository.UsuarioRepository;

/** Início do Admin: barra inferior com Documentos e Signatários e o botão de ação principal. */
public class AdminHomeActivity extends TelaBase {

    private static final String TAG_DOCUMENTOS = "documentos";
    private static final String TAG_SIGNATARIOS = "signatarios";
    private static final String ESTADO_ABA_DOCUMENTOS = "abaDocumentos";

    private ExtendedFloatingActionButton botaoPrincipal;
    private boolean naAbaDocumentos = true;
    /** Quantos signatários existem (-1 = ainda não sabe). Com o limite cheio, o botão trava. */
    private int totalSignatarios = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirPerfil(Perfil.ADMIN)) {
            return;
        }
        setContentView(R.layout.activity_admin_home);
        configurarToolbar(R.string.titulo_documentos, false);

        botaoPrincipal = findViewById(R.id.botao_principal);
        botaoPrincipal.setOnClickListener(v -> {
            if (naAbaDocumentos) {
                novoDocumento();
            } else {
                novoSignatario();
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            fm.beginTransaction()
                    .add(R.id.conteudo, new DocumentosFragment(), TAG_DOCUMENTOS)
                    .add(R.id.conteudo, new SignatariosFragment(), TAG_SIGNATARIOS)
                    .commitNow();
        }

        BottomNavigationView navegacao = findViewById(R.id.navegacao);
        navegacao.setOnItemSelectedListener(item -> {
            mostrarAba(item.getItemId() == R.id.aba_documentos);
            return true;
        });
        // Ao recriar a tela (girar, trocar o tema) volta para a mesma aba.
        boolean abaDocumentos = savedInstanceState == null || savedInstanceState.getBoolean(ESTADO_ABA_DOCUMENTOS, true);
        navegacao.setSelectedItemId(abaDocumentos ? R.id.aba_documentos : R.id.aba_signatarios);
        mostrarAba(abaDocumentos);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putBoolean(ESTADO_ABA_DOCUMENTOS, naAbaDocumentos);
    }

    private void mostrarAba(boolean documentos) {
        naAbaDocumentos = documentos;
        FragmentManager fm = getSupportFragmentManager();
        Fragment docs = fm.findFragmentByTag(TAG_DOCUMENTOS);
        Fragment sigs = fm.findFragmentByTag(TAG_SIGNATARIOS);
        if (docs == null || sigs == null) {
            return;
        }
        fm.beginTransaction()
                .show(documentos ? docs : sigs)
                .hide(documentos ? sigs : docs)
                .commitNow();
        // Aba Documentos: a logo (rabisco + LetsSign) fica no lugar do título.
        setTitle(documentos ? R.string.titulo_documentos : R.string.titulo_signatarios);
        findViewById(R.id.logo_barra).setVisibility(documentos ? View.VISIBLE : View.GONE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(!documentos);
        }
        atualizarBotaoPrincipal();
        botaoPrincipal.extend();
    }

    void definirTotalSignatarios(int total) {
        totalSignatarios = total;
        atualizarBotaoPrincipal();
    }

    private void atualizarBotaoPrincipal() {
        boolean limiteCheio = !naAbaDocumentos && totalSignatarios >= UsuarioRepository.LIMITE_SIGNATARIOS;
        botaoPrincipal.setEnabled(!limiteCheio);
        botaoPrincipal.setAlpha(limiteCheio ? 0.5f : 1f);
        if (naAbaDocumentos) {
            botaoPrincipal.setText(R.string.botao_novo_documento);
        } else {
            botaoPrincipal.setText(limiteCheio ? R.string.limite_signatarios_atingido : R.string.botao_novo_signatario);
        }
    }

    void novoDocumento() {
        startActivity(new Intent(this, NovoDocumentoActivity.class));
    }

    void novoSignatario() {
        startActivity(new Intent(this, SignatarioFormActivity.class));
    }

    ExtendedFloatingActionButton botaoPrincipal() {
        return botaoPrincipal;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }

}
