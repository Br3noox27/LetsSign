package br.com.fiap.letssign.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.data.Usuario;
import br.com.fiap.letssign.util.Formatos;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.Linha> {

    public interface AoTocar {
        void tocou(Usuario usuario);
    }

    private final AoTocar aoTocar;
    private final List<Usuario> itens = new ArrayList<>();

    public UsuarioAdapter(AoTocar aoTocar) {
        this.aoTocar = aoTocar;
    }

    @SuppressLint("NotifyDataSetChanged") // lista pequena, recarregada inteira
    public void mostrar(List<Usuario> lista) {
        itens.clear();
        itens.addAll(lista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Linha onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new Linha(LayoutInflater.from(pai.getContext()).inflate(R.layout.item_usuario, pai, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Linha linha, int posicao) {
        Usuario u = itens.get(posicao);
        linha.iniciais.setText(Formatos.iniciais(u.nomeCompleto));
        linha.nome.setText(u.nomeCompleto);
        linha.email.setText(u.email);
        linha.telefone.setText(u.telefone);
        linha.telefone.setVisibility(u.telefone == null || u.telefone.isEmpty() ? View.GONE : View.VISIBLE);
        linha.itemView.setOnClickListener(v -> aoTocar.tocou(u));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class Linha extends RecyclerView.ViewHolder {
        final TextView iniciais;
        final TextView nome;
        final TextView email;
        final TextView telefone;

        Linha(View view) {
            super(view);
            iniciais = view.findViewById(R.id.iniciais);
            nome = view.findViewById(R.id.nome);
            email = view.findViewById(R.id.email);
            telefone = view.findViewById(R.id.telefone);
        }
    }
}
