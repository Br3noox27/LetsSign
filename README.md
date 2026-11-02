# LetsSign (protótipo Android, Java)

App nativo para preparar um PDF, enviar para assinatura e acompanhar a situação. Challenge LetsSign, FIAP 2TDS, Sprint 1.

## O que o app faz
- Login com 2 perfis: **Admin** e **Signatário**. A senha fica só como hash (SHA-256 com sal).
- **Admin**: cadastra e edita Signatários, cria um documento (título, PDF do celular, signatários com papel: contratante, contratado ou testemunha), posiciona os campos de assinatura nas páginas, revisa e envia. Depois acompanha tudo numa linha do tempo (criado, enviado, assinando, concluído), com a situação de cada signatário e o histórico.
- **Nova versão**: no acompanhamento de um documento já enviado, "Criar nova versão" gera um rascunho (versão 2, 3...) com o mesmo PDF, signatários, papéis e campos, sem assinaturas. O original não muda.
- **Signatário**: vê os documentos pendentes e os assinados. Assina com o dedo e salva. Quando todos assinam, o documento fica **concluído**.
- Banco local Room. O PDF é copiado para a pasta do app; o banco guarda nome, caminho, datas e status. Os campos ficam guardados em proporção da página (0 a 1).
- Visual preto e branco. O app abre no **tema claro**; o botão de meio círculo na barra de cima troca para o escuro e a escolha fica guardada.

## Logins de teste
Criados (ou completados) sozinhos toda vez que o app abre, também em instalações antigas. Quem já existe não é duplicado nem alterado.

| Perfil | Nome | E-mail | Senha |
|---|---|---|---|
| Admin | Administrador LetsSign | `admin@letssign.com` | `admin123` |
| Signatário | Vitor Silva | `vitor.fiap@exemplo.com` | `123456` |
| Signatário | Breno Silva | `breno.fiap@exemplo.com` | `123456` |
| Signatário | Julia Azevedo | `julia.fiap@exemplo.com` | `123456` |
| Signatário | Yuri Luna | `yuri.fiap@exemplo.com` | `123456` |

Os e-mails são de exemplo (não são os institucionais). Outros signatários podem ser cadastrados pelo Admin, na aba Signatários (não existe tela de "criar conta" no login).

**Limite: 10 signatários.** A aba Signatários mostra "N de 10 signatários cadastrados". Com 10, o botão vira "Limite de signatários atingido" e fica travado; editar quem já existe continua permitido. O número fica numa constante só: `UsuarioRepository.LIMITE_SIGNATARIOS`.

## Documento pré-carregado
Ao abrir, o app garante o documento **"Acordo de trabalho do grupo"** (PDF em `app/src/main/assets/acordo-grupo-letssign.pdf`, 2 páginas). Ele já vem **Enviado** para os 4 integrantes, todos como Contratante, com um campo de assinatura para cada um na página 2. Se já existir um documento com esse título, nada é recriado.

Para a demonstração: cada integrante entra com o seu login, abre o documento em **Pendentes**, assina no quadro e salva. O status vai de **Enviado** para **Parcial** (1 a 3 assinaturas) e **Concluído** (as 4). O Admin acompanha tudo na linha do tempo.

## Gerar o APK
No Windows, dentro desta pasta:
```
gradlew.bat assembleDebug
```
O arquivo sai em `app\build\outputs\apk\debug\app-debug.apk`. Testes: `gradlew.bat test`.
Precisa do JDK 17 ou mais novo e do `local.properties` com o caminho do Android SDK (`sdk.dir=...`).

## Instalar no celular
1. O APK sai do build em `app\build\outputs\apk\debug\app-debug.apk` (o mesmo arquivo de "Gerar o APK"). Envie esse arquivo ao celular por cabo USB, Google Drive ou WhatsApp.
2. Abra o arquivo no celular. O Android vai pedir para **permitir instalar apps desconhecidos** para o app que abriu o arquivo (Arquivos, Drive...). Permita.
3. Toque em Instalar. Se aparecer aviso do Play Protect, toque em "Instalar mesmo assim" (é um APK de teste, sem assinatura da loja).
Precisa de Android 8.0 ou mais novo.

## Teste rápido do fluxo
1. Entre como Admin. Toque em **Novo documento**: título, escolha um PDF, adicione os signatários e o papel de cada um.
2. Posicione pelo menos 1 campo para cada signatário, toque em **Revisar envio** e depois em **Enviar para assinatura**.
3. Toque em **Sair** e entre como um signatário. Abra o pendente, assine no quadro e salve.
4. Volte como Admin, abra o documento (linha do tempo) e teste **Criar nova versão**.

## O que NÃO está pronto
- A assinatura **não é gravada dentro do PDF** (fica como imagem PNG na pasta do app, ligada ao documento). Gravar no PDF exige PDFBox-Android.
- Não existe envio real de e-mail ou notificação: o "convite" é registrado no app e o signatário vê o documento ao entrar.
- Mensagem do convite é fixa (não personalizável).
- Sem backend: tudo fica no celular. Os repositórios estão separados por interface para trocar por API REST na Sprint 2.
- Um admin não vê os documentos de outro admin, mas não há tela para cadastrar outros admins.
- A regra `validarEnvio` é **provisória** (marcada no código): o Breno vai reescrever.
- Testado no computador (JUnit e Robolectric) e no emulador Android 15 (01/11/2026). **Não testado num celular real.**

## Créditos
Fontes Sora e Plus Jakarta Sans (Google Fonts, licença SIL Open Font License 1.1), em `app/src/main/res/font`.
