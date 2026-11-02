package br.com.fiap.letssign.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.com.fiap.letssign.dominio.StatusDocumento;

@Dao
public interface DocumentoDao {

    @Insert
    long inserir(Documento documento);

    @Update
    int atualizar(Documento documento);

    /** Só devolve o documento se ele foi criado por este Admin. */
    @Query("SELECT * FROM documentos WHERE id = :id AND criadoPor = :adminId")
    Documento buscarDoAdmin(long id, long adminId);

    @Query("SELECT status FROM documentos WHERE id = :id")
    StatusDocumento buscarStatus(long id);

    @Query("SELECT d.id, d.titulo, d.nomeArquivo, d.versao, d.status, d.criadoEm, d.enviadoEm, d.concluidoEm, "
            + "(SELECT COUNT(*) FROM documento_signatarios s WHERE s.documentoId = d.id) AS totalSignatarios, "
            + "(SELECT COUNT(*) FROM documento_signatarios s WHERE s.documentoId = d.id AND s.assinadoEm IS NOT NULL) AS assinaturas, "
            + "NULL AS papel, NULL AS assinadoEm "
            + "FROM documentos d WHERE d.criadoPor = :adminId ORDER BY d.criadoEm DESC")
    List<DocumentoResumo> listarDoAdmin(long adminId);

    /** O signatário só vê documentos em que ele está e que já foram enviados. */
    @Query("SELECT d.id, d.titulo, d.nomeArquivo, d.versao, d.status, d.criadoEm, d.enviadoEm, d.concluidoEm, "
            + "(SELECT COUNT(*) FROM documento_signatarios t WHERE t.documentoId = d.id) AS totalSignatarios, "
            + "(SELECT COUNT(*) FROM documento_signatarios t WHERE t.documentoId = d.id AND t.assinadoEm IS NOT NULL) AS assinaturas, "
            + "s.papel AS papel, s.assinadoEm AS assinadoEm "
            + "FROM documentos d JOIN documento_signatarios s ON s.documentoId = d.id "
            + "WHERE s.usuarioId = :usuarioId AND d.status != 'RASCUNHO' ORDER BY d.enviadoEm DESC")
    List<DocumentoResumo> listarRecebidos(long usuarioId);

    @Query("SELECT u.id AS usuarioId, u.nomeCompleto, u.email, s.papel, s.obrigatorio, s.assinadoEm "
            + "FROM documento_signatarios s JOIN usuarios u ON u.id = s.usuarioId "
            + "WHERE s.documentoId = :documentoId ORDER BY u.nomeCompleto COLLATE NOCASE")
    List<SignatarioDoDocumento> listarSignatarios(long documentoId);

    @Insert
    void inserirSignatarios(List<DocumentoSignatario> signatarios);

    @Query("DELETE FROM documento_signatarios WHERE documentoId = :documentoId")
    void apagarSignatarios(long documentoId);

    /**
     * Troca o status só se ele ainda for o esperado. Devolve quantas linhas mudaram:
     * 0 quer dizer que outro envio já aconteceu (assim o envio não duplica).
     */
    @Query("UPDATE documentos SET status = :novoStatus, enviadoEm = :agora "
            + "WHERE id = :id AND status = :statusEsperado")
    int trocarStatusParaEnviado(long id, StatusDocumento statusEsperado, StatusDocumento novoStatus, long agora);

    /** Documento enviado para este signatário (null se ele não está no documento). */
    @Query("SELECT d.* FROM documentos d JOIN documento_signatarios s ON s.documentoId = d.id "
            + "WHERE d.id = :documentoId AND s.usuarioId = :usuarioId AND d.status != 'RASCUNHO'")
    Documento buscarRecebido(long documentoId, long usuarioId);

    /** Grava a assinatura só se ainda não existe (0 = já tinha assinado). */
    @Query("UPDATE documento_signatarios SET assinadoEm = :agora, caminhoAssinatura = :caminho "
            + "WHERE documentoId = :documentoId AND usuarioId = :usuarioId AND assinadoEm IS NULL")
    int registrarAssinatura(long documentoId, long usuarioId, String caminho, long agora);

    @Query("SELECT COUNT(*) FROM documento_signatarios WHERE documentoId = :documentoId AND obrigatorio = 1")
    int contarObrigatorios(long documentoId);

    @Query("SELECT COUNT(*) FROM documento_signatarios "
            + "WHERE documentoId = :documentoId AND obrigatorio = 1 AND assinadoEm IS NOT NULL")
    int contarObrigatoriosAssinados(long documentoId);

    @Query("UPDATE documentos SET status = :status, concluidoEm = :concluidoEm WHERE id = :id")
    void atualizarStatus(long id, StatusDocumento status, Long concluidoEm);

    /** Assinaturas (com a imagem) só para o Admin que criou o documento; para qualquer outro, lista vazia. */
    @Query("SELECT u.id AS usuarioId, u.nomeCompleto, s.papel, s.assinadoEm, s.caminhoAssinatura "
            + "FROM documento_signatarios s JOIN usuarios u ON u.id = s.usuarioId "
            + "JOIN documentos d ON d.id = s.documentoId "
            + "WHERE s.documentoId = :documentoId AND d.criadoPor = :adminId ORDER BY u.nomeCompleto COLLATE NOCASE")
    List<AssinaturaDoSignatario> listarAssinaturasDoAdmin(long documentoId, long adminId);

    @Query("SELECT COUNT(*) FROM documentos WHERE criadoPor = :adminId AND titulo = :titulo")
    int contarComTitulo(long adminId, String titulo);

    /** Nova versão já criada a partir deste documento (null se ainda não existe). */
    @Query("SELECT * FROM documentos WHERE documentoOrigemId = :origemId LIMIT 1")
    Documento buscarVersaoDerivada(long origemId);
}
