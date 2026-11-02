package br.com.fiap.letssign.repository.room;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.fiap.letssign.data.AppDatabase;
import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.DocumentoResumo;
import br.com.fiap.letssign.data.DocumentoSignatario;
import br.com.fiap.letssign.data.EventoDetalhado;
import br.com.fiap.letssign.data.TipoEvento;
import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.dominio.PapelSignatario;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.data.SignatarioDoDocumento;
import br.com.fiap.letssign.dominio.Regras;
import br.com.fiap.letssign.repository.LimiteDeSignatariosException;
import br.com.fiap.letssign.repository.SignatariosDoGrupo;
import br.com.fiap.letssign.repository.UsuarioRepository;
import br.com.fiap.letssign.servico.AcordoDoGrupo;

/** Testa as implementações Room com um banco em memória (Robolectric, sem celular). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = android.app.Application.class)
public class RepositoriosRoomTest {

    private AppDatabase banco;
    private UsuarioRepositoryRoom usuarios;
    private DocumentoRepositoryRoom documentos;
    private CampoRepositoryRoom campos;
    private EventoRepositoryRoom eventos;
    private long adminId;
    private long anaId;
    private long beto;

    @Before
    public void preparar() {
        Context context = ApplicationProvider.getApplicationContext();
        banco = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
        usuarios = new UsuarioRepositoryRoom(banco);
        documentos = new DocumentoRepositoryRoom(banco);
        campos = new CampoRepositoryRoom(banco);
        eventos = new EventoRepositoryRoom(banco);

        usuarios.garantirAdminPadrao();
        adminId = usuarios.autenticar(UsuarioRepository.EMAIL_ADMIN_PADRAO, UsuarioRepository.SENHA_ADMIN_PADRAO).id;
        anaId = usuarios.salvarSignatario(signatario("Ana Souza", "ana@empresa.com"), "senha123");
        beto = usuarios.salvarSignatario(signatario("Beto Lima", "beto@empresa.com"), "senha456");
    }

    @After
    public void fechar() {
        banco.close();
    }

    @Test
    public void adminPadraoCriadoUmaVezSoComSenhaEmHash() {
        usuarios.garantirAdminPadrao();
        Usuario admin = usuarios.autenticar("ADMIN@letssign.com ", "admin123");
        assertNotNull(admin);
        assertEquals(Perfil.ADMIN, admin.perfil);
        assertFalse(admin.senhaHash.contains("admin123"));
        assertNull(usuarios.autenticar("admin@letssign.com", "errada"));
    }

    @Test
    public void signatariosDoGrupoEntramUmaVezSo() {
        Instalacao app = new Instalacao(); // instalação só com o Admin
        List<Usuario> grupo = SignatariosDoGrupo.lista();
        // Primeira abertura: cria os 4.
        assertEquals(4, app.usuarios.garantirSignatarios(grupo, "123456"));
        // Segunda e terceira aberturas: não duplica nada.
        assertEquals(0, app.usuarios.garantirSignatarios(grupo, "123456"));
        assertEquals(0, app.usuarios.garantirSignatarios(grupo, "123456"));
        assertEquals(4, app.usuarios.contarSignatarios());

        Usuario yuri = app.usuarios.autenticar("yuri.fiap@exemplo.com", "123456");
        assertNotNull(yuri);
        assertEquals("Yuri Luna", yuri.nomeCompleto);
        assertEquals(Perfil.SIGNATARIO, yuri.perfil);
        assertEquals("", yuri.telefone);
        assertFalse(yuri.senhaHash.contains("123456"));
        app.banco.close();
    }

    @Test
    public void quemJaExisteNaoEAlteradoPelaSemente() {
        Instalacao app = new Instalacao();
        // Instalação antiga em que a pessoa já foi cadastrada com outra senha.
        Usuario breno = signatario("Breno L. Silva", "breno.fiap@exemplo.com");
        app.usuarios.salvarSignatario(breno, "outraSenha");

        assertEquals(3, app.usuarios.garantirSignatarios(SignatariosDoGrupo.lista(), "123456"));
        assertNotNull(app.usuarios.autenticar("breno.fiap@exemplo.com", "outraSenha"));
        assertNull(app.usuarios.autenticar("breno.fiap@exemplo.com", "123456"));
        assertEquals("Breno L. Silva", app.usuarios.autenticar("breno.fiap@exemplo.com", "outraSenha").nomeCompleto);
        app.banco.close();
    }

    @Test
    public void limiteDeDezSignatarios() {
        assertEquals(10, UsuarioRepository.LIMITE_SIGNATARIOS);
        // Já existem Ana e Beto (2): cabem mais 8.
        long ultimo = 0;
        for (int i = 1; i <= 8; i++) {
            ultimo = usuarios.salvarSignatario(signatario("Pessoa Numero" + i, "pessoa" + i + "@empresa.com"), "senha123");
        }
        assertEquals(10, usuarios.contarSignatarios());

        // O 11º não entra.
        LimiteDeSignatariosException erro = assertThrows(LimiteDeSignatariosException.class,
                () -> usuarios.salvarSignatario(signatario("Fabio Melo", "fabio@empresa.com"), "senha123"));
        assertEquals("O limite de 10 signatários foi atingido. Edite um dos cadastrados.", erro.getMessage());
        assertEquals(10, usuarios.contarSignatarios());
        assertNull(usuarios.buscarPorEmail("fabio@empresa.com"));

        // Editar um que já existe continua permitido.
        Usuario e = usuarios.buscarPorId(ultimo);
        e.nomeCompleto = "Pessoa Editada";
        usuarios.salvarSignatario(e, "");
        assertEquals("Pessoa Editada", usuarios.buscarPorId(ultimo).nomeCompleto);
        assertEquals(10, usuarios.contarSignatarios());
    }

    @Test
    public void sementeDoGrupoRespeitaOLimite() {
        // Ana, Beto e mais 7: 9 vagas ocupadas. Dos 4 do grupo, só 1 cabe.
        for (int i = 1; i <= 7; i++) {
            usuarios.salvarSignatario(signatario("Pessoa Numero" + i, "pessoa" + i + "@empresa.com"), "senha123");
        }
        assertEquals(1, usuarios.garantirSignatarios(SignatariosDoGrupo.lista(), "123456"));
        assertEquals(10, usuarios.contarSignatarios());
        assertEquals(0, usuarios.garantirSignatarios(SignatariosDoGrupo.lista(), "123456"));
        assertEquals(10, usuarios.contarSignatarios());
    }

    @Test
    public void acordoDoGrupoCriadoUmaVezSoJaEnviado() {
        Instalacao app = new Instalacao();
        app.usuarios.garantirSignatarios(SignatariosDoGrupo.lista(), SignatariosDoGrupo.SENHA);

        long id = AcordoDoGrupo.criar(app.usuarios, app.documentos, app.adminId, "/acordo.pdf", 2, 1000L);
        assertTrue(id > 0);
        // Abrir o app de novo não duplica.
        assertEquals(-1, AcordoDoGrupo.criar(app.usuarios, app.documentos, app.adminId, "/acordo.pdf", 2, 2000L));
        assertEquals(-1, AcordoDoGrupo.criar(app.usuarios, app.documentos, app.adminId, "/acordo.pdf", 2, 3000L));
        assertEquals(1, app.documentos.listarDoAdmin(app.adminId).size());

        Documento d = app.documentos.buscarDoAdmin(id, app.adminId);
        assertEquals(AcordoDoGrupo.TITULO, d.titulo);
        assertEquals(StatusDocumento.ENVIADO, d.status);
        assertEquals(1, d.versao);
        assertEquals(2, d.quantidadePaginas);
        assertEquals(Long.valueOf(1000L), d.enviadoEm);

        List<SignatarioDoDocumento> sigs = app.documentos.listarSignatarios(id);
        assertEquals(4, sigs.size());
        for (SignatarioDoDocumento s : sigs) {
            assertEquals(PapelSignatario.CONTRATANTE, s.papel);
            assertTrue(s.obrigatorio);
            assertNull(s.assinadoEm);
        }
        List<CampoAssinatura> cs = app.campos.listar(id);
        assertEquals(4, cs.size());
        for (CampoAssinatura c : cs) {
            assertEquals("página 2", 1, c.pagina);
            assertTrue(Regras.campoDentroDaPagina(c.x, c.y, c.largura, c.altura));
        }
        EventoRepositoryRoom ev = new EventoRepositoryRoom(app.banco);
        assertEquals(2, ev.listar(id).size());
        assertEquals(TipoEvento.CRIADO, ev.listar(id).get(0).tipo);
        assertEquals(TipoEvento.ENVIADO, ev.listar(id).get(1).tipo);
        // Cada integrante vê o documento como pendente.
        long yuri = app.usuarios.buscarPorEmail("yuri.fiap@exemplo.com").id;
        assertEquals(1, app.documentos.listarRecebidos(yuri).size());
        app.banco.close();
    }

    @Test
    public void quatroAssinaturasLevamOAcordoAConcluido() {
        Instalacao app = new Instalacao();
        app.usuarios.garantirSignatarios(SignatariosDoGrupo.lista(), SignatariosDoGrupo.SENHA);
        long id = AcordoDoGrupo.criar(app.usuarios, app.documentos, app.adminId, "/acordo.pdf", 2, 1000L);

        String[] emails = {"vitor.fiap@exemplo.com", "breno.fiap@exemplo.com",
                "julia.fiap@exemplo.com", "yuri.fiap@exemplo.com"};
        StatusDocumento[] esperado = {StatusDocumento.PARCIAL, StatusDocumento.PARCIAL,
                StatusDocumento.PARCIAL, StatusDocumento.CONCLUIDO};
        for (int i = 0; i < emails.length; i++) {
            Usuario pessoa = app.usuarios.autenticar(emails[i], SignatariosDoGrupo.SENHA);
            assertNotNull(emails[i], pessoa);
            assertEquals(esperado[i], app.documentos.registrarAssinatura(id, pessoa.id, "/a" + i + ".png", 2000L + i));
        }
        Documento d = app.documentos.buscarDoAdmin(id, app.adminId);
        assertEquals(StatusDocumento.CONCLUIDO, d.status);
        assertEquals(Long.valueOf(2003L), d.concluidoEm);
        app.banco.close();
    }

    /** Um banco novo, só com o Admin (como uma instalação recém-feita). */
    private static class Instalacao {
        final AppDatabase banco;
        final UsuarioRepositoryRoom usuarios;
        final DocumentoRepositoryRoom documentos;
        final CampoRepositoryRoom campos;
        final long adminId;

        Instalacao() {
            banco = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase.class)
                    .allowMainThreadQueries().build();
            usuarios = new UsuarioRepositoryRoom(banco);
            documentos = new DocumentoRepositoryRoom(banco);
            campos = new CampoRepositoryRoom(banco);
            usuarios.garantirAdminPadrao();
            adminId = usuarios.buscarPorEmail(UsuarioRepository.EMAIL_ADMIN_PADRAO).id;
        }
    }

    @Test
    public void emailDeSignatarioNaoRepete() {
        assertTrue(usuarios.emailEmUso("ANA@empresa.com", 0));
        assertFalse(usuarios.emailEmUso("ana@empresa.com", anaId));
        assertEquals(2, usuarios.listarSignatarios().size());
    }

    @Test
    public void editarSemSenhaMantemASenhaAntiga() {
        Usuario ana = usuarios.buscarPorId(anaId);
        ana.telefone = "11999998888";
        usuarios.salvarSignatario(ana, "");
        assertNotNull(usuarios.autenticar("ana@empresa.com", "senha123"));
        assertEquals("11999998888", usuarios.buscarPorId(anaId).telefone);
    }

    @Test
    public void rascunhoNaoApareceParaOSignatario() {
        long doc = criarRascunho(anaId);
        assertEquals(1, documentos.listarDoAdmin(adminId).size());
        assertTrue(documentos.listarRecebidos(anaId).isEmpty());
        assertNull(documentos.buscarRecebido(doc, anaId));
        // Outro admin não vê o documento.
        assertNull(documentos.buscarDoAdmin(doc, 999));
    }

    @Test
    public void removerSignatarioApagaOsCamposDele() {
        long doc = criarRascunho(anaId, beto);
        campos.substituirTodos(doc, Arrays.asList(campo(anaId, 0), campo(beto, 0), campo(beto, 1)));
        assertEquals(3, campos.listar(doc).size());

        Documento d = documentos.buscarDoAdmin(doc, adminId);
        documentos.salvarRascunho(d, Collections.singletonList(
                new DocumentoSignatario(doc, anaId, PapelSignatario.CONTRATANTE)), adminId);
        List<CampoAssinatura> restantes = campos.listar(doc);
        assertEquals(1, restantes.size());
        assertEquals(anaId, restantes.get(0).signatarioId);
    }

    @Test
    public void enviarDuasVezesNaoDuplica() {
        long doc = criarRascunho(anaId);
        assertTrue(documentos.marcarEnviado(doc, StatusDocumento.ENVIADO, 1000L, adminId));
        assertFalse(documentos.marcarEnviado(doc, StatusDocumento.ENVIADO, 2000L, adminId));

        Documento d = documentos.buscarDoAdmin(doc, adminId);
        assertEquals(StatusDocumento.ENVIADO, d.status);
        assertEquals(Long.valueOf(1000L), d.enviadoEm);
        assertEquals(1, contarEventos(doc, TipoEvento.ENVIADO));
        assertEquals(1, contarEventos(doc, TipoEvento.CRIADO));

        List<DocumentoResumo> recebidos = documentos.listarRecebidos(anaId);
        assertEquals(1, recebidos.size());
        assertEquals(PapelSignatario.CONTRATANTE, recebidos.get(0).papel);
        assertTrue(documentos.listarRecebidos(beto).isEmpty());
    }

    @Test
    public void documentoEnviadoNaoPodeSerAlterado() {
        long doc = criarRascunho(anaId);
        documentos.marcarEnviado(doc, StatusDocumento.ENVIADO, 1000L, adminId);
        Documento d = documentos.buscarDoAdmin(doc, adminId);
        assertThrows(IllegalStateException.class, () -> documentos.salvarRascunho(d,
                Collections.singletonList(new DocumentoSignatario(doc, beto, PapelSignatario.TESTEMUNHA)), adminId));
        assertThrows(IllegalStateException.class,
                () -> campos.substituirTodos(doc, Collections.singletonList(campo(anaId, 0))));
    }

    @Test
    public void assinaturasLevamAParcialEDepoisConcluido() {
        long doc = criarRascunho(anaId, beto);
        documentos.marcarEnviado(doc, StatusDocumento.ENVIADO, 1000L, adminId);

        assertEquals(StatusDocumento.PARCIAL, documentos.registrarAssinatura(doc, anaId, "/a.png", 2000L));
        assertNull("assinar de novo não duplica", documentos.registrarAssinatura(doc, anaId, "/a.png", 2500L));
        assertEquals(StatusDocumento.CONCLUIDO, documentos.registrarAssinatura(doc, beto, "/b.png", 3000L));

        Documento d = documentos.buscarDoAdmin(doc, adminId);
        assertEquals(StatusDocumento.CONCLUIDO, d.status);
        assertEquals(Long.valueOf(3000L), d.concluidoEm);
        assertEquals(2, contarEventos(doc, TipoEvento.ASSINADO));
        assertEquals(1, contarEventos(doc, TipoEvento.CONCLUIDO));
        DocumentoResumo resumo = documentos.listarDoAdmin(adminId).get(0);
        assertEquals(2, resumo.assinaturas);
        assertEquals(2, resumo.totalSignatarios);
    }

    @Test
    public void imagemDaAssinaturaSoParaOAdminQueCriou() {
        long doc = criarRascunho(anaId, beto);
        documentos.marcarEnviado(doc, StatusDocumento.ENVIADO, 1000L, adminId);
        documentos.registrarAssinatura(doc, anaId, "/ana.png", 2000L);

        // O Admin que criou vê quem assinou, quando e o caminho da imagem.
        List<br.com.fiap.letssign.data.AssinaturaDoSignatario> doAdmin = documentos.listarAssinaturas(doc, adminId);
        assertEquals(2, doAdmin.size());
        br.com.fiap.letssign.data.AssinaturaDoSignatario daAna = doAdmin.get(0).usuarioId == anaId ? doAdmin.get(0) : doAdmin.get(1);
        assertEquals("/ana.png", daAna.caminhoAssinatura);
        assertEquals(Long.valueOf(2000L), daAna.assinadoEm);

        // Ninguém mais recebe as assinaturas: nem outro admin, nem os próprios signatários.
        assertTrue(documentos.listarAssinaturas(doc, 999).isEmpty());
        assertTrue(documentos.listarAssinaturas(doc, beto).isEmpty());
        assertTrue(documentos.listarAssinaturas(doc, anaId).isEmpty());
    }

    @Test
    public void consultasDoSignatarioNaoTrazemImagemDeAssinatura() {
        // O que o signatário recebe (lista de documentos e signatários) não tem campo de imagem.
        for (java.lang.reflect.Field f : DocumentoResumo.class.getFields()) {
            assertFalse(f.getName(), f.getName().toLowerCase(java.util.Locale.ROOT).contains("caminho"));
        }
        for (java.lang.reflect.Field f : SignatarioDoDocumento.class.getFields()) {
            assertFalse(f.getName(), f.getName().toLowerCase(java.util.Locale.ROOT).contains("caminho"));
        }
    }

    @Test
    public void quemNaoEstaNoDocumentoNaoAssina() {
        long doc = criarRascunho(anaId);
        documentos.marcarEnviado(doc, StatusDocumento.ENVIADO, 1000L, adminId);
        assertThrows(IllegalStateException.class,
                () -> documentos.registrarAssinatura(doc, beto, "/b.png", 2000L));
    }

    @Test
    public void novaVersaoNasceRascunhoComOsMesmosDadosESemAssinaturas() {
        long original = criarRascunho(anaId, beto);
        campos.substituirTodos(original, Arrays.asList(campo(anaId, 0), campo(beto, 1)));
        documentos.marcarEnviado(original, StatusDocumento.ENVIADO, 1000L, adminId);
        documentos.registrarAssinatura(original, anaId, "/a.png", 2000L);

        long nova = documentos.criarNovaVersao(original, adminId, "/copia.pdf", 3000L);

        Documento v2 = documentos.buscarDoAdmin(nova, adminId);
        assertEquals(StatusDocumento.RASCUNHO, v2.status);
        assertEquals(2, v2.versao);
        assertEquals(Long.valueOf(original), v2.documentoOrigemId);
        assertEquals("/copia.pdf", v2.caminhoPdf);
        assertEquals("Contrato", v2.titulo);
        assertNull(v2.enviadoEm);

        List<SignatarioDoDocumento> sigs = documentos.listarSignatarios(nova);
        assertEquals(2, sigs.size());
        for (SignatarioDoDocumento s : sigs) {
            assertNull("assinatura zerada", s.assinadoEm);
            assertEquals(PapelSignatario.CONTRATANTE, s.papel);
        }
        assertEquals(2, campos.listar(nova).size());
        assertEquals(1, contarEventos(nova, TipoEvento.CRIADO));
        assertEquals(0, contarEventos(nova, TipoEvento.ENVIADO));
        // A nova versão não aparece para o signatário enquanto é rascunho.
        assertNull(documentos.buscarRecebido(nova, anaId));
    }

    @Test
    public void novaVersaoNaoMexeNoOriginal() {
        long original = criarRascunho(anaId, beto);
        campos.substituirTodos(original, Collections.singletonList(campo(anaId, 0)));
        documentos.marcarEnviado(original, StatusDocumento.ENVIADO, 1000L, adminId);
        documentos.registrarAssinatura(original, anaId, "/a.png", 2000L);

        long nova = documentos.criarNovaVersao(original, adminId, null, 3000L);
        // Mexer na nova versão também não afeta o original.
        campos.substituirTodos(nova, Arrays.asList(campo(anaId, 1), campo(beto, 1)));

        Documento v1 = documentos.buscarDoAdmin(original, adminId);
        assertEquals(StatusDocumento.PARCIAL, v1.status);
        assertEquals(1, v1.versao);
        assertNull(v1.documentoOrigemId);
        assertEquals(1, campos.listar(original).size());
        int assinados = 0;
        for (SignatarioDoDocumento s : documentos.listarSignatarios(original)) {
            if (s.assinadoEm != null) {
                assinados++;
            }
        }
        assertEquals(1, assinados);
        assertEquals(1, contarEventos(original, TipoEvento.ENVIADO));
        assertEquals(1, contarEventos(original, TipoEvento.ASSINADO));
        assertEquals("/tmp/contrato.pdf", documentos.buscarDoAdmin(nova, adminId).caminhoPdf);

        List<DocumentoResumo> lista = documentos.listarDoAdmin(adminId);
        assertEquals(2, lista.size());
        DocumentoResumo resumoV2 = lista.get(0).id == nova ? lista.get(0) : lista.get(1);
        assertEquals(2, resumoV2.versao);
        assertEquals("contrato.pdf", resumoV2.nomeArquivo);
        assertEquals(StatusDocumento.RASCUNHO, resumoV2.status);
    }

    @Test
    public void novaVersaoSoDeDocumentoEnviadoEUmaVezSo() {
        long rascunho = criarRascunho(anaId);
        assertThrows(IllegalStateException.class, () -> documentos.criarNovaVersao(rascunho, adminId, null, 1L));

        documentos.marcarEnviado(rascunho, StatusDocumento.ENVIADO, 1000L, adminId);
        assertThrows("outro admin não cria", IllegalStateException.class,
                () -> documentos.criarNovaVersao(rascunho, 999, null, 1L));
        documentos.criarNovaVersao(rascunho, adminId, null, 2000L);
        assertThrows("não duplica a versão 2", IllegalStateException.class,
                () -> documentos.criarNovaVersao(rascunho, adminId, null, 3000L));
        assertEquals(2, documentos.listarDoAdmin(adminId).size());
    }

    // ---------- ajudantes ----------

    private long criarRascunho(long... signatarios) {
        Documento d = new Documento();
        d.titulo = "Contrato";
        d.caminhoPdf = "/tmp/contrato.pdf";
        d.nomeArquivo = "contrato.pdf";
        d.quantidadePaginas = 2;
        DocumentoSignatario[] lista = new DocumentoSignatario[signatarios.length];
        for (int i = 0; i < signatarios.length; i++) {
            lista[i] = new DocumentoSignatario(0, signatarios[i], PapelSignatario.CONTRATANTE);
        }
        return documentos.salvarRascunho(d, Arrays.asList(lista), adminId);
    }

    private int contarEventos(long doc, TipoEvento tipo) {
        int total = 0;
        for (EventoDetalhado e : eventos.listar(doc)) {
            if (e.tipo == tipo) {
                total++;
            }
        }
        return total;
    }

    private static Usuario signatario(String nome, String email) {
        Usuario u = new Usuario();
        u.nomeCompleto = nome;
        u.email = email;
        u.telefone = "11912345678";
        return u;
    }

    private static CampoAssinatura campo(long signatario, int pagina) {
        CampoAssinatura c = new CampoAssinatura();
        c.signatarioId = signatario;
        c.pagina = pagina;
        c.x = 0.1;
        c.y = 0.1;
        c.largura = 0.3;
        c.altura = 0.07;
        return c;
    }
}
