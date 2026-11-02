package br.com.fiap.letssign.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.fiap.letssign.data.CampoAssinatura;

/**
 * Mostra uma página do PDF (já renderizada em Bitmap) e desenha os campos de assinatura
 * por cima. No modo editável, o toque seleciona um campo e o arrasto move o campo.
 * A conversão tela <-> proporção da página (0 a 1) acontece só aqui.
 */
public class PdfPageView extends View {

    public interface Ouvinte {
        /** Campo tocado (ou null quando tocou fora de qualquer campo). */
        void aoSelecionar(CampoAssinatura campo);

        /** O dedo saiu depois de arrastar o campo. */
        void aoMover(CampoAssinatura campo);
    }

    private Bitmap pagina;
    private int indicePagina;
    private List<CampoAssinatura> campos = Collections.emptyList();
    private CampoAssinatura selecionado;
    private boolean editavel;
    private Ouvinte ouvinte;
    private final Map<Long, Integer> cores = new HashMap<>();
    private final Map<Long, String> rotulos = new HashMap<>();
    /** Imagem da assinatura de quem já assinou (chave = id do usuário). Só desenhada na tela. */
    private final Map<Long, Bitmap> assinaturas = new HashMap<>();
    private final RectF areaAssinatura = new RectF();

    private final RectF destino = new RectF();
    private final RectF retangulo = new RectF();
    private final Paint pintaPagina = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint pintaSombra = new Paint();
    private final Paint pintaPreenchimento = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaBorda = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaFundoRotulo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF fundoRotulo = new RectF();

    private boolean arrastando;
    private float deslocX;
    private float deslocY;

    public PdfPageView(Context context) {
        this(context, null);
    }

