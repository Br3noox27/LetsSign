package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.SignatarioDoDocumento;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.repository.CampoRepository;
import br.com.fiap.letssign.repository.DocumentoRepository;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.util.CoresSignatario;
import br.com.fiap.letssign.util.Formatos;
import br.com.fiap.letssign.util.PdfPageView;
import br.com.fiap.letssign.util.Tarefas;
import br.com.fiap.letssign.util.VisualizadorPdf;

/**
 * Passo 2 do fluxo: mostra as páginas do PDF e deixa adicionar, arrastar e excluir
 * campos de assinatura para cada signatário. Tudo em proporção da página (0 a 1).
 */
public class PosicionarCamposActivity extends TelaBase {

    public static final String EXTRA_DOCUMENTO_ID = "documentoId";
    private static final String ESTADO_PAGINA = "pagina";

    /** Tamanho inicial do campo, em proporção da página. */
    private static final double LARGURA_PADRAO = 0.30;
    private static final double ALTURA_PADRAO = 0.07;

    private static class Dados {
        Documento documento;
        List<SignatarioDoDocumento> signatarios;
        List<CampoAssinatura> campos;
    }

    private long documentoId;
    private long adminId;
    private DocumentoRepository documentos;
    private CampoRepository camposRepo;

    private final List<CampoAssinatura> campos = new ArrayList<>();
    private final List<SignatarioDoDocumento> signatarios = new ArrayList<>();
    private final Map<Integer, Long> signatarioPorChip = new HashMap<>();
    private final Map<Long, Integer> chipPorSignatario = new HashMap<>();
    private long signatarioAtivo;
    private CampoAssinatura selecionado;
    private boolean alterado;
    private boolean pronto;

