package br.com.fiap.letssign.repository;

/** Tentativa de cadastrar mais signatários do que o limite (UsuarioRepository.LIMITE_SIGNATARIOS). */
public class LimiteDeSignatariosException extends IllegalStateException {

    public LimiteDeSignatariosException(int limite) {
        super("O limite de " + limite + " signatários foi atingido. Edite um dos cadastrados.");
    }
}
