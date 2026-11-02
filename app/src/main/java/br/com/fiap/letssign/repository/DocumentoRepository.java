package br.com.fiap.letssign.repository;

import java.util.List;

import br.com.fiap.letssign.data.AssinaturaDoSignatario;
import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.DocumentoResumo;
import br.com.fiap.letssign.data.DocumentoSignatario;
import br.com.fiap.letssign.data.SignatarioDoDocumento;
import br.com.fiap.letssign.dominio.StatusDocumento;

/** Acesso aos documentos. Toda leitura filtra pelo usuário logado. */
public interface DocumentoRepository {

    /**
     * Grava o documento como RASCUNHO junto com os signatários. Na primeira vez registra o
     * evento CRIADO. Campos de quem saiu da lista são apagados. Devolve o id do documento.
     * Lança IllegalStateException se o documento já foi enviado (enviado não muda).
     */
    long salvarRascunho(Documento documento, List<DocumentoSignatario> signatarios, long adminId);

    /** Null se o documento não existe ou não é deste Admin. */
    Documento buscarDoAdmin(long documentoId, long adminId);

    List<DocumentoResumo> listarDoAdmin(long adminId);

    /** Documentos enviados para este signatário. */
    List<DocumentoResumo> listarRecebidos(long signatarioId);

    List<SignatarioDoDocumento> listarSignatarios(long documentoId);

    /**
     * Marca como enviado e registra o evento ENVIADO numa operação só, mas apenas se o
     * documento ainda estiver em RASCUNHO. Devolve false quando ele já tinha sido enviado.
     */
    boolean marcarEnviado(long documentoId, StatusDocumento novoStatus, long agora, long adminId);

    /** Null se o documento não foi enviado para este signatário. */
    Documento buscarRecebido(long documentoId, long signatarioId);

    /**
     * Registra a assinatura do signatário e recalcula o status (Regras.statusDoDocumento).
     * Devolve o novo status, ou null se ele já tinha assinado (não duplica).
     */
    StatusDocumento registrarAssinatura(long documentoId, long signatarioId, String caminhoAssinatura, long agora);

    /**
     * Cria uma NOVA VERSÃO de um documento já enviado: um RASCUNHO com versao + 1,
     * documentoOrigemId = original, os mesmos signatários, papéis e campos, e as
     * assinaturas zeradas. O original não muda. Devolve o id da nova versão.
     *
     * @param caminhoPdfDaCopia cópia do PDF para a nova versão (null = usa o mesmo caminho)
     * @throws IllegalStateException se o original ainda é rascunho ou se já existe uma nova versão dele
     */
    long criarNovaVersao(long documentoId, long adminId, String caminhoPdfDaCopia, long agora);

    boolean existeDocumentoComTitulo(long adminId, String titulo);

    /**
     * Quem assinou, quando e o caminho da imagem da assinatura. Só para o Admin que criou o
     * documento: para qualquer outro usuário a lista vem vazia (dados de um não vão para outro).
     */
    List<AssinaturaDoSignatario> listarAssinaturas(long documentoId, long adminId);

    /**
     * Cria um documento já ENVIADO (status pelo Regras.statusDoDocumento), com os signatários,
     * os campos e os eventos CRIADO e ENVIADO. Usado para o documento pré-carregado do grupo.
     * Se já existe um documento desse Admin com o mesmo título, não cria nada e devolve -1.
     *
     * @throws IllegalArgumentException se algum campo sai da página (Regras.campoDentroDaPagina)
     */
    long criarDocumentoEnviado(Documento documento, List<DocumentoSignatario> signatarios,
                               List<CampoAssinatura> campos, long adminId, long agora);
}