    private View raiz;
    private VisualizadorPdf visualizador;
    private PdfPageView vista;
    private ChipGroup grupoSignatarios;
    private TextView textoResumo;
    private MaterialButton botaoAdicionar;
    private MaterialButton botaoExcluir;
    private MaterialButton botaoRevisar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirPerfil(Perfil.ADMIN)) {
            return;
        }
        setContentView(R.layout.activity_posicionar_campos);
        configurarToolbar(R.string.titulo_posicionar, true);
        documentoId = getIntent().getLongExtra(EXTRA_DOCUMENTO_ID, 0);
        adminId = sessao.usuarioId();
        documentos = Repositorios.documentos(this);
        camposRepo = Repositorios.campos(this);

        Passos.marcar(this, 2);
        raiz = findViewById(R.id.raiz);
        grupoSignatarios = findViewById(R.id.grupo_signatarios);
        textoResumo = findViewById(R.id.texto_resumo_campos);
        botaoAdicionar = findViewById(R.id.botao_adicionar_campo);
        botaoExcluir = findViewById(R.id.botao_excluir_campo);
        botaoRevisar = findViewById(R.id.botao_revisar);

        visualizador = new VisualizadorPdf(this, findViewById(R.id.visualizador));
        vista = visualizador.vista();
        vista.setEditavel(true);
        vista.setOuvinte(new PdfPageView.Ouvinte() {
            @Override
            public void aoSelecionar(CampoAssinatura campo) {
                selecionar(campo);
            }

            @Override
            public void aoMover(CampoAssinatura campo) {
                alterado = true;
            }
        });
        visualizador.setAoTrocarPagina((pagina, total) -> {
            selecionar(null);
            atualizarResumo();
        });

        botaoAdicionar.setOnClickListener(v -> adicionarCampo());
        botaoExcluir.setOnClickListener(v -> excluirCampo());
        botaoRevisar.setOnClickListener(v -> revisar());
        botaoAdicionar.setEnabled(false);
        botaoRevisar.setEnabled(false);

        int paginaInicial = savedInstanceState != null ? savedInstanceState.getInt(ESTADO_PAGINA) : 0;
        Tarefas.executar(this, () -> {
            Dados d = new Dados();
            d.documento = documentos.buscarDoAdmin(documentoId, adminId);
            d.signatarios = documentos.listarSignatarios(documentoId);
            d.campos = camposRepo.listar(documentoId);
            return d;
        }, d -> aplicar(d, paginaInicial));
    }

    private void aplicar(Dados d, int paginaInicial) {
        if (d.documento == null) {
            Toast.makeText(this, R.string.documento_nao_encontrado, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (d.documento.status != StatusDocumento.RASCUNHO) {
            Toast.makeText(this, R.string.documento_ja_enviado, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        signatarios.clear();
        signatarios.addAll(d.signatarios);
        campos.clear();
        campos.addAll(d.campos);
        montarChips();
        vista.setCampos(campos);
        pronto = true;
        botaoAdicionar.setEnabled(!signatarios.isEmpty());
        botaoRevisar.setEnabled(true);
        visualizador.abrir(d.documento.caminhoPdf, paginaInicial, erro ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.dialogo_arquivo_invalido)
                        .setMessage(R.string.erro_abrir_pdf)
                        .setPositiveButton(R.string.ok, (dlg, qual) -> finish())
                        .setCancelable(false)
                        .show());
        atualizarResumo();
    }

    private void montarChips() {
        grupoSignatarios.removeAllViews();
        signatarioPorChip.clear();
        chipPorSignatario.clear();
        Map<Long, Integer> cores = new HashMap<>();
        Map<Long, String> rotulos = new HashMap<>();
        for (int i = 0; i < signatarios.size(); i++) {
            SignatarioDoDocumento s = signatarios.get(i);
            int cor = CoresSignatario.cor(this, i);
            cores.put(s.usuarioId, CoresSignatario.corNaPagina(i));
            rotulos.put(s.usuarioId, Formatos.primeiroNome(s.nomeCompleto));

            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_signatario, grupoSignatarios, false);
            chip.setId(View.generateViewId());
            chip.setText(getString(R.string.chip_signatario, Formatos.primeiroNome(s.nomeCompleto), Formatos.papel(s.papel)));
            chip.setChipStrokeColor(ColorStateList.valueOf(cor));
            signatarioPorChip.put(chip.getId(), s.usuarioId);
            chipPorSignatario.put(s.usuarioId, chip.getId());
            grupoSignatarios.addView(chip);
        }
        vista.setSignatarios(cores, rotulos);
        grupoSignatarios.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (!ids.isEmpty()) {
                Long id = signatarioPorChip.get(ids.get(0));
                signatarioAtivo = id == null ? 0 : id;
            }
        });
        if (!signatarios.isEmpty()) {
            Integer primeiro = chipPorSignatario.get(signatarios.get(0).usuarioId);
            if (primeiro != null) {
                grupoSignatarios.check(primeiro);
            }
        }
    }

    private void adicionarCampo() {
        if (!pronto || visualizador.totalPaginas() == 0 || signatarioAtivo == 0) {
            return;
        }
        int pagina = visualizador.paginaAtual();
        int nestaPagina = contarNaPagina(pagina);
        // Cada campo novo nasce um pouco abaixo do anterior para não ficar um em cima do outro.
        double deslocamento = (nestaPagina % 6) * 0.08;

        CampoAssinatura c = new CampoAssinatura();
        c.documentoId = documentoId;
        c.signatarioId = signatarioAtivo;
        c.pagina = pagina;
        c.largura = LARGURA_PADRAO;
        c.altura = ALTURA_PADRAO;
        c.x = PdfPageView.limitar(0.5 - LARGURA_PADRAO / 2, c.largura);
        c.y = PdfPageView.limitar(0.35 + deslocamento, c.altura);
        campos.add(c);
        alterado = true;
        vista.setCampos(campos);
        selecionar(c);
        atualizarResumo();
    }

    private void excluirCampo() {
        CampoAssinatura removido = selecionado;
        if (removido == null) {
            return;
        }
        int posicao = campos.indexOf(removido);
        campos.remove(removido);
        alterado = true;
        selecionar(null);
        vista.setCampos(campos);
        atualizarResumo();
        Snackbar.make(raiz, R.string.campo_excluido, Snackbar.LENGTH_LONG)
                .setAnchorView(botaoRevisar)
                .setAction(R.string.desfazer, v -> {
                    if (!campos.contains(removido)) {
                        campos.add(Math.min(posicao, campos.size()), removido);
                        alterado = true;
                        vista.setCampos(campos);
                        atualizarResumo();
                    }
                })
                .show();
    }

    private void selecionar(CampoAssinatura campo) {
        selecionado = campo;
        vista.setSelecionado(campo);
        botaoExcluir.setEnabled(campo != null);
        if (campo != null) {
            Integer chip = chipPorSignatario.get(campo.signatarioId);
            if (chip != null) {
                grupoSignatarios.check(chip);
            }
        }
    }

    private int contarNaPagina(int pagina) {
        int total = 0;
        for (CampoAssinatura c : campos) {
            if (c.pagina == pagina) {
                total++;
            }
        }
        return total;
    }

    private void atualizarResumo() {
        textoResumo.setText(getString(R.string.resumo_campos,
                getResources().getQuantityString(R.plurals.campos, campos.size(), campos.size()),
                contarNaPagina(visualizador.paginaAtual())));
    }

    private List<CampoAssinatura> copiaDosCampos() {
        List<CampoAssinatura> copia = new ArrayList<>();
        for (CampoAssinatura c : campos) {
            copia.add(c.copia());
        }
        return copia;
    }

    private void revisar() {
        List<CampoAssinatura> copia = copiaDosCampos();
        botaoRevisar.setEnabled(false);
        Tarefas.executar(this, () -> {
            camposRepo.substituirTodos(documentoId, copia);
            return true;
        }, ok -> {
            botaoRevisar.setEnabled(true);
            alterado = false;
            Intent intent = new Intent(this, RevisaoEnvioActivity.class);
            intent.putExtra(RevisaoEnvioActivity.EXTRA_DOCUMENTO_ID, documentoId);
            startActivity(intent);
        }, erro -> {
            botaoRevisar.setEnabled(true);
            Snackbar.make(raiz, getString(R.string.erro_generico, Tarefas.mensagem(erro)), Snackbar.LENGTH_LONG)
                    .setAnchorView(botaoRevisar).show();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Salva ao sair da tela (voltar, girar o celular): nada do que foi posicionado se perde.
        if (pronto && alterado) {
            List<CampoAssinatura> copia = copiaDosCampos();
            alterado = false;
            Tarefas.emSegundoPlano(() -> camposRepo.substituirTodos(documentoId, copia));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        if (visualizador != null) {
            estado.putInt(ESTADO_PAGINA, visualizador.paginaAtual());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visualizador != null) {
            visualizador.fechar();
        }
    }
}
