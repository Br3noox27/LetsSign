package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.SignatarioDoDocumento;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.repository.CampoRepository;
import br.com.fiap.letssign.repository.DocumentoRepository;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.servico.EnvioDocumentoService;
import br.com.fiap.letssign.util.CoresSignatario;
import br.com.fiap.letssign.util.Formatos;
import br.com.fiap.letssign.util.Tarefas;
import br.com.fiap.letssign.util.VisualizadorPdf;

/**
 * Passo 3 do fluxo: mostra tudo antes do envio (documento, signatários, campos e convite)
 * e envia. Se falhar, dá para tentar de novo: o serviço não duplica o envio.
 */
public class RevisaoEnvioActivity extends TelaBase {

    public static final String EXTRA_DOCUMENTO_ID = "documentoId";

    private static class Dados {
        Documento documento;
        List<SignatarioDoDocumento> signatarios;
        List<CampoAssinatura> campos;
        List<String> problemas;
    }

    private long documentoId;
    private long adminId;
    private DocumentoRepository documentos;
    private CampoRepository campos;
    private EnvioDocumentoService envio;

    private VisualizadorPdf visualizador;
    private TextView tituloDocumento;
    private TextView infoDocumento;
    private View cartaoProblemas;
    private TextView textoProblemas;
    private LinearLayout listaSignatarios;
    private TextView textoConvite;
    private TextView textoResumo;
    private View progressoEnvio;
    private MaterialButton botaoEnviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirPerfil(Perfil.ADMIN)) {
            return;
        }
        setContentView(R.layout.activity_revisao_envio);
        configurarToolbar(R.string.titulo_revisao, true);
        documentoId = getIntent().getLongExtra(EXTRA_DOCUMENTO_ID, 0);
        adminId = sessao.usuarioId();
        documentos = Repositorios.documentos(this);
        campos = Repositorios.campos(this);
        envio = Repositorios.envio(this);

        Passos.marcar(this, 3);
        tituloDocumento = findViewById(R.id.titulo_documento);
        infoDocumento = findViewById(R.id.info_documento);
        cartaoProblemas = findViewById(R.id.cartao_problemas);
        textoProblemas = findViewById(R.id.texto_problemas);
        listaSignatarios = findViewById(R.id.lista_signatarios);
        textoConvite = findViewById(R.id.texto_convite);
        textoResumo = findViewById(R.id.texto_resumo);
        progressoEnvio = findViewById(R.id.progresso_envio);
        botaoEnviar = findViewById(R.id.botao_enviar);
        visualizador = new VisualizadorPdf(this, findViewById(R.id.visualizador));
        visualizador.vista().setEditavel(false);

        botaoEnviar.setEnabled(false);
        botaoEnviar.setOnClickListener(v -> enviar());
        carregar();
    }

    private void carregar() {
        Tarefas.executar(this, () -> {
            Dados d = new Dados();
            d.documento = documentos.buscarDoAdmin(documentoId, adminId);
            if (d.documento != null) {
                d.signatarios = documentos.listarSignatarios(documentoId);
                d.campos = campos.listar(documentoId);
                d.problemas = EnvioDocumentoService.validar(d.documento, d.signatarios, d.campos);
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
        Documento doc = d.documento;
        tituloDocumento.setText(Formatos.tituloComVersao(this, doc.titulo, doc.versao));
        infoDocumento.setText(getString(R.string.info_arquivo, doc.nomeArquivo,
                getResources().getQuantityString(R.plurals.paginas, doc.quantidadePaginas, doc.quantidadePaginas)));
        textoConvite.setText(getString(R.string.mensagem_convite, doc.titulo));

        Map<Long, Integer> cores = new HashMap<>();
        Map<Long, String> rotulos = new HashMap<>();
        listaSignatarios.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < d.signatarios.size(); i++) {
            SignatarioDoDocumento s = d.signatarios.get(i);
            cores.put(s.usuarioId, CoresSignatario.corNaPagina(i));
            rotulos.put(s.usuarioId, Formatos.primeiroNome(s.nomeCompleto));

            View linha = inflater.inflate(R.layout.item_pessoa, listaSignatarios, false);
            linha.findViewById(R.id.barra_cor).setBackgroundColor(CoresSignatario.cor(this, i));
            ((TextView) linha.findViewById(R.id.texto_principal)).setText(
                    getString(R.string.nome_e_papel, s.nomeCompleto, Formatos.papel(s.papel)));
            ((TextView) linha.findViewById(R.id.texto_secundario)).setText(s.email);
            ((TextView) linha.findViewById(R.id.texto_terciario)).setText(resumoCampos(s.usuarioId, d.campos));
            listaSignatarios.addView(linha);
        }
        visualizador.vista().setSignatarios(cores, rotulos);
        visualizador.vista().setCampos(d.campos);
        visualizador.abrir(doc.caminhoPdf, 0, erro -> {
            cartaoProblemas.setVisibility(View.VISIBLE);
            textoProblemas.setText(R.string.erro_abrir_pdf);
        });

        textoResumo.setText(getString(R.string.resumo_envio,
                getResources().getQuantityString(R.plurals.signatarios, d.signatarios.size(), d.signatarios.size()),
                getResources().getQuantityString(R.plurals.campos, d.campos.size(), d.campos.size())));

        if (!d.problemas.isEmpty()) {
            cartaoProblemas.setVisibility(View.VISIBLE);
            textoProblemas.setText(listaComMarcadores(getString(R.string.antes_de_enviar), d.problemas));
        } else {
            cartaoProblemas.setVisibility(View.GONE);
        }

        if (doc.status != StatusDocumento.RASCUNHO) {
            botaoEnviar.setEnabled(false);
            botaoEnviar.setText(getString(R.string.enviado_em, Formatos.dataHora(doc.enviadoEm)));
        } else {
            botaoEnviar.setEnabled(true);
        }
    }

    /** Ex.: "Campos: página 1 (2), página 3 (1)". */
    private String resumoCampos(long usuarioId, List<CampoAssinatura> lista) {
        TreeMap<Integer, Integer> porPagina = new TreeMap<>();
        for (CampoAssinatura c : lista) {
            if (c.signatarioId == usuarioId) {
                Integer atual = porPagina.get(c.pagina);
                porPagina.put(c.pagina, atual == null ? 1 : atual + 1);
            }
        }
        if (porPagina.isEmpty()) {
            return getString(R.string.sem_campos);
        }
        List<String> partes = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : porPagina.entrySet()) {
            partes.add(getString(R.string.pagina_n_quantidade, e.getKey() + 1, e.getValue()));
        }
        return getString(R.string.campos_por_pagina, String.join(", ", partes));
    }

    private static String listaComMarcadores(String titulo, List<String> itens) {
        StringBuilder texto = new StringBuilder(titulo);
        for (String item : itens) {
            texto.append("\n- ").append(item);
        }
        return texto.toString();
    }

    private void enviar() {
        botaoEnviar.setEnabled(false);
        progressoEnvio.setVisibility(View.VISIBLE);
        Tarefas.executar(this, () -> envio.enviar(documentoId, adminId, System.currentTimeMillis()), resultado -> {
            progressoEnvio.setVisibility(View.INVISIBLE);
            botaoEnviar.setEnabled(true);
            switch (resultado.situacao) {
                case ENVIADO:
                    mostrarResultado(R.string.dialogo_enviado_titulo, getString(R.string.dialogo_enviado_msg,
                            getResources().getQuantityString(R.plurals.signatarios,
                                    resultado.signatarios, resultado.signatarios),
                            Formatos.dataHora(resultado.enviadoEm)));
                    break;
                case JA_ENVIADO:
                    mostrarResultado(R.string.dialogo_ja_enviado_titulo,
                            getString(R.string.dialogo_ja_enviado_msg, Formatos.dataHora(resultado.enviadoEm)));
                    break;
                default:
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.dialogo_corrija)
                            .setMessage(listaComMarcadores(getString(R.string.antes_de_enviar), resultado.erros))
                            .setPositiveButton(R.string.botao_ajustar, (dlg, qual) -> finish())
                            .setNegativeButton(R.string.cancelar, null)
                            .show();
                    break;
            }
        }, erro -> {
            progressoEnvio.setVisibility(View.INVISIBLE);
            botaoEnviar.setEnabled(true);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialogo_falha_titulo)
                    .setMessage(getString(R.string.dialogo_falha_msg, Tarefas.mensagem(erro)))
                    .setPositiveButton(R.string.tentar_novamente, (dlg, qual) -> enviar())
                    .setNegativeButton(R.string.cancelar, null)
                    .show();
        });
    }

    private void mostrarResultado(int titulo, String mensagem) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setCancelable(false)
                .setPositiveButton(R.string.ver_acompanhamento, (dlg, qual) -> {
                    Intent inicio = new Intent(this, AdminHomeActivity.class);
                    inicio.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(inicio);
                    Intent detalhe = new Intent(this, DocumentoDetalheActivity.class);
                    detalhe.putExtra(DocumentoDetalheActivity.EXTRA_DOCUMENTO_ID, documentoId);
                    startActivity(detalhe);
                    finish();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visualizador != null) {
            visualizador.fechar();
        }
    }
}
