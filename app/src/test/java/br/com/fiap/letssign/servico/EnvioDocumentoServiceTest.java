package br.com.fiap.letssign.servico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.DocumentoResumo;
import br.com.fiap.letssign.data.DocumentoSignatario;
import br.com.fiap.letssign.data.SignatarioDoDocumento;
import br.com.fiap.letssign.dominio.PapelSignatario;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.repository.CampoRepository;
import br.com.fiap.letssign.repository.DocumentoRepository;

/** Testa o envio com repositórios falsos em memória (sem Android, sem banco). */
public class EnvioDocumentoServiceTest {

    private static final long ADMIN = 1;
    private static final long DOC = 10;
    private static final long ANA = 2;

    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    private DocumentosFalsos documentos;
    private CamposFalsos campos;
    private EnvioDocumentoService envio;

    @Before
    public void preparar() throws Exception {
        File pdf = pasta.newFile("contrato.pdf");
        documentos = new DocumentosFalsos();
        documentos.doc = new Documento();
        documentos.doc.id = DOC;
        documentos.doc.titulo = "Contrato";
        documentos.doc.caminhoPdf = pdf.getAbsolutePath();
        documentos.doc.quantidadePaginas = 2;
        documentos.doc.criadoPor = ADMIN;

        SignatarioDoDocumento ana = new SignatarioDoDocumento();
        ana.usuarioId = ANA;
        ana.nomeCompleto = "Ana Souza";
        ana.email = "ana@empresa.com";
        ana.papel = PapelSignatario.CONTRATANTE;
        documentos.signatarios.add(ana);

        campos = new CamposFalsos();
        campos.lista.add(campo(ANA, 0, 0.1, 0.1));

        envio = new EnvioDocumentoService(documentos, campos);
    }

    @Test
    public void enviaQuandoEstaTudoCerto() {
        EnvioDocumentoService.Resultado r = envio.enviar(DOC, ADMIN, 1000L);
        assertEquals(EnvioDocumentoService.Situacao.ENVIADO, r.situacao);
        assertEquals(StatusDocumento.ENVIADO, documentos.doc.status);
        assertEquals(Long.valueOf(1000L), documentos.doc.enviadoEm);
        assertEquals(1, documentos.eventosEnviado);
    }

    @Test
    public void segundaTentativaNaoDuplica() {
        envio.enviar(DOC, ADMIN, 1000L);
        EnvioDocumentoService.Resultado r = envio.enviar(DOC, ADMIN, 2000L);
        assertEquals(EnvioDocumentoService.Situacao.JA_ENVIADO, r.situacao);
        assertEquals(Long.valueOf(1000L), documentos.doc.enviadoEm);
        assertEquals(1, documentos.eventosEnviado);
    }

    @Test
    public void semSignatarioNaoEnvia() {
        documentos.signatarios.clear();
        campos.lista.clear();
        EnvioDocumentoService.Resultado r = envio.enviar(DOC, ADMIN, 1000L);
        assertEquals(EnvioDocumentoService.Situacao.INVALIDO, r.situacao);
        assertEquals(StatusDocumento.RASCUNHO, documentos.doc.status);
        assertEquals(0, documentos.eventosEnviado);
    }

    @Test
    public void signatarioSemCampoNaoEnvia() {
        campos.lista.clear();
        EnvioDocumentoService.Resultado r = envio.enviar(DOC, ADMIN, 1000L);
        assertEquals(EnvioDocumentoService.Situacao.INVALIDO, r.situacao);
        assertTrue(r.erros.get(0).contains("Ana Souza"));
    }

    @Test
    public void campoForaDaPaginaNaoEnvia() {
        campos.lista.clear();
        campos.lista.add(campo(ANA, 0, 0.8, 0.1)); // 0.8 + 0.3 passa de 1
        EnvioDocumentoService.Resultado r = envio.enviar(DOC, ADMIN, 1000L);
        assertEquals(EnvioDocumentoService.Situacao.INVALIDO, r.situacao);
        assertTrue(r.erros.get(0).contains("fora dos limites"));
    }

    @Test
    public void pdfQueSumiuNaoEnvia() {
        documentos.doc.caminhoPdf = new File(pasta.getRoot(), "nao-existe.pdf").getAbsolutePath();
        EnvioDocumentoService.Resultado r = envio.enviar(DOC, ADMIN, 1000L);
        assertEquals(EnvioDocumentoService.Situacao.INVALIDO, r.situacao);
    }

    @Test
    public void outroAdminNaoEncontraODocumento() {
        EnvioDocumentoService.Resultado r = envio.enviar(DOC, 99, 1000L);
        assertEquals(EnvioDocumentoService.Situacao.INVALIDO, r.situacao);
        assertEquals(StatusDocumento.RASCUNHO, documentos.doc.status);
    }

    private static CampoAssinatura campo(long signatario, int pagina, double x, double y) {
        CampoAssinatura c = new CampoAssinatura();
        c.documentoId = DOC;
        c.signatarioId = signatario;
        c.pagina = pagina;
        c.x = x;
        c.y = y;
        c.largura = 0.3;
        c.altura = 0.07;
        return c;
    }

    // ---------- Falsos em memória ----------

    private static class DocumentosFalsos implements DocumentoRepository {
        Documento doc;
        final List<SignatarioDoDocumento> signatarios = new ArrayList<>();
        int eventosEnviado;

        @Override
        public Documento buscarDoAdmin(long documentoId, long adminId) {
            return doc.id == documentoId && doc.criadoPor == adminId ? doc : null;
        }

        @Override
        public List<SignatarioDoDocumento> listarSignatarios(long documentoId) {
            return signatarios;
        }

        @Override
        public boolean marcarEnviado(long documentoId, StatusDocumento novoStatus, long agora, long adminId) {
            if (doc.status != StatusDocumento.RASCUNHO) {
                return false;
            }
            doc.status = novoStatus;
            doc.enviadoEm = agora;
            eventosEnviado++;
            return true;
        }

        @Override
        public long salvarRascunho(Documento documento, List<DocumentoSignatario> lista, long adminId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DocumentoResumo> listarDoAdmin(long adminId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DocumentoResumo> listarRecebidos(long signatarioId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Documento buscarRecebido(long documentoId, long signatarioId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StatusDocumento registrarAssinatura(long documentoId, long signatarioId, String caminho, long agora) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long criarNovaVersao(long documentoId, long adminId, String caminhoPdfDaCopia, long agora) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<br.com.fiap.letssign.data.AssinaturaDoSignatario> listarAssinaturas(long documentoId, long adminId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existeDocumentoComTitulo(long adminId, String titulo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long criarDocumentoEnviado(Documento documento, List<DocumentoSignatario> lista,
                                          List<CampoAssinatura> listaCampos, long adminId, long agora) {
            throw new UnsupportedOperationException();
        }
    }

    private static class CamposFalsos implements CampoRepository {
        final List<CampoAssinatura> lista = new ArrayList<>();

        @Override
        public List<CampoAssinatura> listar(long documentoId) {
            return lista;
        }

        @Override
        public void substituirTodos(long documentoId, List<CampoAssinatura> campos) {
            throw new UnsupportedOperationException();
        }
    }
}
