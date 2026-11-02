package br.com.fiap.letssign.servico;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.repository.DocumentoRepository;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.repository.SignatariosDoGrupo;
import br.com.fiap.letssign.repository.UsuarioRepository;
import br.com.fiap.letssign.util.PdfPaginas;

/**
 * Dados da demonstração, garantidos toda vez que o app abre (sem duplicar):
 * Admin de teste, os 4 signatários do grupo e o documento "Acordo de trabalho do grupo".
 * Rodar fora da thread principal.
 */
public final class SementeDemonstracao {

    private static final String TAG = "LetsSign";

    private SementeDemonstracao() {
    }

    public static void garantir(Context context) {
        UsuarioRepository usuarios = Repositorios.usuarios(context);
        DocumentoRepository documentos = Repositorios.documentos(context);
        usuarios.garantirAdminPadrao();
        usuarios.garantirSignatarios(SignatariosDoGrupo.lista(), SignatariosDoGrupo.SENHA);

        Usuario admin = usuarios.buscarPorEmail(UsuarioRepository.EMAIL_ADMIN_PADRAO);
        if (admin == null || documentos.existeDocumentoComTitulo(admin.id, AcordoDoGrupo.TITULO)) {
            return; // já existe: não copia o PDF de novo
        }
        File pdf = null;
        try {
            pdf = copiarDoAssets(context);
            int paginas = PdfPaginas.contarPaginas(pdf);
            if (paginas <= AcordoDoGrupo.PAGINA_ASSINATURAS) {
                throw new IOException("O PDF do acordo não tem a página das assinaturas.");
            }
            long id = AcordoDoGrupo.criar(usuarios, documentos, admin.id, pdf.getAbsolutePath(), paginas,
                    System.currentTimeMillis());
            if (id < 0) {
                apagar(pdf);
            }
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Não foi possível criar o documento do grupo", e);
            if (pdf != null) {
                apagar(pdf);
            }
        }
    }

    private static File copiarDoAssets(Context context) throws IOException {
        File pasta = new File(context.getFilesDir(), "pdfs");
        if (!pasta.isDirectory() && !pasta.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta dos PDFs.");
        }
        File destino = new File(pasta, UUID.randomUUID() + ".pdf");
        try (InputStream entrada = context.getAssets().open(AcordoDoGrupo.ARQUIVO);
             OutputStream saida = new FileOutputStream(destino)) {
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = entrada.read(buffer)) != -1) {
                saida.write(buffer, 0, lidos);
            }
        }
        return destino;
    }

    private static void apagar(File arquivo) {
        if (arquivo.exists() && !arquivo.delete()) {
            arquivo.deleteOnExit();
        }
    }
}
