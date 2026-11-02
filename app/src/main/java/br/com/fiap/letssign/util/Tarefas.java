package br.com.fiap.letssign.util;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Roda banco e PDF fora da thread da tela e devolve o resultado na tela.
 * Uma fila só: as tarefas acontecem na ordem em que foram pedidas.
 */
public final class Tarefas {

    private static final String TAG = "LetsSign";
    private static final ExecutorService FILA = Executors.newSingleThreadExecutor();
    private static final Handler TELA = new Handler(Looper.getMainLooper());

    public interface Trabalho<T> {
        T executar() throws Exception;
    }

    public interface Resposta<T> {
        void receber(T valor);
    }

    private Tarefas() {
    }

    public static <T> void executar(Activity tela, Trabalho<T> trabalho, Resposta<T> sucesso) {
        executar(tela, trabalho, sucesso, erro ->
                Toast.makeText(tela, "Algo deu errado: " + mensagem(erro), Toast.LENGTH_LONG).show());
    }

    public static <T> void executar(Activity tela, Trabalho<T> trabalho, Resposta<T> sucesso,
                                    Resposta<Exception> falha) {
        FILA.execute(() -> {
            try {
                T resultado = trabalho.executar();
                TELA.post(() -> {
                    if (aberta(tela)) {
                        sucesso.receber(resultado);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Falha numa tarefa", e);
                TELA.post(() -> {
                    if (aberta(tela)) {
                        falha.receber(e);
                    }
                });
            }
        });
    }

    /** Para tarefas sem resposta (ex.: salvar ao sair da tela). */
    public static void emSegundoPlano(Runnable trabalho) {
        FILA.execute(() -> {
            try {
                trabalho.run();
            } catch (RuntimeException e) {
                Log.e(TAG, "Falha numa tarefa em segundo plano", e);
            }
        });
    }

    public static String mensagem(Throwable erro) {
        Throwable causa = erro;
        while (causa.getCause() != null && causa.getMessage() == null) {
            causa = causa.getCause();
        }
        return causa.getMessage() != null ? causa.getMessage() : causa.getClass().getSimpleName();
    }

    private static boolean aberta(Activity tela) {
        return !tela.isFinishing() && !tela.isDestroyed();
    }
}
