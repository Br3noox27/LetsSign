package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.AssinaturaDoSignatario;
import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.EventoDetalhado;
import br.com.fiap.letssign.data.SignatarioDoDocumento;
import br.com.fiap.letssign.data.TipoEvento;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.repository.CampoRepository;
import br.com.fiap.letssign.repository.DocumentoRepository;
import br.com.fiap.letssign.repository.EventoRepository;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.util.CoresSignatario;
import br.com.fiap.letssign.util.Formatos;
import br.com.fiap.letssign.util.ImagemAssinatura;
import br.com.fiap.letssign.util.Tarefas;
import br.com.fiap.letssign.util.VisualizadorPdf;

/**
 * Acompanhamento do documento (Admin): situação geral, signatários, linha do tempo
 * (Criado, Enviado, Assinando, Concluído) e histórico. Também cria a nova versão.
 */
public class DocumentoDetalheActivity extends TelaBase {

    public static final String EXTRA_DOCUMENTO_ID = "documentoId";

    private static class Dados {
        Documento documento;
        List<SignatarioDoDocumento> signatarios;
        List<CampoAssinatura> campos;
        List<EventoDetalhado> eventos;
        /** Só do Admin que criou o documento (o repositório filtra). */
        List<AssinaturaDoSignatario> assinaturas;
        final Map<Long, Bitmap> imagens = new HashMap<>();
    }

    private long documentoId;
    private long adminId;
    private DocumentoRepository documentos;
    private CampoRepository campos;
    private EventoRepository eventos;
    private VisualizadorPdf visualizador;
    private boolean pdfAberto;
    private Documento documento;

