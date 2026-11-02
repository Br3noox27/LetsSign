package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.DocumentoSignatario;
import br.com.fiap.letssign.data.SignatarioDoDocumento;
import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.dominio.PapelSignatario;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.dominio.Regras;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.repository.DocumentoRepository;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.repository.UsuarioRepository;
import br.com.fiap.letssign.util.ArquivosPdf;
import br.com.fiap.letssign.util.CoresSignatario;
import br.com.fiap.letssign.util.Formatos;
import br.com.fiap.letssign.util.PdfPaginas;
import br.com.fiap.letssign.util.Tarefas;

/**
 * Passo 1 do fluxo: título, PDF e signatários com papel.
 * Ao continuar, grava um RASCUNHO e abre o posicionamento dos campos.
 */
public class NovoDocumentoActivity extends TelaBase {

    public static final String EXTRA_DOCUMENTO_ID = "documentoId";

    private static final String ESTADO_DOC = "doc";
    private static final String ESTADO_PDF = "pdf";
    private static final String ESTADO_NOME = "nome";
    private static final String ESTADO_PAGINAS = "paginas";
    private static final String ESTADO_IDS = "ids";
    private static final String ESTADO_PAPEIS = "papeis";

    /** Signatário escolhido para este documento, com o papel dele. */
    private static class Item {
        Usuario usuario;
        PapelSignatario papel;

        Item(Usuario usuario, PapelSignatario papel) {
            this.usuario = usuario;
            this.papel = papel;
        }
    }

    private static class PdfImportado {
        final String caminho;
        final String nome;
        final int paginas;

        PdfImportado(String caminho, String nome, int paginas) {
            this.caminho = caminho;
            this.nome = nome;
            this.paginas = paginas;
        }
    }

    private static class Dados {
        List<Usuario> cadastrados;
        Documento documento;
        List<SignatarioDoDocumento> signatarios;
    }

    private long documentoId;
    private long adminId;
    private String caminhoPdf;
    private String nomeArquivo;
    private int quantidadePaginas;
    /** PDF que já está gravado no banco (não apagar ao trocar antes de salvar). */
    private String caminhoGravado;
    private boolean carregado;

    private final List<Usuario> cadastrados = new ArrayList<>();
    private final List<Item> itens = new ArrayList<>();

    private UsuarioRepository usuarios;
    private DocumentoRepository documentos;

    private View raiz;
    private TextInputLayout campoTitulo;
    private TextInputEditText entradaTitulo;
    private TextView textoArquivo;
    private TextView textoPaginas;
    private TextView erroPdf;
    private TextView erroSignatarios;
    private View carregandoPdf;
    private MaterialButton botaoEscolherPdf;
    private LinearLayout listaSignatarios;
    private View textoSemSignatarios;
    private MaterialButton botaoContinuar;

