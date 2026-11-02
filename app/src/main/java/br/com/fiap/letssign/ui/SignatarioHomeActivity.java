package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.DocumentoResumo;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.repository.DocumentoRepository;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.util.Formatos;
import br.com.fiap.letssign.util.Tarefas;

/** Início do Signatário: documentos recebidos, pendentes e assinados. Só vê o que foi enviado para ele. */
public class SignatarioHomeActivity extends TelaBase {

    private final List<DocumentoResumo> todos = new ArrayList<>();
    private boolean mostrandoPendentes = true;
    private DocumentoAdapter adapter;
    private EstadoVazio vazio;
    private Chip chipPendentes;
    private Chip chipAssinados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirPerfil(Perfil.SIGNATARIO)) {
            return;
        }
        setContentView(R.layout.activity_signatario_home);
        configurarToolbar(R.string.titulo_meus_documentos, false);
        setTitle(getString(R.string.ola_nome, Formatos.primeiroNome(sessao.nome())));

        ChipGroup grupo = findViewById(R.id.filtros);
        chipPendentes = (Chip) getLayoutInflater().inflate(R.layout.chip_filtro, grupo, false);
        chipAssinados = (Chip) getLayoutInflater().inflate(R.layout.chip_filtro, grupo, false);
        chipPendentes.setId(View.generateViewId());
        chipAssinados.setId(View.generateViewId());
        grupo.addView(chipPendentes);
        grupo.addView(chipAssinados);
        chipPendentes.setChecked(true);
        chipPendentes.setOnCheckedChangeListener((c, marcado) -> {
            if (marcado) {
                mostrandoPendentes = true;
                aplicarFiltro();
            }
        });
        chipAssinados.setOnCheckedChangeListener((c, marcado) -> {
            if (marcado) {
                mostrandoPendentes = false;
                aplicarFiltro();
            }
        });

        adapter = new DocumentoAdapter(true, this::abrir);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
        vazio = new EstadoVazio(findViewById(android.R.id.content));
        atualizarChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            carregar();
        }
    }

    private void carregar() {
        long usuarioId = sessao.usuarioId();
        DocumentoRepository documentos = Repositorios.documentos(this);
        Tarefas.executar(this, () -> documentos.listarRecebidos(usuarioId), lista -> {
            todos.clear();
            todos.addAll(lista);
            atualizarChips();
            aplicarFiltro();
        });
    }

    private void atualizarChips() {
        int pendentes = 0;
        for (DocumentoResumo d : todos) {
            if (d.assinadoEm == null) {
                pendentes++;
            }
        }
        chipPendentes.setText(getString(R.string.filtro_com_contagem, getString(R.string.aba_pendentes), pendentes));
        chipAssinados.setText(getString(R.string.filtro_com_contagem, getString(R.string.aba_assinados),
                todos.size() - pendentes));
    }

    private void aplicarFiltro() {
        List<DocumentoResumo> visiveis = new ArrayList<>();
        for (DocumentoResumo d : todos) {
            if ((d.assinadoEm == null) == mostrandoPendentes) {
                visiveis.add(d);
            }
        }
        adapter.mostrar(visiveis);
        if (mostrandoPendentes) {
            vazio.configurar(R.drawable.ic_caixa, R.string.vazio_pendentes_titulo,
                    R.string.vazio_pendentes_detalhe, R.string.botao_atualizar, v -> carregar());
        } else {
            vazio.configurar(R.drawable.ic_caneta, R.string.vazio_assinados_titulo,
                    R.string.vazio_assinados_detalhe, R.string.botao_ver_pendentes, v -> chipPendentes.setChecked(true));
        }
        vazio.mostrar(visiveis.isEmpty());
    }

    private void abrir(DocumentoResumo item) {
        if (item.assinadoEm == null) {
            Intent intent = new Intent(this, AssinarActivity.class);
            intent.putExtra(AssinarActivity.EXTRA_DOCUMENTO_ID, item.id);
            startActivity(intent);
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(item.titulo)
                .setMessage(getString(R.string.detalhe_assinado, Formatos.papel(item.papel),
                        Formatos.dataHora(item.assinadoEm), Formatos.status(item.status)))
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }

}
