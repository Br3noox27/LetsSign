package br.com.fiap.letssign.repository.room;

import java.util.ArrayList;
import java.util.List;

import br.com.fiap.letssign.data.AppDatabase;
import br.com.fiap.letssign.data.AssinaturaDoSignatario;
import br.com.fiap.letssign.data.CampoAssinatura;
import br.com.fiap.letssign.data.Documento;
import br.com.fiap.letssign.data.DocumentoDao;
import br.com.fiap.letssign.data.DocumentoResumo;
import br.com.fiap.letssign.data.DocumentoSignatario;
import br.com.fiap.letssign.data.EventoDocumento;
import br.com.fiap.letssign.data.SignatarioDoDocumento;
import br.com.fiap.letssign.data.TipoEvento;
import br.com.fiap.letssign.dominio.Regras;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.repository.DocumentoRepository;

public class DocumentoRepositoryRoom implements DocumentoRepository {

    private final AppDatabase banco;

    public DocumentoRepositoryRoom(AppDatabase banco) {
        this.banco = banco;
    }

    @Override
    public long salvarRascunho(Documento documento, List<DocumentoSignatario> signatarios, long adminId) {
        return banco.runInTransaction(() -> {
            long agora = System.currentTimeMillis();
            long id;
            if (documento.id == 0) {
                documento.criadoPor = adminId;
                documento.criadoEm = agora;
                documento.status = StatusDocumento.RASCUNHO;
                id = banco.documentoDao().inserir(documento);
                banco.eventoDao().inserir(new EventoDocumento(id, TipoEvento.CRIADO, agora, adminId));
            } else {
                Documento atual = banco.documentoDao().buscarDoAdmin(documento.id, adminId);
                if (atual == null) {
                    throw new IllegalStateException("Documento não encontrado.");
                }
                if (atual.status != StatusDocumento.RASCUNHO) {
                    throw new IllegalStateException("Documento já enviado não pode ser alterado.");
                }
                documento.criadoPor = atual.criadoPor;
                documento.criadoEm = atual.criadoEm;
                documento.versao = atual.versao;
                documento.documentoOrigemId = atual.documentoOrigemId;
                documento.status = StatusDocumento.RASCUNHO;
                banco.documentoDao().atualizar(documento);
                id = documento.id;
            }

            List<Long> mantidos = new ArrayList<>();
            for (DocumentoSignatario s : signatarios) {
                s.documentoId = id;
                mantidos.add(s.usuarioId);
            }
            banco.documentoDao().apagarSignatarios(id);
            banco.documentoDao().inserirSignatarios(signatarios);
            if (mantidos.isEmpty()) {
                banco.campoDao().apagarDoDocumento(id);
            } else {
                banco.campoDao().apagarDeQuemSaiu(id, mantidos);
            }
            return id;
        });
    }

    @Override
    public Documento buscarDoAdmin(long documentoId, long adminId) {
        return banco.documentoDao().buscarDoAdmin(documentoId, adminId);
    }

    @Override
    public List<DocumentoResumo> listarDoAdmin(long adminId) {
        return banco.documentoDao().listarDoAdmin(adminId);
    }

    @Override
    public List<DocumentoResumo> listarRecebidos(long signatarioId) {
        return banco.documentoDao().listarRecebidos(signatarioId);
    }

    @Override
    public List<SignatarioDoDocumento> listarSignatarios(long documentoId) {
        return banco.documentoDao().listarSignatarios(documentoId);
    }

    @Override
    public boolean marcarEnviado(long documentoId, StatusDocumento novoStatus, long agora, long adminId) {
        return banco.runInTransaction(() -> {
            int alterados = banco.documentoDao().trocarStatusParaEnviado(
                    documentoId, StatusDocumento.RASCUNHO, novoStatus, agora);
            if (alterados == 0) {
                return false;
            }
            banco.eventoDao().inserir(new EventoDocumento(documentoId, TipoEvento.ENVIADO, agora, adminId));
            return true;
        });
    }

    @Override
    public Documento buscarRecebido(long documentoId, long signatarioId) {
        return banco.documentoDao().buscarRecebido(documentoId, signatarioId);
    }