    private final ActivityResultLauncher<String[]> escolherPdf =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::aoEscolherPdf);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirPerfil(Perfil.ADMIN)) {
            return;
        }
        setContentView(R.layout.activity_novo_documento);
        adminId = sessao.usuarioId();
        usuarios = Repositorios.usuarios(this);
        documentos = Repositorios.documentos(this);

        documentoId = savedInstanceState != null
                ? savedInstanceState.getLong(ESTADO_DOC)
                : getIntent().getLongExtra(EXTRA_DOCUMENTO_ID, 0);
        configurarToolbar(documentoId == 0 ? R.string.titulo_novo_documento : R.string.titulo_editar_rascunho, true);

        Passos.marcar(this, 1);
        raiz = findViewById(R.id.raiz);
        campoTitulo = findViewById(R.id.campo_titulo);
        entradaTitulo = findViewById(R.id.entrada_titulo);
        textoArquivo = findViewById(R.id.texto_arquivo);
        textoPaginas = findViewById(R.id.texto_paginas);
        erroPdf = findViewById(R.id.erro_pdf);
        erroSignatarios = findViewById(R.id.erro_signatarios);
        carregandoPdf = findViewById(R.id.carregando_pdf);
        botaoEscolherPdf = findViewById(R.id.botao_escolher_pdf);
        listaSignatarios = findViewById(R.id.lista_signatarios);
        textoSemSignatarios = findViewById(R.id.texto_sem_signatarios);
        botaoContinuar = findViewById(R.id.botao_continuar);

        botaoEscolherPdf.setOnClickListener(v -> escolherPdf.launch(new String[]{"application/pdf"}));
        findViewById(R.id.cartao_pdf).setOnClickListener(v -> escolherPdf.launch(new String[]{"application/pdf"}));
        findViewById(R.id.botao_adicionar_signatario).setOnClickListener(v -> adicionarSignatario());
        botaoContinuar.setOnClickListener(v -> continuar());

        carregar(savedInstanceState);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Volta do cadastro de signatário: atualiza a lista de quem pode ser escolhido.
        Tarefas.executar(this, usuarios::listarSignatarios, lista -> {
            cadastrados.clear();
            cadastrados.addAll(lista);
            for (Item item : itens) {
                Usuario atualizado = buscarCadastrado(item.usuario.id);
                if (atualizado != null) {
                    item.usuario = atualizado;
                }
            }
            atualizarTela();
        });
    }

    private void carregar(Bundle estado) {
        Tarefas.executar(this, () -> {
            Dados d = new Dados();
            d.cadastrados = usuarios.listarSignatarios();
            if (documentoId != 0) {
                d.documento = documentos.buscarDoAdmin(documentoId, adminId);
                d.signatarios = documentos.listarSignatarios(documentoId);
            }
            return d;
        }, d -> aplicar(d, estado));
    }

    private void aplicar(Dados d, Bundle estado) {
        cadastrados.clear();
        cadastrados.addAll(d.cadastrados);
        if (documentoId != 0) {
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
            caminhoGravado = d.documento.caminhoPdf;
            if (d.documento.versao > 1) {
                setTitle(getString(R.string.titulo_nova_versao, d.documento.versao));
            }
        }

        itens.clear();
        if (estado != null && estado.containsKey(ESTADO_IDS)) {
            caminhoPdf = estado.getString(ESTADO_PDF);
            nomeArquivo = estado.getString(ESTADO_NOME);
            quantidadePaginas = estado.getInt(ESTADO_PAGINAS);
            long[] ids = estado.getLongArray(ESTADO_IDS);
            String[] papeis = estado.getStringArray(ESTADO_PAPEIS);
            if (ids != null && papeis != null) {
                for (int i = 0; i < ids.length; i++) {
                    Usuario u = buscarCadastrado(ids[i]);
                    if (u != null) {
                        itens.add(new Item(u, PapelSignatario.valueOf(papeis[i])));
                    }
                }
            }
        } else if (d.documento != null) {
            entradaTitulo.setText(d.documento.titulo);
            caminhoPdf = d.documento.caminhoPdf;
            nomeArquivo = d.documento.nomeArquivo;
            quantidadePaginas = d.documento.quantidadePaginas;
            for (SignatarioDoDocumento s : d.signatarios) {
                Usuario u = buscarCadastrado(s.usuarioId);
                if (u != null) {
                    itens.add(new Item(u, s.papel));
                }
            }
        }
        carregado = true;
        atualizarTela();
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putLong(ESTADO_DOC, documentoId);
        if (!carregado) {
            return;
        }
        estado.putString(ESTADO_PDF, caminhoPdf);
        estado.putString(ESTADO_NOME, nomeArquivo);
        estado.putInt(ESTADO_PAGINAS, quantidadePaginas);
        long[] ids = new long[itens.size()];
        String[] papeis = new String[itens.size()];
        for (int i = 0; i < itens.size(); i++) {
            ids[i] = itens.get(i).usuario.id;
            papeis[i] = itens.get(i).papel.name();
        }
        estado.putLongArray(ESTADO_IDS, ids);
        estado.putStringArray(ESTADO_PAPEIS, papeis);
    }

    // ---------- PDF ----------

    private void aoEscolherPdf(Uri uri) {
        if (uri == null) {
            return;
        }
        carregandoPdf.setVisibility(View.VISIBLE);
        botaoEscolherPdf.setEnabled(false);
        erroPdf.setVisibility(View.GONE);
        Tarefas.executar(this, () -> importarPdf(uri), importado -> {
            carregandoPdf.setVisibility(View.GONE);
            botaoEscolherPdf.setEnabled(true);
            apagarCopiaNaoGravada();
            caminhoPdf = importado.caminho;
            nomeArquivo = importado.nome;
            quantidadePaginas = importado.paginas;
            atualizarTela();
        }, erro -> {
            carregandoPdf.setVisibility(View.GONE);
            botaoEscolherPdf.setEnabled(true);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialogo_arquivo_invalido)
                    .setMessage(erro.getMessage() != null ? erro.getMessage() : getString(R.string.erro_pdf_invalido))
                    .setPositiveButton(R.string.botao_escolher_outro,
                            (dlg, qual) -> escolherPdf.launch(new String[]{"application/pdf"}))
                    .setNegativeButton(R.string.cancelar, null)
                    .show();
        });
    }

    /** Copia o PDF escolhido para a pasta do app e confere se ele abre. */
    private PdfImportado importarPdf(Uri uri) throws IOException {
        String nome = nomeDoArquivo(uri);
        File pasta = new File(getFilesDir(), "pdfs");
        if (!pasta.isDirectory() && !pasta.mkdirs()) {
            throw new IOException(getString(R.string.erro_pasta));
        }
        File destino = new File(pasta, UUID.randomUUID() + ".pdf");
        try (InputStream entrada = getContentResolver().openInputStream(uri);
             OutputStream saida = new FileOutputStream(destino)) {
            if (entrada == null) {
                throw new IOException(getString(R.string.erro_ler_arquivo));
            }
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = entrada.read(buffer)) != -1) {
                saida.write(buffer, 0, lidos);
            }
        } catch (IOException | SecurityException e) {
            apagar(destino);
            throw new IOException(getString(R.string.erro_ler_arquivo), e);
        }
        if (destino.length() == 0) {
            apagar(destino);
            throw new IOException(getString(R.string.erro_pdf_vazio));
        }
        try {
            int paginas = PdfPaginas.contarPaginas(destino);
            if (paginas == 0) {
                throw new IOException("sem páginas");
            }
            return new PdfImportado(destino.getAbsolutePath(), nome, paginas);
        } catch (IOException | RuntimeException e) {
            apagar(destino);
            throw new IOException(getString(R.string.erro_pdf_invalido), e);
        }
    }

    private String nomeDoArquivo(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String nome = cursor.getString(0);
                if (nome != null && !nome.isEmpty()) {
                    return nome;
                }
            }
        } catch (RuntimeException ignorado) {
            // Sem nome: usa o padrão abaixo.
        }
        return "documento.pdf";
    }

    /** Apaga a cópia do PDF que está na tela se ela ainda não foi gravada no banco. */
    private void apagarCopiaNaoGravada() {
        String naTela = caminhoPdf;
        String gravado = caminhoGravado;
        Tarefas.emSegundoPlano(() -> ArquivosPdf.apagarCopiaNaoGravada(naTela, gravado));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Fechou sem salvar (voltar ou sair da tela): a cópia escolhida não pode ficar esquecida.
        // Girar o celular não entra aqui (isFinishing() é false e a cópia continua em uso).
        if (isFinishing() && carregado) {
            apagarCopiaNaoGravada();
        }
    }

    private static void apagar(File arquivo) {
        if (arquivo.exists() && !arquivo.delete()) {
            arquivo.deleteOnExit();
        }
    }

    // ---------- Signatários ----------

    private void adicionarSignatario() {
        if (cadastrados.isEmpty()) {
            Snackbar.make(raiz, R.string.dialogo_sem_cadastrados_msg, Snackbar.LENGTH_LONG)
                    .setAnchorView(botaoContinuar)
                    .setAction(R.string.botao_cadastrar,
                            v -> startActivity(new Intent(this, SignatarioFormActivity.class)))
                    .show();
            return;
        }
        List<Usuario> disponiveis = disponiveis();
        if (disponiveis.isEmpty()) {
            Snackbar.make(raiz, R.string.todos_ja_na_lista, Snackbar.LENGTH_SHORT)
                    .setAnchorView(botaoContinuar).show();
            return;
        }
        escolherPessoa(getString(R.string.dialogo_escolher_signatario), disponiveis, escolhido ->
                escolherPapel(escolhido.nomeCompleto, null, papel -> {
                    itens.add(new Item(escolhido, papel));
                    atualizarTela();
                }));
    }

    private void trocarSignatario(Item item) {
        List<Usuario> disponiveis = disponiveis();
        if (disponiveis.isEmpty()) {
            Snackbar.make(raiz, R.string.todos_ja_na_lista, Snackbar.LENGTH_SHORT)
                    .setAnchorView(botaoContinuar).show();
            return;
        }
        escolherPessoa(getString(R.string.dialogo_trocar_signatario, item.usuario.nomeCompleto), disponiveis,
                escolhido -> {
                    item.usuario = escolhido;
                    atualizarTela();
                });
    }

    private void removerSignatario(Item item) {
        int posicao = itens.indexOf(item);
        itens.remove(item);
        atualizarTela();
        Snackbar.make(raiz, getString(R.string.signatario_removido, Formatos.primeiroNome(item.usuario.nomeCompleto)),
                        Snackbar.LENGTH_LONG)
                .setAnchorView(botaoContinuar)
                .setAction(R.string.desfazer, v -> {
                    if (!itens.contains(item) && buscarItem(item.usuario.id) == null) {
                        itens.add(Math.min(posicao, itens.size()), item);
                        atualizarTela();
                    }
                })
                .show();
    }

    private interface AoEscolherPessoa {
        void escolheu(Usuario usuario);
    }

    private interface AoEscolherPapel {
        void escolheu(PapelSignatario papel);
    }

    private void escolherPessoa(String titulo, List<Usuario> opcoes, AoEscolherPessoa aoEscolher) {
        List<String> nomes = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        for (Usuario u : opcoes) {
            nomes.add(u.nomeCompleto);
            emails.add(u.email);
        }
        FolhaEscolha.mostrar(this, titulo, nomes, emails, -1, posicao -> aoEscolher.escolheu(opcoes.get(posicao)));
    }

    private void escolherPapel(String nome, PapelSignatario atual, AoEscolherPapel aoEscolher) {
        PapelSignatario[] papeis = PapelSignatario.values();
        List<String> descricoes = Arrays.asList(getResources().getStringArray(R.array.descricao_papeis));
        int marcada = atual == null ? -1 : atual.ordinal();
        FolhaEscolha.mostrar(this, getString(R.string.dialogo_escolher_papel, nome),
                Arrays.asList(Formatos.papeis()), descricoes, marcada, posicao -> aoEscolher.escolheu(papeis[posicao]));
    }

    private List<Usuario> disponiveis() {
        List<Usuario> lista = new ArrayList<>();
        for (Usuario u : cadastrados) {
            if (buscarItem(u.id) == null) {
                lista.add(u);
            }
        }
        return lista;
    }

    private Item buscarItem(long usuarioId) {
        for (Item item : itens) {
            if (item.usuario.id == usuarioId) {
                return item;
            }
        }
        return null;
    }

    private Usuario buscarCadastrado(long id) {
        for (Usuario u : cadastrados) {
            if (u.id == id) {
                return u;
            }
        }
        return null;
    }

    // ---------- Tela ----------

    private void atualizarTela() {
        if (caminhoPdf != null) {
            textoArquivo.setText(nomeArquivo);
            textoPaginas.setText(getResources().getQuantityString(R.plurals.paginas, quantidadePaginas, quantidadePaginas));
            botaoEscolherPdf.setText(R.string.botao_trocar_pdf);
            erroPdf.setVisibility(View.GONE);
        } else {
            textoArquivo.setText(R.string.nenhum_pdf);
            textoPaginas.setText(R.string.ajuda_pdf);
            botaoEscolherPdf.setText(R.string.botao_escolher_pdf);
        }

        listaSignatarios.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < itens.size(); i++) {
            Item item = itens.get(i);
            View linha = inflater.inflate(R.layout.item_signatario_documento, listaSignatarios, false);
            linha.findViewById(R.id.barra_cor).setBackgroundColor(CoresSignatario.cor(this, i));
            ((TextView) linha.findViewById(R.id.nome)).setText(item.usuario.nomeCompleto);
            ((TextView) linha.findViewById(R.id.email)).setText(item.usuario.email);
            MaterialButton papel = linha.findViewById(R.id.botao_papel);
            papel.setText(getString(R.string.papel_x, Formatos.papel(item.papel)));
            papel.setOnClickListener(v -> escolherPapel(item.usuario.nomeCompleto, item.papel, novo -> {
                item.papel = novo;
                atualizarTela();
            }));
            linha.findViewById(R.id.botao_trocar).setOnClickListener(v -> trocarSignatario(item));
            linha.findViewById(R.id.botao_remover).setOnClickListener(v -> removerSignatario(item));
            listaSignatarios.addView(linha);
        }
        textoSemSignatarios.setVisibility(itens.isEmpty() ? View.VISIBLE : View.GONE);
        if (!itens.isEmpty()) {
            erroSignatarios.setVisibility(View.GONE);
        }
    }

    private void continuar() {
        String titulo = LoginActivity.texto(entradaTitulo);
        List<String> emails = new ArrayList<>();
        for (Item item : itens) {
            emails.add(item.usuario.email);
        }

        // Mensagens específicas, perto do campo com problema.
        campoTitulo.setError(titulo.isEmpty() ? getString(R.string.erro_titulo) : null);
        erroPdf.setText(R.string.erro_sem_pdf);
        erroPdf.setVisibility(caminhoPdf == null ? View.VISIBLE : View.GONE);
        erroSignatarios.setText(R.string.erro_sem_signatario);
        erroSignatarios.setVisibility(itens.isEmpty() ? View.VISIBLE : View.GONE);

        List<String> erros = Regras.validarEnvio(titulo, caminhoPdf != null, emails);
        if (!erros.isEmpty()) {
            Snackbar.make(raiz, erros.get(0), Snackbar.LENGTH_LONG).setAnchorView(botaoContinuar).show();
            return;
        }

        Documento doc = new Documento();
        doc.id = documentoId;
        doc.titulo = titulo;
        doc.caminhoPdf = caminhoPdf;
        doc.nomeArquivo = nomeArquivo;
        doc.quantidadePaginas = quantidadePaginas;
        List<DocumentoSignatario> lista = new ArrayList<>();
        for (Item item : itens) {
            lista.add(new DocumentoSignatario(documentoId, item.usuario.id, item.papel));
        }

        String pdfAnterior = caminhoGravado;
        botaoContinuar.setEnabled(false);
        Tarefas.executar(this, () -> documentos.salvarRascunho(doc, lista, adminId), id -> {
            botaoContinuar.setEnabled(true);
            documentoId = id;
            caminhoGravado = caminhoPdf;
            if (pdfAnterior != null && !pdfAnterior.equals(caminhoPdf)) {
                Tarefas.emSegundoPlano(() -> apagar(new File(pdfAnterior)));
            }
            Intent intent = new Intent(this, PosicionarCamposActivity.class);
            intent.putExtra(PosicionarCamposActivity.EXTRA_DOCUMENTO_ID, id);
            startActivity(intent);
        }, erro -> {
            botaoContinuar.setEnabled(true);
            Snackbar.make(raiz, getString(R.string.erro_generico, Tarefas.mensagem(erro)), Snackbar.LENGTH_LONG)
                    .setAnchorView(botaoContinuar).show();
        });
    }
}
