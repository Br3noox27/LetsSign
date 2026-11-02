package br.com.fiap.letssign;

import android.app.Application;

import br.com.fiap.letssign.servico.SementeDemonstracao;
import br.com.fiap.letssign.util.Tarefas;
import br.com.fiap.letssign.util.Tema;

public class LetsSignApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Começa no tema claro (ou no escuro, se a pessoa escolheu antes).
        Tema.aplicarSalvo(this);
        // Toda abertura: garante Admin, signatários do grupo e o documento do grupo, sem duplicar
        // (vale também para quem já tinha o app instalado). Entra na fila antes de qualquer login.
        Tarefas.emSegundoPlano(() -> SementeDemonstracao.garantir(this));
    }
}