    @Override
    public StatusDocumento registrarAssinatura(long documentoId, long signatarioId,
                                               String caminhoAssinatura, long agora) {
        return banco.runInTransaction(() -> {
            DocumentoDao dao = banco.documentoDao();
            if (dao.buscarRecebido(documentoId, signatarioId) == null) {
                throw new IllegalStateException("Documento não encontrado para este signatário.");
            }
            if (dao.registrarAssinatura(documentoId, signatarioId, caminhoAssinatura, agora) == 0) {
                return null;
            }
            banco.eventoDao().inserir(new EventoDocumento(documentoId, TipoEvento.ASSINADO, agora, signatarioId));

            int obrigatorios = dao.contarObrigatorios(documentoId);
            int assinados = dao.contarObrigatoriosAssinados(documentoId);
            StatusDocumento novo = Regras.statusDoDocumento(true, obrigatorios, assinados);
            boolean concluiu = novo == StatusDocumento.CONCLUIDO;
            dao.atualizarStatus(documentoId, novo, concluiu ? agora : null);
            if (concluiu) {
                banco.eventoDao().inserir(new EventoDocumento(documentoId, TipoEvento.CONCLUIDO, agora, signatarioId));
            }
            return novo;
        });
    }

    @Override
    public long criarNovaVersao(long documentoId, long adminId, String caminhoPdfDaCopia, long agora) {
        return banco.runInTransaction(() -> {
            DocumentoDao dao = banco.documentoDao();
            Documento original = dao.buscarDoAdmin(documentoId, adminId);
            if (original == null) {
                throw new IllegalStateException("Documento não encontrado.");
            }
            if (original.status == StatusDocumento.RASCUNHO) {
                throw new IllegalStateException("Este documento ainda é rascunho: edite direto, sem nova versão.");
            }
            Documento existente = dao.buscarVersaoDerivada(documentoId);
            if (existente != null) {
                throw new IllegalStateException("Já existe a versão " + existente.versao + " deste documento.");
            }

            Documento nova = new Documento();
            nova.titulo = original.titulo;
            nova.nomeArquivo = original.nomeArquivo;
            nova.caminhoPdf = caminhoPdfDaCopia != null ? caminhoPdfDaCopia : original.caminhoPdf;
            nova.quantidadePaginas = original.quantidadePaginas;
            nova.status = StatusDocumento.RASCUNHO;
            nova.versao = original.versao + 1;
            nova.documentoOrigemId = original.id;
            nova.criadoPor = adminId;
            nova.criadoEm = agora;
            long novoId = dao.inserir(nova);

            // Mesmos signatários e papéis, sem nenhuma assinatura.
            List<DocumentoSignatario> signatarios = new ArrayList<>();
            for (SignatarioDoDocumento s : dao.listarSignatarios(documentoId)) {
                DocumentoSignatario copia = new DocumentoSignatario(novoId, s.usuarioId, s.papel);
                copia.obrigatorio = s.obrigatorio;
                signatarios.add(copia);
            }
            dao.inserirSignatarios(signatarios);

            // Mesmos campos, nas mesmas posições.
            List<CampoAssinatura> campos = new ArrayList<>();
            for (CampoAssinatura c : banco.campoDao().listar(documentoId)) {
                CampoAssinatura copia = c.copia();
                copia.id = 0;
                copia.documentoId = novoId;
                campos.add(copia);
            }
            banco.campoDao().inserir(campos);

            banco.eventoDao().inserir(new EventoDocumento(novoId, TipoEvento.CRIADO, agora, adminId));
            return novoId;
        });
    }

    @Override
    public List<AssinaturaDoSignatario> listarAssinaturas(long documentoId, long adminId) {
        return banco.documentoDao().listarAssinaturasDoAdmin(documentoId, adminId);
    }

    @Override
    public boolean existeDocumentoComTitulo(long adminId, String titulo) {
        return banco.documentoDao().contarComTitulo(adminId, titulo) > 0;
    }

    @Override
    public long criarDocumentoEnviado(Documento documento, List<DocumentoSignatario> signatarios,
                                      List<CampoAssinatura> campos, long adminId, long agora) {
        for (CampoAssinatura c : campos) {
            if (!Regras.campoDentroDaPagina(c.x, c.y, c.largura, c.altura)) {
                throw new IllegalArgumentException("Campo fora da página.");
            }
        }
        return banco.runInTransaction(() -> {
            DocumentoDao dao = banco.documentoDao();
            if (dao.contarComTitulo(adminId, documento.titulo) > 0) {
                return -1L;
            }
            documento.criadoPor = adminId;
            documento.criadoEm = agora;
            documento.enviadoEm = agora;
            documento.status = Regras.statusDoDocumento(true, signatarios.size(), 0);
            long id = dao.inserir(documento);
            for (DocumentoSignatario s : signatarios) {
                s.documentoId = id;
                s.assinadoEm = null;
            }
            dao.inserirSignatarios(signatarios);
            for (CampoAssinatura c : campos) {
                c.id = 0;
                c.documentoId = id;
            }
            banco.campoDao().inserir(campos);
            banco.eventoDao().inserir(new EventoDocumento(id, TipoEvento.CRIADO, agora, adminId));
            banco.eventoDao().inserir(new EventoDocumento(id, TipoEvento.ENVIADO, agora, adminId));
            return id;
        });
    }
}
