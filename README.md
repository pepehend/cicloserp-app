# CiclosERP (app Android)

Aplicativo Android do [CiclosERP](https://github.com/pepehend/ciclos-erp). Usa **o mesmo banco de dados, as mesmas regras e as mesmas permissões** do sistema web, pela API dele (`api.php`).

## Como o app acessa o banco

O app **não** conecta direto no MySQL/MariaDB. Ele conversa com o `api.php` do sistema web, que já valida permissões, estoque, financeiro etc. O acesso é por **token**:

1. `POST api.php?r=token_login` com `{email, password, device}` devolve um token `cer_…`.
2. Todo pedido seguinte leva `Authorization: Bearer <token>` (sem cookie nem CSRF).
3. O token cai ao sair do app, ao trocar a senha, se o usuário for desativado ou após 60 dias sem uso. Em **Minha conta → Aparelhos do aplicativo** (no sistema web) dá para remover um aparelho.

> O login por token chega ao `ciclos-erp` na **v3.4.0** (branch `development`). Servidores mais antigos respondem "Este servidor ainda não aceita o aplicativo".

## O que já existe no app

Módulo de **ordens de serviço / técnico**, espelhando o app do técnico do sistema web:

- Login (e-mail e senha) e sessão salva no aparelho (fora do backup). O servidor é fixo e não aparece no app.
- Início (OS de hoje, atrasadas, em execução), Agenda semanal (com filtros e "Equipe" para quem tem `ordens.all`) e lista de OS com busca e filtro de situação.
- Ficha da OS: detalhes (telefone e endereço abrem discador e mapa), peças/serviços, controle de horas e pendências.
- Execução: A caminho, Check-in, Pausar (com motivo), Retomar e Check-out (respeitando as pendências do tipo de tarefa).
- Relatório: questionários com os 12 tipos de pergunta e perguntas condicionais (mesma lógica do servidor), fotos (câmera ou galeria, reduzidas antes de enviar), relato, km e assinatura.
- Depois do check-out, no celular a OS fica só para consulta (igual ao app web).

Navegação por **menu lateral** (como o app do técnico do sistema web), conforme as permissões do usuário, sobre as mesmas tabelas do `ciclos-erp`:

- **Clientes** (`customers`): lista com busca e "Mostrar inativos"; ficha com total comprado, lucro, ticket médio, o que mais compra, portal e equipamentos.
- **Orçamentos** (`quotes`): indicadores do mês (tocar filtra por status), busca, visão Lista ou Kanban (somente leitura) e orçamento completo com itens e custos.
- **Produtos** e **Serviços** (`products`): busca, estoque baixo, inativos.
- **Equipamentos** (`equipments`) e **Minha conta**.
- Início com atalhos (Agenda, Clientes, Orçamento, Produto).

Ainda não existe (só consulta; criar/editar segue pelo sistema web): vendas, compras, financeiro, notas e relatórios.

## Estrutura

```
data/AppConfig.kt            servidor fixo do aplicativo (`SERVER_URL`)
data/api/ApiClient.kt        OkHttp: Bearer token e erros da API
data/CiclosRepository.kt     rotas usadas (token_login, tech_home, tech_tasks, service_orders, service_order_detail, …)
data/model/                  modelos e QuestionLogic (condicionais, igual ao PHP)
data/SessionStore.kt         DataStore com o token
ui/                          Compose: login, menu lateral, início, agenda, ordens, clientes, orçamentos, produtos/serviços, equipamentos
```

## Rodar e testar

- Requisitos: Android Studio, `minSdk 28`, `targetSdk 37`.
- Servidor: fixo em `AppConfig.SERVER_URL` (`data/AppConfig.kt`); para apontar o app para outro sistema, troque essa constante. O HTTP sem criptografia só é liberado para o endereço dela em `res/xml/network_security_config.xml`; com um domínio `https://` basta trocar a constante e remover a exceção.
- Testes unitários: `./gradlew :app:testDebugUnitTest`.

## Versão do app

A versão fica em `app/build.gradle.kts` (`versionName` e `versionCode`) e aparece em **Minha conta**. As regras seguem as do sistema web (`ciclos-erp`):

- **Primeiro número (maior):** muda quando algo deixa de ser compatível (por exemplo, o app passa a exigir um CiclosERP mais novo).
- **Segundo número (menor):** muda a cada funcionalidade nova. O terceiro volta a `0`.
- **Terceiro número (correção):** muda a cada correção sem funcionalidade nova.
- `versionCode = maior * 10000 + menor * 100 + correção` (a versão `0.5.0` tem `versionCode` 500). O Android exige que ele só aumente.
- Enquanto o primeiro número for `0`, o app ainda está em construção.
- Cada PR que muda o comportamento do app atualiza `versionName`, `versionCode` e o `CHANGELOG.md` no mesmo commit.

O app exige o CiclosERP **3.4.0** ou mais novo no servidor (login por token); servidores mais antigos respondem "Este servidor ainda não aceita o aplicativo".

## Multiempresa (CiclosERP 3.6.0)

O sistema web guarda várias empresas no mesmo banco, cada uma isolada pelo `company_id`. Para o app nada muda no login: o e-mail é único no banco todo e o servidor descobre a empresa pelo usuário (um usuário pertence a uma empresa só). O token (`cer_…`) fica ligado ao usuário e, portanto, à empresa dele; todas as rotas devolvem só dados dela. `token_login` e `me` passam a trazer `company: {id, name, slug}` (o app mostra o nome em **Minha conta**; servidor mais antigo não manda e o app continua funcionando). O superadmin, que cria as empresas pelo sistema web, não usa o app (o servidor recusa o login dele aqui). Empresa desativada: o login e os tokens do app dela deixam de valer.