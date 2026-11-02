package br.com.fiap.letssign.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import br.com.fiap.letssign.R;
import br.com.fiap.letssign.repository.Repositorios;
import br.com.fiap.letssign.repository.UsuarioRepository;
import br.com.fiap.letssign.util.Tarefas;

/** Lista de Signatários cadastrados. Tocar abre a edição. */
public class SignatariosFragment extends Fragment {

    private UsuarioAdapter adapter;
    private EstadoVazio vazio;
    private TextView textoLimite;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View raiz = inflater.inflate(R.layout.fragment_signatarios, container, false);
        adapter = new UsuarioAdapter(usuario -> {
            Intent intent = new Intent(requireContext(), SignatarioFormActivity.class);
            intent.putExtra(SignatarioFormActivity.EXTRA_USUARIO_ID, usuario.id);
            startActivity(intent);
        });
        RecyclerView lista = raiz.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        lista.setAdapter(adapter);

        textoLimite = raiz.findViewById(R.id.texto_limite);
        vazio = new EstadoVazio(raiz);
        vazio.configurar(R.drawable.ic_pessoas, R.string.vazio_signatarios_titulo,
                R.string.vazio_signatarios_detalhe, R.string.botao_novo_signatario,
                v -> ((AdminHomeActivity) requireActivity()).novoSignatario());
        return raiz;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            carregar();
        }
    }

    private void carregar() {
        UsuarioRepository usuarios = Repositorios.usuarios(requireContext());
        Tarefas.executar(requireActivity(), usuarios::listarSignatarios, lista -> {
            if (!isAdded()) {
                return;
            }
            adapter.mostrar(lista);
            vazio.mostrar(lista.isEmpty());
            textoLimite.setText(getString(R.string.signatarios_de_limite, lista.size(),
                    UsuarioRepository.LIMITE_SIGNATARIOS));
            ((AdminHomeActivity) requireActivity()).definirTotalSignatarios(lista.size());
        });
    }
}