    public PdfPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float densidade = getResources().getDisplayMetrics().density;
        pintaSombra.setColor(Color.argb(40, 0, 0, 0));
        pintaBorda.setStyle(Paint.Style.STROKE);
        pintaBorda.setStrokeWidth(2 * densidade);
        pintaTexto.setTextSize(12 * densidade);
        pintaTexto.setFakeBoldText(true);
        pintaFundoRotulo.setColor(Color.WHITE);
    }

    public void setPagina(Bitmap imagem, int indice) {
        pagina = imagem;
        indicePagina = indice;
        invalidate();
    }

    public void setCampos(List<CampoAssinatura> lista) {
        campos = lista == null ? Collections.emptyList() : lista;
        invalidate();
    }

    public void setSelecionado(CampoAssinatura campo) {
        selecionado = campo;
        invalidate();
    }

    public void setEditavel(boolean valor) {
        editavel = valor;
    }

    public void setOuvinte(Ouvinte valor) {
        ouvinte = valor;
    }

    /** Cor e texto curto de cada signatário (chave = id do usuário). */
    public void setSignatarios(Map<Long, Integer> coresPorId, Map<Long, String> rotulosPorId) {
        cores.clear();
        cores.putAll(coresPorId);
        rotulos.clear();
        rotulos.putAll(rotulosPorId);
        invalidate();
    }

    /** "Documento assinado": desenha a imagem de cada assinatura dentro do campo da pessoa. */
    public void setAssinaturas(Map<Long, Bitmap> imagensPorId) {
        assinaturas.clear();
        assinaturas.putAll(imagensPorId);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pagina == null) {
            return;
        }
        calcularDestino();
        canvas.drawRect(destino.left + 4, destino.top + 4, destino.right + 4, destino.bottom + 4, pintaSombra);
        canvas.drawBitmap(pagina, null, destino, pintaPagina);

        for (CampoAssinatura c : campos) {
            if (c.pagina != indicePagina) {
                continue;
            }
            int cor = corDe(c.signatarioId);
            paraTela(c, retangulo);
            pintaPreenchimento.setColor(cor);
            pintaPreenchimento.setAlpha(c == selecionado ? 90 : 50);
            canvas.drawRect(retangulo, pintaPreenchimento);
            pintaBorda.setColor(cor);
            pintaBorda.setStrokeWidth((c == selecionado ? 4 : 2) * getResources().getDisplayMetrics().density);
            canvas.drawRect(retangulo, pintaBorda);

            Bitmap assinatura = assinaturas.get(c.signatarioId);
            if (assinatura != null) {
                desenharAssinatura(canvas, assinatura, retangulo);
                continue; // assinado: mostra a assinatura no lugar do rótulo
            }
            String rotulo = rotulos.get(c.signatarioId);
            if (rotulo != null) {
                // Rótulo com fundo branco (como no protótipo), legível em qualquer página.
                float densidade = getResources().getDisplayMetrics().density;
                // Texto proporcional ao campo (cabe mesmo quando a página aparece pequena).
                pintaTexto.setTextSize(Math.max(6 * densidade, Math.min(11 * densidade, retangulo.height() * 0.45f)));
                float margem = Math.min(4 * densidade, retangulo.height() * 0.12f);
                float largura = pintaTexto.measureText(rotulo) + 2 * margem;
                float altura = pintaTexto.descent() - pintaTexto.ascent() + margem;
                canvas.save();
                canvas.clipRect(retangulo);
                fundoRotulo.set(retangulo.left + margem, retangulo.top + margem,
                        retangulo.left + margem + largura, retangulo.top + margem + altura);
                canvas.drawRoundRect(fundoRotulo, 3 * densidade, 3 * densidade, pintaFundoRotulo);
                pintaTexto.setColor(cor);
                canvas.drawText(rotulo, fundoRotulo.left + margem,
                        fundoRotulo.top + margem / 2 - pintaTexto.ascent(), pintaTexto);
                canvas.restore();
            }
        }
    }

    /** Encaixa a imagem no campo sem distorcer, sobre fundo branco. */
    private void desenharAssinatura(Canvas canvas, Bitmap imagem, RectF campo) {
        float borda = 2 * getResources().getDisplayMetrics().density;
        areaAssinatura.set(campo.left + borda, campo.top + borda, campo.right - borda, campo.bottom - borda);
        canvas.drawRect(areaAssinatura, pintaFundoRotulo);
        float escala = Math.min(areaAssinatura.width() / imagem.getWidth(), areaAssinatura.height() / imagem.getHeight());
        float largura = imagem.getWidth() * escala;
        float altura = imagem.getHeight() * escala;
        float esquerda = campo.left + (campo.width() - largura) / 2f;
        float topo = campo.top + (campo.height() - altura) / 2f;
        areaAssinatura.set(esquerda, topo, esquerda + largura, topo + altura);
        canvas.drawBitmap(imagem, null, areaAssinatura, pintaPagina);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evento) {
        if (!editavel || pagina == null) {
            return super.onTouchEvent(evento);
        }
        calcularDestino();
        switch (evento.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                selecionado = campoEm(evento.getX(), evento.getY());
                arrastando = selecionado != null;
                if (arrastando) {
                    paraTela(selecionado, retangulo);
                    deslocX = evento.getX() - retangulo.left;
                    deslocY = evento.getY() - retangulo.top;
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (ouvinte != null) {
                    ouvinte.aoSelecionar(selecionado);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (arrastando && selecionado != null) {
                    double x = (evento.getX() - deslocX - destino.left) / destino.width();
                    double y = (evento.getY() - deslocY - destino.top) / destino.height();
                    selecionado.x = limitar(x, selecionado.largura);
                    selecionado.y = limitar(y, selecionado.altura);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (arrastando && selecionado != null && ouvinte != null) {
                    ouvinte.aoMover(selecionado);
                }
                arrastando = false;
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                arrastando = false;
                return true;
            default:
                return super.onTouchEvent(evento);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /**
     * Mantém o campo inteiro dentro da página: 0 <= início e início + tamanho <= 1.
     * O laço final corrige o arredondamento de double (ex.: 0.7 + 0.3 pode passar de 1).
     */
    public static double limitar(double inicio, double tamanho) {
        double valor = Math.max(0, Math.min(inicio, 1.0 - tamanho));
        while (valor > 0 && valor + tamanho > 1.0) {
            valor = Math.max(0, valor - 1e-6);
        }
        return valor;
    }

    private CampoAssinatura campoEm(float x, float y) {
        List<CampoAssinatura> daPagina = new ArrayList<>();
        for (CampoAssinatura c : campos) {
            if (c.pagina == indicePagina) {
                daPagina.add(c);
            }
        }
        // O último desenhado fica por cima: procura de trás para frente.
        for (int i = daPagina.size() - 1; i >= 0; i--) {
            paraTela(daPagina.get(i), retangulo);
            if (retangulo.contains(x, y)) {
                return daPagina.get(i);
            }
        }
        return null;
    }

    private void paraTela(CampoAssinatura c, RectF saida) {
        saida.set(
                (float) (destino.left + c.x * destino.width()),
                (float) (destino.top + c.y * destino.height()),
                (float) (destino.left + (c.x + c.largura) * destino.width()),
                (float) (destino.top + (c.y + c.altura) * destino.height()));
    }

    /** Encaixa a página inteira na view, sem distorcer, centralizada. */
    private void calcularDestino() {
        float larguraView = getWidth() - getPaddingLeft() - getPaddingRight();
        float alturaView = getHeight() - getPaddingTop() - getPaddingBottom();
        if (larguraView <= 0 || alturaView <= 0) {
            destino.set(0, 0, 0, 0);
            return;
        }
        float escala = Math.min(larguraView / pagina.getWidth(), alturaView / pagina.getHeight());
        float largura = pagina.getWidth() * escala;
        float altura = pagina.getHeight() * escala;
        float esquerda = getPaddingLeft() + (larguraView - largura) / 2f;
        float topo = getPaddingTop() + (alturaView - altura) / 2f;
        destino.set(esquerda, topo, esquerda + largura, topo + altura);
    }

    private int corDe(long signatarioId) {
        Integer cor = cores.get(signatarioId);
        return cor != null ? cor : Color.DKGRAY;
    }
}
