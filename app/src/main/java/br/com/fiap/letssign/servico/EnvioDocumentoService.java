package br.com.fiap.letssign.servico;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.SignatarioDoDocumento;
import br.com.fiap.letssign.dominio.Regras;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.repository.CampoRepository;
import br.com.fiap.letssign.repository.DocumentoRepository;

/**
 * Faz o envio do documento: valida com as Regras e grava o status.
 * Pode ser chamado de novo depois de uma falha: se já foi enviado, não envia outra vez.
 */
public class EnvioDocumentoService {

    public enum Situacao { ENVIADO, JA_ENVIADO, INVALIDO }

    public static class Resultado {
        public final Situacao situacao;
        public final List<String> erros;
        public final Long enviadoEm;
        public final int signatarios;

        private Resultado(Situacao situacao, List<String> erros, Long enviadoEm, int signatarios) {
            this.situacao = situacao;
            this.erros = erros;
            this.enviadoEm = enviadoEm;
            this.signatarios = signatarios;
        }

        static Resultado invalido(List<String> erros) {
            return new Resultado(Situacao.INVALIDO, erros, null, 0);
        }

        static Resultado jaEnviado(Long enviadoEm, int signatarios) {
            return new Resultado(Situacao.JA_ENVIADO, Collections.emptyList(), enviadoEm, signatarios);
        }

        static Resultado enviado(long enviadoEm, int signatarios) {
            return new Resultado(Situacao.ENVIADO, Collections.emptyList(), enviadoEm, signatarios);
        }
    }

    private final DocumentoRepository documentos;
    private final CampoRepository campos;

    public EnvioDocumentoService(DocumentoRepository documentos, CampoRepository campos) {
        this.documentos = documentos;
        this.campos = campos;
    }

    public Resultado enviar(long documentoId, long adminId, long agora) {
        Documento doc = documentos.buscarDoAdmin(documentoId, adminId);
        if (doc == null) {
            return Resultado.invalido(Collections.singletonList("Documento não encontrado."));
        }
        List<SignatarioDoDocumento> signatarios = documentos.listarSignatarios(documentoId);
        if (doc.status != StatusDocumento.RASCUNHO) {
            return Resultado.jaEnviado(doc.enviadoEm, signatarios.size());
        }

        List<String> erros = validar(doc, signatarios, campos.listar(documentoId));
        if (!erros.isEmpty()) {
            return Resultado.invalido(erros);
        }

        StatusDocumento novoStatus = Regras.statusDoDocumento(true, signatarios.size(), 0);
        boolean mudou = documentos.marcarEnviado(documentoId, novoStatus, agora, adminId);
        if (!mudou) {
            Documento atual = documentos.buscarDoAdmin(documentoId, adminId);
            return Resultado.jaEnviado(atual == null ? null : atual.enviadoEm, signatarios.size());
        }
        return Resultado.enviado(agora, signatarios.size());
    }

    /** Tudo o que impede o envio, numa lista de mensagens para mostrar na tela. */
    public static List<String> validar(Documento doc, List<SignatarioDoDocumento> signatarios,
                                       List<CampoAssinatura> listaCampos) {
        List<String> emails = new ArrayList<>();
        for (SignatarioDoDocumento s : signatarios) {
            emails.add(s.email);
        }
        boolean temPdf = doc.caminhoPdf != null && new File(doc.caminhoPdf).isFile();
        List<String> erros = new ArrayList<>(Regras.validarEnvio(doc.titulo, temPdf, emails));

        Set<Long> comCampo = new HashSet<>();
        Set<Long> participantes = new HashSet<>();
        for (SignatarioDoDocumento s : signatarios) {
            participantes.add(s.usuarioId);
        }
        for (CampoAssinatura c : listaCampos) {
            comCampo.add(c.signatarioId);
            if (!participantes.contains(c.signatarioId)) {
                erros.add("Há um campo sem signatário na página " + (c.pagina + 1) + ".");
            }
            if (!Regras.campoDentroDaPagina(c.x, c.y, c.largura, c.altura)) {
                erros.add("Há um campo fora dos limites da página " + (c.pagina + 1) + ".");
            }
            if (doc.quantidadePaginas > 0 && (c.pagina < 0 || c.pagina >= doc.quantidadePaginas)) {
                erros.add("Há um campo numa página que não existe no PDF.");
            }
        }
        for (SignatarioDoDocumento s : signatarios) {
            if (!comCampo.contains(s.usuarioId)) {
                erros.add(s.nomeCompleto + " ainda não tem campo de assinatura.");
            }
        }
        return erros;
    }
}
