package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.DocumentoResumo;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.repository.DocumentoRepository;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.util.Sessao;
import br.com.fiap.letssign.util.Tarefas;

/** Lista de documentos do Admin com filtros por status (chips com contagem). */
public class DocumentosFragment extends Fragment {

    /** null = Todos. A ordem segue a dos chips. */
    private static final StatusDocumento[] FILTROS = {null, StatusDocumento.RASCUNHO,
            StatusDocumento.ENVIADO, StatusDocumento.PARCIAL, StatusDocumento.CONCLUIDO};
    private static final int[] NOMES_FILTROS = {R.string.filtro_todos, R.string.filtro_rascunho,
            R.string.filtro_enviado, R.string.filtro_parcial, R.string.filtro_concluido};

    private final List<DocumentoResumo> todos = new ArrayList<>();
    private final Chip[] chips = new Chip[FILTROS.length];
    private int filtroAtual;
    private DocumentoAdapter adapter;
    private EstadoVazio vazio;
    private TextView textoEmAndamento;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View raiz = inflater.inflate(R.layout.fragment_documentos, container, false);

        ChipGroup grupo = raiz.findViewById(R.id.filtros);
        for (int i = 0; i < FILTROS.length; i++) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_filtro, grupo, false);
            chip.setId(View.generateViewId());
            int posicao = i;
            chip.setOnCheckedChangeListener((c, marcado) -> {
                if (marcado) {
                    filtroAtual = posicao;
                    aplicarFiltro();
                }
            });
            grupo.addView(chip);
            chips[i] = chip;
        }
        chips[0].setChecked(true);
        atualizarChips();

        adapter = new DocumentoAdapter(false, this::abrir);
        RecyclerView lista = raiz.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        lista.setAdapter(adapter);
        lista.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                AdminHomeActivity tela = (AdminHomeActivity) requireActivity();
                if (dy > 8) {
                    tela.botaoPrincipal().shrink();
                } else if (dy < -8) {
                    tela.botaoPrincipal().extend();
                }
            }
        });

        vazio = new EstadoVazio(raiz);
        textoEmAndamento = raiz.findViewById(R.id.texto_em_andamento);
        atualizarEmAndamento();
        return raiz;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            carregar();
        }
    }

    private void carregar() {
        long adminId = new Sessao(requireContext()).usuarioId();
        DocumentoRepository documentos = Repositorios.documentos(requireContext());
        Tarefas.executar(requireActivity(), () -> documentos.listarDoAdmin(adminId), lista -> {
            if (!isAdded()) {
                return;
            }
            todos.clear();
            todos.addAll(lista);
            atualizarChips();
            atualizarEmAndamento();
            aplicarFiltro();
        });
    }

    /** "N em andamento": enviados que ainda não foram concluídos. */
    private void atualizarEmAndamento() {
        if (textoEmAndamento != null) {
            int n = contar(StatusDocumento.ENVIADO) + contar(StatusDocumento.PARCIAL);
            textoEmAndamento.setText(getString(R.string.em_andamento, n));
        }
    }

    private void atualizarChips() {
        for (int i = 0; i < FILTROS.length; i++) {
            chips[i].setText(getString(R.string.filtro_com_contagem, getString(NOMES_FILTROS[i]), contar(FILTROS[i])));
        }
    }

    private int contar(StatusDocumento status) {
        if (status == null) {
            return todos.size();
        }
        int total = 0;
        for (DocumentoResumo d : todos) {
            if (d.status == status) {
                total++;
            }
        }
        return total;
    }

    private void aplicarFiltro() {
        if (adapter == null) {
            return;
        }
        StatusDocumento filtro = FILTROS[filtroAtual];
        List<DocumentoResumo> visiveis = new ArrayList<>();
        for (DocumentoResumo d : todos) {
            if (filtro == null || d.status == filtro) {
                visiveis.add(d);
            }
        }
        adapter.mostrar(visiveis);
        if (todos.isEmpty()) {
            // Sem botão aqui: o botão flutuante "Novo documento" já faz isso.
            vazio.configurar(R.drawable.ic_documento, R.string.vazio_documentos_titulo,
                    R.string.vazio_documentos_detalhe, 0, null);
        } else {
            vazio.configurar(R.drawable.ic_caixa, R.string.vazio_filtro_titulo,
                    R.string.vazio_filtro_detalhe, R.string.filtro_ver_todos, v -> chips[0].setChecked(true));
        }
        vazio.mostrar(visiveis.isEmpty());
    }

    private void abrir(DocumentoResumo item) {
        Intent intent;
        if (item.status == StatusDocumento.RASCUNHO) {
            intent = new Intent(requireContext(), NovoDocumentoActivity.class);
            intent.putExtra(NovoDocumentoActivity.EXTRA_DOCUMENTO_ID, item.id);
        } else {
            intent = new Intent(requireContext(), DocumentoDetalheActivity.class);
            intent.putExtra(DocumentoDetalheActivity.EXTRA_DOCUMENTO_ID, item.id);
        }
        startActivity(intent);
    }
}
