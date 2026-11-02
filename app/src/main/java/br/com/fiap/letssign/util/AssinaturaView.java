package br.com.fiap.letssign.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import br.com.fiap.letssign.R;

/** Quadro onde a pessoa assina com o dedo. Guarda o traço e gera a imagem da assinatura. */
public class AssinaturaView extends View {

    public interface AoMudar {
        void mudou(boolean vazia);
    }

    private final Path traco = new Path();
    private final Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linhaBase = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean vazia = true;
    private AoMudar aoMudar;

    public AssinaturaView(Context context) {
        this(context, null);
    }

    public AssinaturaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float densidade = getResources().getDisplayMetrics().density;
        tinta.setColor(ContextCompat.getColor(context, R.color.tinta_assinatura));
        tinta.setStyle(Paint.Style.STROKE);
        tinta.setStrokeWidth(3.5f * densidade);
        tinta.setStrokeCap(Paint.Cap.ROUND);
        tinta.setStrokeJoin(Paint.Join.ROUND);
        linhaBase.setColor(Color.argb(60, 0, 0, 0));
        linhaBase.setStrokeWidth(densidade);
    }

    public void setAoMudar(AoMudar ouvinte) {
        aoMudar = ouvinte;
    }

    public boolean estaVazia() {
        return vazia;
    }

    public void limpar() {
        traco.reset();
        vazia = true;
        invalidate();
        avisar();
    }

    /** Imagem da assinatura com fundo branco, do tamanho do quadro. */
    public Bitmap gerarImagem() {
        Bitmap imagem = Bitmap.createBitmap(Math.max(1, getWidth()), Math.max(1, getHeight()), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(imagem);
        canvas.drawColor(Color.WHITE);
        canvas.drawPath(traco, tinta);
        return imagem;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float y = getHeight() * 0.75f;
        float margem = getWidth() * 0.08f;
        canvas.drawLine(margem, y, getWidth() - margem, y, linhaBase);
        canvas.drawPath(traco, tinta);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evento) {
        float x = evento.getX();
        float y = evento.getY();
        switch (evento.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                traco.moveTo(x, y);
                traco.lineTo(x + 0.1f, y + 0.1f);
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < evento.getHistorySize(); i++) {
                    traco.lineTo(evento.getHistoricalX(i), evento.getHistoricalY(i));
                }
                traco.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                performClick();
                if (vazia) {
                    vazia = false;
                    avisar();
                }
                break;
            default:
                return super.onTouchEvent(evento);
        }
        invalidate();
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void avisar() {
        if (aoMudar != null) {
            aoMudar.mudou(vazia);
        }
    }
}