    private View barraNovaVersao;
    private View progressoNovaVersao;
    private MaterialButton botaoNovaVersao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirPerfil(Perfil.ADMIN)) {
            return;
        }
        setContentView(R.layout.activity_documento_detalhe);
        configurarToolbar(R.string.titulo_detalhe, true);
        documentoId = getIntent().getLongExtra(EXTRA_DOCUMENTO_ID, 0);
        adminId = sessao.usuarioId();
        documentos = Repositorios.documentos(this);
        campos = Repositorios.campos(this);
        eventos = Repositorios.eventos(this);
        visualizador = new VisualizadorPdf(this, findViewById(R.id.visualizador));
        visualizador.vista().setEditavel(false);

        barraNovaVersao = findViewById(R.id.barra_nova_versao);
        progressoNovaVersao = findViewById(R.id.progresso_nova_versao);
        botaoNovaVersao = findViewById(R.id.botao_nova_versao);
        botaoNovaVersao.setOnClickListener(v -> confirmarNovaVersao());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (visualizador == null) {
            return;
        }
        Tarefas.executar(this, () -> {
            Dados d = new Dados();
            d.documento = documentos.buscarDoAdmin(documentoId, adminId);
            if (d.documento != null) {
                d.signatarios = documentos.listarSignatarios(documentoId);
                d.campos = campos.listar(documentoId);
                d.eventos = eventos.listar(documentoId);
                d.assinaturas = documentos.listarAssinaturas(documentoId, adminId);
                for (AssinaturaDoSignatario a : d.assinaturas) {
                    Bitmap imagem = ImagemAssinatura.carregar(a.caminhoAssinatura);
                    if (imagem != null) {
                        d.imagens.put(a.usuarioId, imagem);
                    }
                }
            }
            return d;
        }, this::mostrar);
    }

    private void mostrar(Dados d) {
        if (d.documento == null) {
            Toast.makeText(this, R.string.documento_nao_encontrado, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        documento = d.documento;
        Documento doc = d.documento;
        int assinados = 0;
        for (SignatarioDoDocumento s : d.signatarios) {
            if (s.assinadoEm != null) {
                assinados++;
            }
        }
        int total = d.signatarios.size();

        ((TextView) findViewById(R.id.titulo_documento)).setText(Formatos.tituloComVersao(this, doc.titulo, doc.versao));
        ((TextView) findViewById(R.id.info_documento)).setText(getString(R.string.info_arquivo, doc.nomeArquivo,
                getResources().getQuantityString(R.plurals.paginas, doc.quantidadePaginas, doc.quantidadePaginas)));
        Formatos.pintarChipStatus((Chip) findViewById(R.id.chip_status), doc.status);
        ((LinearProgressIndicator) findViewById(R.id.progresso_assinaturas)).setProgress(Formatos.progresso(assinados, total));
        String textoAssinaturas = getString(R.string.assinaturas_x_de_y, assinados, total);
        if (doc.status == StatusDocumento.CONCLUIDO) {
            textoAssinaturas = getString(R.string.assinaturas_concluido, textoAssinaturas);
        }
        ((TextView) findViewById(R.id.texto_assinaturas)).setText(textoAssinaturas);

        mostrarSignatarios(d.assinaturas, d.imagens);
        mostrarLinhaDoTempo(doc, d.eventos, assinados, total);
        mostrarHistorico(d.eventos);

        Map<Long, Integer> cores = new HashMap<>();
        Map<Long, String> rotulos = new HashMap<>();
        for (int i = 0; i < d.signatarios.size(); i++) {
            cores.put(d.signatarios.get(i).usuarioId, CoresSignatario.corNaPagina(i));
            rotulos.put(d.signatarios.get(i).usuarioId, Formatos.primeiroNome(d.signatarios.get(i).nomeCompleto));
        }
        visualizador.vista().setSignatarios(cores, rotulos);
        visualizador.vista().setCampos(d.campos);
        // "Documento assinado": a imagem de cada assinatura aparece dentro do campo (só na tela).
        visualizador.vista().setAssinaturas(d.imagens);
        configurarVerAssinado(d.campos, d.imagens);
        if (!pdfAberto) {
            pdfAberto = true;
            visualizador.abrir(doc.caminhoPdf, 0, erro ->
                    ((TextView) findViewById(R.id.info_documento)).setText(R.string.erro_abrir_pdf));
        }

        // Nova versão: só de documento já enviado.
        barraNovaVersao.setVisibility(doc.status != StatusDocumento.RASCUNHO ? View.VISIBLE : View.GONE);
    }

    /** Um cartão por pessoa: nome e papel, data e hora, e a imagem da assinatura (se já assinou). */
    private void mostrarSignatarios(List<AssinaturaDoSignatario> lista, Map<Long, Bitmap> imagens) {
        LinearLayout pai = findViewById(R.id.lista_signatarios);
        pai.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (AssinaturaDoSignatario s : lista) {
            View linha = inflater.inflate(R.layout.item_signatario_situacao, pai, false);
            boolean assinou = s.assinadoEm != null;
            ((TextView) linha.findViewById(R.id.nome)).setText(s.nomeCompleto);
            ((TextView) linha.findViewById(R.id.papel)).setText(Formatos.papel(s.papel));
            Formatos.pintarChip(linha.findViewById(R.id.chip_situacao),
                    getString(assinou ? R.string.assinou : R.string.pendente),
                    ContextCompat.getColor(this, assinou ? R.color.status_concluido : R.color.status_enviado));
            ((TextView) linha.findViewById(R.id.data_assinatura)).setText(assinou
                    ? getString(R.string.assinou_em, Formatos.dataHora(s.assinadoEm))
                    : getString(R.string.ainda_nao_assinou));
            ImageView imagem = linha.findViewById(R.id.imagem_assinatura);
            Bitmap bitmap = imagens.get(s.usuarioId);
            if (assinou && bitmap != null) {
                imagem.setImageBitmap(bitmap);
                imagem.setContentDescription(getString(R.string.assinatura_de, s.nomeCompleto));
                imagem.setVisibility(View.VISIBLE);
            } else {
                imagem.setVisibility(View.GONE);
            }
            pai.addView(linha);
        }
    }

    /** Leva até a página do PDF com a primeira assinatura, desenhada dentro do campo. */
    private void configurarVerAssinado(List<CampoAssinatura> lista, Map<Long, Bitmap> imagens) {
        MaterialButton botao = findViewById(R.id.botao_ver_assinado);
        int pagina = -1;
        for (CampoAssinatura c : lista) {
            if (imagens.containsKey(c.signatarioId) && (pagina < 0 || c.pagina < pagina)) {
                pagina = c.pagina;
            }
        }
        botao.setVisibility(pagina >= 0 ? View.VISIBLE : View.GONE);
        int paginaAssinada = pagina;
        botao.setOnClickListener(v -> {
            visualizador.mostrar(paginaAssinada);
            ScrollView rolagem = findViewById(R.id.rolagem);
            rolagem.smoothScrollTo(0, findViewById(R.id.secao_documento).getTop());
        });
    }

    /** As 4 etapas, como no protótipo (e no rastreio de pedido). */
    private void mostrarLinhaDoTempo(Documento doc, List<EventoDetalhado> lista, int assinados, int total) {
        LinearLayout pai = findViewById(R.id.linha_do_tempo);
        pai.removeAllViews();
        boolean enviado = doc.status != StatusDocumento.RASCUNHO;
        boolean concluido = doc.status == StatusDocumento.CONCLUIDO;
        boolean assinando = doc.status == StatusDocumento.ENVIADO || doc.status == StatusDocumento.PARCIAL;

        adicionarEtapa(pai, getString(R.string.etapa_criado), quando(lista, TipoEvento.CRIADO), true, false, true);
        adicionarEtapa(pai, getString(R.string.etapa_enviado), quando(lista, TipoEvento.ENVIADO), enviado, !enviado, true);
        adicionarEtapa(pai, getString(R.string.etapa_assinando, assinados, total),
                assinados > 0 ? quando(lista, TipoEvento.ASSINADO) : null, concluido, assinando, true);
        adicionarEtapa(pai, getString(R.string.etapa_concluido), quando(lista, TipoEvento.CONCLUIDO), concluido, false, false);
    }

    /** Data/hora do último evento do tipo, ou null. */
    private static String quando(List<EventoDetalhado> lista, TipoEvento tipo) {
        String resposta = null;
        for (EventoDetalhado e : lista) {
            if (e.tipo == tipo) {
                resposta = Formatos.dataHora(e.dataHora);
            }
        }
        return resposta;
    }

    private void adicionarEtapa(LinearLayout pai, String titulo, String data, boolean feita, boolean atual,
                                boolean temProxima) {
        View etapa = getLayoutInflater().inflate(R.layout.item_etapa, pai, false);
        ImageView ponto = etapa.findViewById(R.id.ponto_etapa);
        ponto.setBackgroundResource(feita ? R.drawable.ponto_feito : atual ? R.drawable.ponto_atual : R.drawable.ponto_pendente);
        ponto.setImageAlpha(feita ? 255 : 0);

        View traco = etapa.findViewById(R.id.traco_etapa);
        traco.setVisibility(temProxima ? View.VISIBLE : View.INVISIBLE);
        traco.setBackgroundColor(ContextCompat.getColor(this, feita ? R.color.cor_primaria : R.color.cor_linha));

        TextView textoTitulo = etapa.findViewById(R.id.titulo_etapa);
        textoTitulo.setText(titulo);
        if (!feita && !atual) {
            textoTitulo.setTextColor(ContextCompat.getColor(this, R.color.cor_secundario));
        }
        String sub = data != null ? data : getString(atual ? R.string.etapa_em_andamento : R.string.etapa_aguardando);
        ((TextView) etapa.findViewById(R.id.data_etapa)).setText(sub);
        pai.addView(etapa);
    }

    private void mostrarHistorico(List<EventoDetalhado> lista) {
        LinearLayout pai = findViewById(R.id.lista_historico);
        pai.removeAllViews();
        for (EventoDetalhado e : lista) {
            View linha = getLayoutInflater().inflate(R.layout.item_historico, pai, false);
            String texto = textoEvento(e.tipo);
            if (e.tipo == TipoEvento.ASSINADO && e.nomeUsuario != null) {
                texto = getString(R.string.evento_assinou_nome, Formatos.primeiroNome(e.nomeUsuario));
            }
            ((TextView) linha.findViewById(R.id.texto_evento)).setText(texto);
            ((TextView) linha.findViewById(R.id.data_evento)).setText(Formatos.dataHoraCurta(e.dataHora));
            pai.addView(linha);
        }
    }

    private String textoEvento(TipoEvento tipo) {
        switch (tipo) {
            case CRIADO:
                return getString(R.string.evento_criado);
            case ENVIADO:
                return getString(R.string.evento_enviado);
            case ASSINADO:
                return getString(R.string.evento_assinado);
            default:
                return getString(R.string.evento_concluido);
        }
    }

    // ---------- Nova versão ----------

    private void confirmarNovaVersao() {
        if (documento == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.nova_versao_titulo)
                .setMessage(getString(R.string.nova_versao_msg, documento.versao + 1))
                .setPositiveButton(R.string.botao_criar, (dlg, qual) -> criarNovaVersao())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void criarNovaVersao() {
        Documento original = documento;
        botaoNovaVersao.setEnabled(false);
        progressoNovaVersao.setVisibility(View.VISIBLE);
        Tarefas.executar(this, () -> {
            // A nova versão ganha a sua própria cópia do PDF: trocar o arquivo nela não afeta o original.
            File copia = copiarPdf(original.caminhoPdf);
            try {
                return documentos.criarNovaVersao(original.id, adminId, copia.getAbsolutePath(), System.currentTimeMillis());
            } catch (RuntimeException e) {
                if (!copia.delete()) {
                    copia.deleteOnExit();
                }
                throw e;
            }
        }, novoId -> {
            botaoNovaVersao.setEnabled(true);
            progressoNovaVersao.setVisibility(View.GONE);
            Toast.makeText(this, R.string.nova_versao_criada, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, NovoDocumentoActivity.class);
            intent.putExtra(NovoDocumentoActivity.EXTRA_DOCUMENTO_ID, novoId);
            startActivity(intent);
        }, erro -> {
            botaoNovaVersao.setEnabled(true);
            progressoNovaVersao.setVisibility(View.GONE);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.nova_versao_falha)
                    .setMessage(Tarefas.mensagem(erro))
                    .setPositiveButton(R.string.ok, null)
                    .show();
        });
    }

    private File copiarPdf(String caminho) throws IOException {
        File origem = caminho == null ? null : new File(caminho);
        if (origem == null || !origem.isFile()) {
            throw new IOException(getString(R.string.erro_abrir_pdf));
        }
        File destino = new File(origem.getParentFile(), UUID.randomUUID() + ".pdf");
        Files.copy(origem.toPath(), destino.toPath());
        return destino;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visualizador != null) {
            visualizador.fechar();
        }
    }
}
