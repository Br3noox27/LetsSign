package br.com.fiap.letssign.util;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.dominio.PapelSignatario;
import br.com.fiap.letssign.dominio.StatusDocumento;

/** Textos e cores que aparecem na tela para os valores do domínio. */
public final class Formatos {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

    private Formatos() {
    }

    public static String dataHora(Long milissegundos) {
        if (milissegundos == null || milissegundos == 0) {
            return "-";
        }
        return new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", BRASIL).format(new Date(milissegundos));
    }

    /** "01/11 15:40", para listas curtas como o histórico. */
    public static String dataHoraCurta(long milissegundos) {
        return new SimpleDateFormat("dd/MM HH:mm", BRASIL).format(new Date(milissegundos));
    }

    public static String status(StatusDocumento status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case RASCUNHO:
                return "Rascunho";
            case ENVIADO:
                return "Enviado";
            case PARCIAL:
                return "Parcial";
            case CONCLUIDO:
                return "Concluído";
            default:
                return status.name();
        }
    }

    /** Cor do status (a única cor da tela, junto com os campos dos signatários). */
    public static int corStatus(Context context, StatusDocumento status) {
        int cor;
        if (status == StatusDocumento.ENVIADO) {
            cor = R.color.status_enviado;
        } else if (status == StatusDocumento.PARCIAL) {
            cor = R.color.status_parcial;
        } else if (status == StatusDocumento.CONCLUIDO) {
            cor = R.color.status_concluido;
        } else {
            cor = R.color.status_rascunho;
        }
        return ContextCompat.getColor(context, cor);
    }

    /** Chip de status como no protótipo: só contorno e texto na cor do status. */
    public static void pintarChipStatus(Chip chip, StatusDocumento status) {
        pintarChip(chip, status(status), corStatus(chip.getContext(), status));
    }

    public static void pintarChip(Chip chip, String texto, int cor) {
        chip.setText(texto);
        chip.setTextColor(cor);
        chip.setChipStrokeColor(ColorStateList.valueOf(cor));
        chip.setChipBackgroundColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
    }

    /** "Contrato" ou "Contrato · versão 2". */
    public static String tituloComVersao(Context context, String titulo, int versao) {
        return versao > 1 ? context.getString(R.string.titulo_com_versao, titulo, versao) : titulo;
    }

    /** Porcentagem de assinaturas (0 a 100) para a barra de progresso. */
    public static int progresso(int assinaturas, int total) {
        return total <= 0 ? 0 : Math.round(assinaturas * 100f / total);
    }

    public static String iniciais(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return "?";
        }
        String[] partes = nome.trim().split("\\s+");
        String primeira = partes[0].substring(0, 1);
        String ultima = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primeira + ultima).toUpperCase(BRASIL);
    }

    public static String primeiroNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return "";
        }
        return nome.trim().split("\\s+")[0];
    }

    public static String papel(PapelSignatario papel) {
        if (papel == null) {
            return "";
        }
        switch (papel) {
            case CONTRATANTE:
                return "Contratante";
            case CONTRATADO:
                return "Contratado";
            case TESTEMUNHA:
                return "Testemunha";
            default:
                return papel.name();
        }
    }

    public static String[] papeis() {
        PapelSignatario[] valores = PapelSignatario.values();
        String[] textos = new String[valores.length];
        for (int i = 0; i < valores.length; i++) {
            textos[i] = papel(valores[i]);
        }
        return textos;
    }
}
