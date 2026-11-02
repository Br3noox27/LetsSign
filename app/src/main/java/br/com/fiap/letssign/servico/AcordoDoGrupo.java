package br.com.fiap.letssign.servico;

import java.util.ArrayList;
import java.util.List;

import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.DocumentoSignatario;
import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.dominio.PapelSignatario;
import br.com.fiap.letssign.repository.DocumentoRepository;
import br.com.fiap.letssign.repository.UsuarioRepository;

/**
 * Documento pré-carregado para a demonstração: "Acordo de trabalho do grupo", já enviado
 * para os 4 integrantes, com um campo de assinatura por pessoa na página 2.
 * Esta classe não usa Android (dá para testar no JUnit); quem copia o PDF é a SementeDemonstracao.
 */
public final class AcordoDoGrupo {

    public static final String TITULO = "Acordo de trabalho do grupo";
    public static final String ARQUIVO = "acordo-grupo-letssign.pdf";

    /** Página 2 do PDF (o índice começa em 0). */
    static final int PAGINA_ASSINATURAS = 1;
    static final double LARGURA = 0.38;
    static final double ALTURA = 0.07;

    /** E-mail e posição (x, y) do campo de cada um, em proporção da página. */
    private static final Object[][] CAMPOS = {
            {"vitor.fiap@exemplo.com", 0.08, 0.60},
            {"breno.fiap@exemplo.com", 0.54, 0.60},
            {"julia.fiap@exemplo.com", 0.08, 0.76},
            {"yuri.fiap@exemplo.com", 0.54, 0.76},
    };

    private AcordoDoGrupo() {
    }

    /**
     * Cria o documento se ainda não existir. Devolve o id criado, ou -1 se ele já existia
     * ou se algum integrante não está cadastrado.
     */
    public static long criar(UsuarioRepository usuarios, DocumentoRepository documentos, long adminId,
                             String caminhoPdf, int paginas, long agora) {
        if (documentos.existeDocumentoComTitulo(adminId, TITULO)) {
            return -1;
        }
        List<DocumentoSignatario> signatarios = new ArrayList<>();
        List<CampoAssinatura> campos = new ArrayList<>();
        for (Object[] linha : CAMPOS) {
            Usuario pessoa = usuarios.buscarPorEmail((String) linha[0]);
            if (pessoa == null) {
                return -1;
            }
            DocumentoSignatario s = new DocumentoSignatario(0, pessoa.id, PapelSignatario.CONTRATANTE);
            s.obrigatorio = true;
            signatarios.add(s);

            CampoAssinatura c = new CampoAssinatura();
            c.signatarioId = pessoa.id;
            c.pagina = PAGINA_ASSINATURAS;
            c.x = (double) linha[1];
            c.y = (double) linha[2];
            c.largura = LARGURA;
            c.altura = ALTURA;
            campos.add(c);
        }

        Documento doc = new Documento();
        doc.titulo = TITULO;
        doc.nomeArquivo = ARQUIVO;
        doc.caminhoPdf = caminhoPdf;
        doc.quantidadePaginas = paginas;
        doc.versao = 1;
        return documentos.criarDocumentoEnviado(doc, signatarios, campos, adminId, agora);
    }
}
