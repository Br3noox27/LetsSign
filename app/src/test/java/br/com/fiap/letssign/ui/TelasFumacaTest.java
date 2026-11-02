package br.com.fiap.letssign.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.DocumentoSignatario;
import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.dominio.PapelSignatario;
import br.com.fiap.letssign.dominio.Perfil;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.repository.UsuarioRepository;
import br.com.fiap.letssign.servico.EnvioDocumentoService;
import br.com.fiap.letssign.util.Sessao;

/**
 * Teste de fumaça: abre cada tela no Robolectric (sem celular) e confere que nada trava
 * ao inflar o layout e carregar os dados. Não substitui testar no aparelho:
 * o PDF não é desenhado aqui (o PdfRenderer depende do Android real).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TelasFumacaTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void telasAbremSemTravar() throws Exception {
        org.robolectric.shadows.ShadowLog.stream = System.out;
        esperar(); // LetsSignApp cria o Admin de teste em segundo plano
        long[] ids = new long[2];
        emSegundoPlano(() -> {
            UsuarioRepository usuarios = Repositorios.usuarios(context);
            ids[0] = usuarios.autenticar(UsuarioRepository.EMAIL_ADMIN_PADRAO, UsuarioRepository.SENHA_ADMIN_PADRAO).id;
            Usuario ana = new Usuario();
            ana.nomeCompleto = "Ana Souza";
            ana.email = "ana@empresa.com";
            ana.telefone = "11912345678";
            ids[1] = usuarios.salvarSignatario(ana, "senha123");
        });
        long adminId = ids[0];
        long anaId = ids[1];

        // Login sem sessão
        ActivityController<LoginActivity> login = Robolectric.buildActivity(LoginActivity.class).setup();
        esperar();
        assertFalse(login.get().isFinishing());

        // Admin: início, troca de aba, girar a tela
        entrar(adminId, "Administrador LetsSign", Perfil.ADMIN);
        ActivityController<AdminHomeActivity> home = Robolectric.buildActivity(AdminHomeActivity.class).setup();
        esperar();
        assertFalse(home.get().isFinishing());
        BottomNavigationView navegacao = home.get().findViewById(R.id.navegacao);
        navegacao.setSelectedItemId(R.id.aba_signatarios);
        esperar();
        navegacao.setSelectedItemId(R.id.aba_documentos);
        esperar();
        home.recreate();
        esperar();

        // Cadastro de signatário: validação inline
        ActivityController<SignatarioFormActivity> form = Robolectric.buildActivity(SignatarioFormActivity.class).setup();
        form.get().findViewById(R.id.botao_salvar).performClick();
        esperar();
        assertNotNull(((TextInputLayout) form.get().findViewById(R.id.campo_nome)).getError());

        // Limite: os 4 do grupo (semente) + Ana + 5 = 10. O 11º mostra o erro no formulário, sem fechar.
        emSegundoPlano(() -> {
            UsuarioRepository usuarios = Repositorios.usuarios(context);
            for (int i = 1; i <= 5; i++) {
                Usuario extra = new Usuario();
                extra.nomeCompleto = "Pessoa Numero" + i;
                extra.email = "pessoa" + i + "@empresa.com";
                extra.telefone = "11912345678";
                usuarios.salvarSignatario(extra, "senha123");
            }
        });
        ActivityController<SignatarioFormActivity> sexto = Robolectric.buildActivity(SignatarioFormActivity.class).setup();
        ((android.widget.EditText) sexto.get().findViewById(R.id.entrada_nome)).setText("Fabio Melo");
        ((android.widget.EditText) sexto.get().findViewById(R.id.entrada_email)).setText("fabio@empresa.com");
        ((android.widget.EditText) sexto.get().findViewById(R.id.entrada_telefone)).setText("11912345678");
        ((android.widget.EditText) sexto.get().findViewById(R.id.entrada_senha)).setText("senha123");
        sexto.get().findViewById(R.id.botao_salvar).performClick();
        esperar();
        android.widget.TextView erroLimite = sexto.get().findViewById(R.id.erro_formulario);
        assertEquals(android.view.View.VISIBLE, erroLimite.getVisibility());
        assertEquals("O limite de 10 signatários foi atingido. Edite um dos cadastrados.", erroLimite.getText().toString());
        assertFalse(sexto.get().isFinishing());

        // Com 10, a aba Signatários mostra "10 de 10" e o botão de cadastrar fica travado.
        ActivityController<AdminHomeActivity> cheio = Robolectric.buildActivity(AdminHomeActivity.class).setup();
        esperar();
        ((BottomNavigationView) cheio.get().findViewById(R.id.navegacao)).setSelectedItemId(R.id.aba_signatarios);
        esperar();
        assertEquals("10 de 10 signatários cadastrados",
                ((android.widget.TextView) cheio.get().findViewById(R.id.texto_limite)).getText().toString());
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton botao =
                cheio.get().findViewById(R.id.botao_principal);
        assertFalse(botao.isEnabled());
        assertEquals("Limite de signatários atingido", botao.getText().toString());

        // Novo documento: folha de escolha e validação inline
        ActivityController<NovoDocumentoActivity> novo = Robolectric.buildActivity(NovoDocumentoActivity.class).setup();
        esperar();
        novo.get().findViewById(R.id.botao_adicionar_signatario).performClick();
        esperar();
        novo.get().findViewById(R.id.botao_continuar).performClick();
        esperar();
        assertNotNull(((TextInputLayout) novo.get().findViewById(R.id.campo_titulo)).getError());
        novo.recreate();
        esperar();

        // Rascunho com PDF falso (não abre: testa o caminho de erro do visualizador)
        File pdfFalso = new File(context.getFilesDir(), "falso.pdf");
        try (FileOutputStream saida = new FileOutputStream(pdfFalso)) {
            saida.write("isto nao e um pdf".getBytes());
        }
        long[] doc = new long[1];
        emSegundoPlano(() -> {
            Documento d = new Documento();
            d.titulo = "Contrato de teste";
            d.caminhoPdf = pdfFalso.getAbsolutePath();
            d.nomeArquivo = "falso.pdf";
            d.quantidadePaginas = 1;
            doc[0] = Repositorios.documentos(context).salvarRascunho(d, Collections.singletonList(
                    new DocumentoSignatario(0, anaId, PapelSignatario.CONTRATADO)), adminId);
            CampoAssinatura c = new CampoAssinatura();
            c.signatarioId = anaId;
            c.x = 0.1;
            c.y = 0.1;
            c.largura = 0.3;
            c.altura = 0.07;
            Repositorios.campos(context).substituirTodos(doc[0], Collections.singletonList(c));
        });

        Intent comDoc = new Intent(context, NovoDocumentoActivity.class).putExtra(NovoDocumentoActivity.EXTRA_DOCUMENTO_ID, doc[0]);
        ActivityController<NovoDocumentoActivity> rascunho = Robolectric.buildActivity(NovoDocumentoActivity.class, comDoc).setup();
        esperar();
        assertFalse(rascunho.get().isFinishing());

        Intent posicionar = new Intent(context, PosicionarCamposActivity.class).putExtra(PosicionarCamposActivity.EXTRA_DOCUMENTO_ID, doc[0]);
        ActivityController<PosicionarCamposActivity> campos = Robolectric.buildActivity(PosicionarCamposActivity.class, posicionar).setup();
        esperar();
        campos.pause().stop().destroy();
        esperar();

        Intent revisar = new Intent(context, RevisaoEnvioActivity.class).putExtra(RevisaoEnvioActivity.EXTRA_DOCUMENTO_ID, doc[0]);
        ActivityController<RevisaoEnvioActivity> revisao = Robolectric.buildActivity(RevisaoEnvioActivity.class, revisar).setup();
        esperar();
        assertFalse(revisao.get().isFinishing());

        // Envio pelo serviço (o mesmo que o botão chama)
        EnvioDocumentoService.Resultado[] resultado = new EnvioDocumentoService.Resultado[1];
        emSegundoPlano(() -> resultado[0] = Repositorios.envio(context).enviar(doc[0], adminId, System.currentTimeMillis()));
        assertEquals(EnvioDocumentoService.Situacao.ENVIADO, resultado[0].situacao);

        Intent detalhe = new Intent(context, DocumentoDetalheActivity.class).putExtra(DocumentoDetalheActivity.EXTRA_DOCUMENTO_ID, doc[0]);
        ActivityController<DocumentoDetalheActivity> acompanhamento = Robolectric.buildActivity(DocumentoDetalheActivity.class, detalhe).setup();
        esperar();
        assertTrue(((android.widget.LinearLayout) acompanhamento.get().findViewById(R.id.linha_do_tempo)).getChildCount() == 4);

        // Signatário: início e tela de assinar
        entrar(anaId, "Ana Souza", Perfil.SIGNATARIO);
        ActivityController<SignatarioHomeActivity> inicioSig = Robolectric.buildActivity(SignatarioHomeActivity.class).setup();
        esperar();
        assertFalse(inicioSig.get().isFinishing());
        assertEquals(1, ((androidx.recyclerview.widget.RecyclerView) inicioSig.get().findViewById(R.id.lista)).getAdapter().getItemCount());

        Intent assinar = new Intent(context, AssinarActivity.class).putExtra(AssinarActivity.EXTRA_DOCUMENTO_ID, doc[0]);
        ActivityController<AssinarActivity> telaAssinar = Robolectric.buildActivity(AssinarActivity.class, assinar).setup();
        esperar();
        assertFalse(telaAssinar.get().isFinishing());

        // Signatário não entra em tela de Admin
        ActivityController<AdminHomeActivity> proibida = Robolectric.buildActivity(AdminHomeActivity.class).setup();
        assertTrue(proibida.get().isFinishing());

        // Nem no acompanhamento, onde ficam as imagens das assinaturas de todos.
        ActivityController<DocumentoDetalheActivity> semAcesso =
                Robolectric.buildActivity(DocumentoDetalheActivity.class, detalhe).setup();
        assertTrue(semAcesso.get().isFinishing());
    }

    private void entrar(long id, String nome, Perfil perfil) {
        Usuario u = new Usuario();
        u.id = id;
        u.nomeCompleto = nome;
        u.perfil = perfil;
        new Sessao(context).entrar(u);
    }

    /** Dá tempo para a fila de Tarefas terminar e entrega as respostas na thread da tela. */
    private static void esperar() throws InterruptedException {
        for (int i = 0; i < 15; i++) {
            Thread.sleep(40);
            shadowOf(Looper.getMainLooper()).idle();
        }
    }

    /** O Room não deixa consultar na thread principal; o teste usa outra thread. */
    private static void emSegundoPlano(Runnable trabalho) throws Exception {
        Throwable[] erro = new Throwable[1];
        Thread t = new Thread(() -> {
            try {
                trabalho.run();
            } catch (Throwable e) {
                erro[0] = e;
            }
        });
        t.start();
        t.join(10000);
        if (erro[0] != null) {
            throw new AssertionError(erro[0]);
        }
    }
}
