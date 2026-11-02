package br.com.fiap.letssign.ui;

import android.content.Context;
import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.DocumentoResumo;
import br.com.fiap.letssign.dominio.StatusDocumento;
import br.com.fiap.letssign.util.Formatos;

/** Cartões de documento: título, chip de status, datas e barra de assinaturas. */
public class DocumentoAdapter extends RecyclerView.Adapter<DocumentoAdapter.Linha> {

    public interface AoTocar {
        void tocou(DocumentoResumo item);
    }

    private final boolean modoSignatario;
    private final AoTocar aoTocar;
    private final List<DocumentoResumo> itens = new ArrayList<>();

    public DocumentoAdapter(boolean modoSignatario, AoTocar aoTocar) {
        this.modoSignatario = modoSignatario;
        this.aoTocar = aoTocar;
    }

    @SuppressLint("NotifyDataSetChanged") // lista pequena, recarregada inteira
    public void mostrar(List<DocumentoResumo> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Linha onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        View view = LayoutInflater.from(pai.getContext()).inflate(R.layout.item_documento, pai, false);
        return new Linha(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Linha linha, int posicao) {
        DocumentoResumo d = itens.get(posicao);
        Context c = linha.itemView.getContext();
        linha.titulo.setText(d.titulo);
        Formatos.pintarChipStatus(linha.status, d.status);

        // Linha 1: arquivo (e a versão, se for 2 ou mais), como no protótipo.
        String arquivo = d.nomeArquivo != null ? d.nomeArquivo : c.getString(R.string.sem_pdf);
        String linha1 = d.versao > 1 ? c.getString(R.string.arquivo_e_versao, arquivo, d.versao) : arquivo;
        if (modoSignatario) {
            linha1 = d.assinadoEm != null
                    ? c.getString(R.string.papel_assinado_em, Formatos.papel(d.papel), Formatos.dataHora(d.assinadoEm))
                    : c.getString(R.string.papel_toque_para_assinar, Formatos.papel(d.papel));
        }
        linha.datas.setText(linha1);

        boolean enviado = d.status != StatusDocumento.RASCUNHO;
        linha.progresso.setVisibility(enviado && !modoSignatario ? View.VISIBLE : View.GONE);
        linha.progresso.setProgress(Formatos.progresso(d.assinaturas, d.totalSignatarios));
        String linha2;
        if (modoSignatario) {
            linha2 = c.getString(R.string.recebido_em, Formatos.dataHora(d.enviadoEm));
        } else if (enviado) {
            linha2 = c.getString(R.string.assinaturas_x_de_y, d.assinaturas, d.totalSignatarios);
        } else {
            linha2 = c.getString(R.string.ainda_nao_enviado);
        }
        linha.assinaturas.setText(linha2);

        linha.itemView.setOnClickListener(v -> aoTocar.tocou(d));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class Linha extends RecyclerView.ViewHolder {
        final TextView titulo;
        final Chip status;
        final TextView datas;
        final LinearProgressIndicator progresso;
        final TextView assinaturas;

        Linha(View view) {
            super(view);
            titulo = view.findViewById(R.id.titulo);
            status = view.findViewById(R.id.chip_status);
            datas = view.findViewById(R.id.texto_datas);
            progresso = view.findViewById(R.id.progresso_assinaturas);
            assinaturas = view.findViewById(R.id.texto_assinaturas);
        }
    }
}
