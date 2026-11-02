# LetsSign

App Android nativo (Java) para preparar um PDF, enviar para assinatura e acompanhar a situação de cada signatário.
Challenge LetsSign, FIAP 2TDS, Sprint 1.

## Funcionalidades
- **Login com 2 perfis:** Admin e Signatário. A senha é guardada só como hash (SHA-256 com sal).
- **Admin:** cadastra e edita Signatários e cria um documento: título, PDF do celular e signatários, cada um com um papel (contratante, contratado ou testemunha). Posiciona os campos de assinatura nas páginas, revisa e envia. Depois acompanha tudo numa linha do tempo (criado, enviado, assinando, concluído), com a situação de cada signatário e o histórico.
- **Signatário:** vê os documentos pendentes e os assinados, assina com o dedo e salva. Quando todos assinam, o documento fica **concluído**.
- **Status do documento:** Rascunho, Enviado, Parcial (algumas assinaturas) e Concluído.
- **Nova versão:** em um documento já enviado, "Criar nova versão" gera um rascunho (versão 2, 3...) com o mesmo PDF, signatários, papéis e campos, sem assinaturas. O original não muda.
- **Limite de 10 signatários:** a aba Signatários mostra "N de 10 signatários cadastrados". Com 10, o botão de cadastro fica travado ("Limite de signatários atingido"); editar quem já existe continua permitido. O valor fica numa constante só: `UsuarioRepository.LIMITE_SIGNATARIOS`.
- **Tema claro e escuro:** o app abre no tema claro; o botão de meio círculo na barra de cima troca, e a escolha fica guardada.

## Como o projeto está organizado
Código em `app/src/main/java/br/com/fiap/letssign`:

| pasta | o que tem |
|---|---|
| `dominio` | regras de negócio e enums (`Regras`, `StatusDocumento`, `PapelSignatario`, `Perfil`) |
| `data` | banco Room: entidades e DAOs |
| `repository` | interfaces dos repositórios; a implementação em Room fica em `repository/room` |
| `servico` | `EnvioDocumentoService` (valida e dispara o envio sem duplicar) e a carga do documento de demonstração |
| `ui` | as telas (Activities e Fragments) |
| `util` | PDF, desenho da assinatura, hash de senha, tema e utilitários |

O PDF escolhido é copiado para a pasta do app; o banco guarda nome, caminho, datas e status. A posição de cada campo de assinatura é guardada em proporção da página (0 a 1). Os repositórios são interfaces, para trocar o banco local por uma API REST na Sprint 2.

## Como rodar
Precisa de JDK 17 ou mais novo e do arquivo `local.properties` na raiz com o caminho do Android SDK (`sdk.dir=...`).

Gerar o APK (no Windows; em Linux ou macOS use `./gradlew`):
```
gradlew.bat assembleDebug
```
O arquivo sai em `app\build\outputs\apk\debug\app-debug.apk`.

Rodar os testes:
```
gradlew.bat testDebugUnitTest
```

### Instalar no celular
1. Envie o `app-debug.apk` ao celular (cabo USB, Google Drive ou WhatsApp).
2. Abra o arquivo no celular e permita **instalar apps desconhecidos** para o app que o abriu.
3. Toque em Instalar. Se aparecer o aviso do Play Protect, toque em "Instalar mesmo assim" (é um APK de teste, sem assinatura da loja).

Precisa de Android 8.0 ou mais novo.

## Logins de teste
Criados sozinhos quando o app abre. Quem já existe não é duplicado nem alterado.

| Perfil | Nome | E-mail | Senha |
|---|---|---|---|
| Admin | Administrador LetsSign | `admin@letssign.com` | `admin123` |
| Signatário | Vitor Silva | `vitor.fiap@exemplo.com` | `123456` |
| Signatário | Breno Silva | `breno.fiap@exemplo.com` | `123456` |
| Signatário | Julia Azevedo | `julia.fiap@exemplo.com` | `123456` |
| Signatário | Yuri Luna | `yuri.fiap@exemplo.com` | `123456` |

Os e-mails são de exemplo. Outros signatários são cadastrados pelo Admin, na aba Signatários (não existe tela de "criar conta" no login).

## Demonstração
Ao abrir, o app garante o documento **"Acordo de trabalho do grupo"** (PDF de 2 páginas em `app/src/main/assets/acordo-grupo-letssign.pdf`). Ele já vem **Enviado** para os 4 integrantes, todos como Contratante, com um campo de assinatura para cada um na página 2.

1. Entre com o login de um integrante, abra o documento em **Pendentes**, assine no quadro e salve.
2. Repita com os outros três. O status vai de **Enviado** para **Parcial** (1 a 3 assinaturas) e **Concluído** (as 4).
3. Entre como Admin e abra o documento para ver a linha do tempo e testar **Criar nova versão**.

Para criar um documento do zero: como Admin, **Novo documento** → título, PDF e signatários com papel → posicione pelo menos 1 campo para cada signatário → **Revisar envio** → **Enviar para assinatura**.

## Testes
54 testes automatizados (JUnit e Robolectric) cobrem as regras de negócio, os repositórios em Room, o envio sem duplicar e o login dos usuários de teste. O fluxo completo também foi percorrido no emulador Android 15 em 01/11/2026. Não foi testado num celular real.

## Limitações conhecidas
- A assinatura **não é gravada dentro do PDF**: fica como imagem PNG na pasta do app, ligada ao documento. Gravar no PDF exige uma biblioteca como o PDFBox-Android. Por isso não há "PDF final" gerado quando o documento conclui.
- Não existe envio real de e-mail ou notificação: o "convite" é registrado no app e o signatário vê o documento ao entrar. A mensagem do convite é fixa.
- Não há backend: tudo fica no celular.
- Um Admin não vê os documentos de outro Admin, mas não há tela para cadastrar outros Admins.

## Créditos
Fontes Sora e Plus Jakarta Sans (Google Fonts, licença SIL Open Font License 1.1), em `app/src/main/res/font`.
