package br.com.fiap.letssign.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import br.com.fiap.letssign.util.AssinaturaView;
import br.com.fiap.letssign.util.CoresSignatario;
import br.com.fiap.letssign.util.Formatos;
import br.com.fiap.letssign.util.Tarefas;
import br.com.fiap.letssign.util.VisualizadorPdf;

/**
 * Signatário assina com o dedo. A imagem da assinatura fica na pasta do app e o status
 * do documento é recalculado (Regras.statusDoDocumento). A assinatura ainda não é
 * gravada dentro do PDF.
 */
public class AssinarActivity extends TelaBase {

    public static final String EXTRA_DOCUMENTO_ID = "documentoId";

    private static class Dados {
        Documento documento;
        SignatarioDoDocumento eu;
        List<CampoAssinatura> meusCampos = new ArrayList<>();
    }

    private long documentoId;
    private long usuarioId;
    private DocumentoRepository documentos;
    private CampoRepository campos;
    private VisualizadorPdf visualizador;
    private AssinaturaView quadro;
    private MaterialButton botaoAssinar;
    private MaterialButton botaoLimpar;
    private View progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirPerfil(Perfil.SIGNATARIO)) {
            return;
        }
        setContentView(R.layout.activity_assinar);
        configurarToolbar(R.string.titulo_assinar, true);
        documentoId = getIntent().getLongExtra(EXTRA_DOCUMENTO_ID, 0);
        usuarioId = sessao.usuarioId();
        documentos = Repositorios.documentos(this);
        campos = Repositorios.campos(this);

        visualizador = new VisualizadorPdf(this, findViewById(R.id.visualizador));
        visualizador.vista().setEditavel(false);
        quadro = findViewById(R.id.quadro_assinatura);
        botaoAssinar = findViewById(R.id.botao_assinar);
        botaoLimpar = findViewById(R.id.botao_limpar);
        progresso = findViewById(R.id.progresso_assinatura);

        quadro.setAoMudar(vazia -> botaoAssinar.setEnabled(!vazia));
        botaoLimpar.setOnClickListener(v -> quadro.limpar());
        botaoAssinar.setOnClickListener(v -> confirmar());

        Tarefas.executar(this, () -> {
            Dados d = new Dados();
            d.documento = documentos.buscarRecebido(documentoId, usuarioId);
            if (d.documento != null) {
                for (SignatarioDoDocumento s : documentos.listarSignatarios(documentoId)) {
                    if (s.usuarioId == usuarioId) {
                        d.eu = s;
                    }
                }
                for (CampoAssinatura c : campos.listar(documentoId)) {
                    if (c.signatarioId == usuarioId) {
                        d.meusCampos.add(c);
                    }
                }
            }
            return d;
        }, this::mostrar);
    }

    private void mostrar(Dados d) {
        if (d.documento == null || d.eu == null) {
            Toast.makeText(this, R.string.documento_nao_encontrado, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setTitle(d.documento.titulo);
        TextView info = findViewById(R.id.info_assinatura);
        int primeiraPagina = d.meusCampos.isEmpty() ? 0 : d.meusCampos.get(0).pagina;
        info.setText(getString(R.string.info_assinar, Formatos.papel(d.eu.papel),
                getResources().getQuantityString(R.plurals.campos, d.meusCampos.size(), d.meusCampos.size()),
                primeiraPagina + 1));

        Map<Long, Integer> cores = new HashMap<>();
        Map<Long, String> rotulos = new HashMap<>();
        cores.put(usuarioId, CoresSignatario.corNaPagina(0));
        rotulos.put(usuarioId, getString(R.string.rotulo_assine_aqui));
        visualizador.vista().setSignatarios(cores, rotulos);
        visualizador.vista().setCampos(d.meusCampos);
        visualizador.abrir(d.documento.caminhoPdf, primeiraPagina, erro -> info.setText(R.string.erro_abrir_pdf));

        if (d.eu.assinadoEm != null) {
            botaoAssinar.setEnabled(false);
            botaoLimpar.setEnabled(false);
            botaoAssinar.setText(getString(R.string.assinado_em, Formatos.dataHora(d.eu.assinadoEm)));
        }
    }

    private void confirmar() {
        if (quadro.estaVazia()) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirmar_assinatura_titulo)
                .setMessage(R.string.confirmar_assinatura_msg)
                .setPositiveButton(R.string.botao_assinar_salvar, (dlg, qual) -> salvar())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void salvar() {
        Bitmap imagem = quadro.gerarImagem();
        botaoAssinar.setEnabled(false);
        botaoLimpar.setEnabled(false);
        progresso.setVisibility(View.VISIBLE);
        File destino = new File(new File(getFilesDir(), "assinaturas"),
                "doc_" + documentoId + "_usuario_" + usuarioId + ".png");
        long agora = System.currentTimeMillis();

        Tarefas.executar(this, () -> {
            File pasta = destino.getParentFile();
            if (pasta != null && !pasta.isDirectory() && !pasta.mkdirs()) {
                throw new IOException(getString(R.string.erro_pasta));
            }
            try (OutputStream saida = new FileOutputStream(destino)) {
                imagem.compress(Bitmap.CompressFormat.PNG, 100, saida);
            }
            // null = já tinha assinado antes (não duplica)
            return documentos.registrarAssinatura(documentoId, usuarioId, destino.getAbsolutePath(), agora);
        }, novo -> {
            progresso.setVisibility(View.GONE);
            int mensagem;
            if (novo == null) {
                mensagem = R.string.assinatura_ja_feita;
            } else if (novo == StatusDocumento.CONCLUIDO) {
                mensagem = R.string.assinatura_ok_concluido;
            } else {
                mensagem = R.string.assinatura_ok_parcial;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.assinatura_ok_titulo)
                    .setMessage(getString(mensagem, Formatos.dataHora(agora)))
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, (dlg, qual) -> finish())
                    .show();
        }, erro -> {
            progresso.setVisibility(View.GONE);
            botaoAssinar.setEnabled(true);
            botaoLimpar.setEnabled(true);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.assinatura_falha_titulo)
                    .setMessage(getString(R.string.assinatura_falha_msg, Tarefas.mensagem(erro)))
                    .setPositiveButton(R.string.tentar_novamente, (dlg, qual) -> salvar())
                    .setNegativeButton(R.string.cancelar, null)
                    .show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visualizador != null) {
            visualizador.fechar();
        }
    }
}
