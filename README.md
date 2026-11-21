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

- Login (endereço do sistema, e-mail e senha) e sessão salva no aparelho (fora do backup).
- Início (OS de hoje, atrasadas, em execução), Agenda semanal (com filtros e "Equipe" para quem tem `ordens.all`) e lista de OS com busca e filtro de situação.
- Ficha da OS: detalhes (telefone e endereço abrem discador e mapa), peças/serviços, controle de horas e pendências.
- Execução: A caminho, Check-in, Pausar (com motivo), Retomar e Check-out (respeitando as pendências do tipo de tarefa).
- Relatório: questionários com os 12 tipos de pergunta e perguntas condicionais (mesma lógica do servidor), fotos (câmera ou galeria, reduzidas antes de enviar), relato, km e assinatura.
- Depois do check-out, no celular a OS fica só para consulta (igual ao app web).

Ainda não existe: orçamentos, vendas, produtos, clientes, compras, financeiro, notas e relatórios.

## Estrutura

```
data/api/ApiClient.kt        OkHttp: Bearer token, erros da API, endereço do servidor
data/CiclosRepository.kt     rotas usadas (token_login, tech_home, tech_tasks, service_orders, service_order_detail, …)
data/model/                  modelos e QuestionLogic (condicionais, igual ao PHP)
data/SessionStore.kt         DataStore com servidor e token
ui/                          Compose: login, início, agenda, ordens, ficha da OS e relatório
```

## Rodar e testar

- Requisitos: Android Studio, `minSdk 28`, `targetSdk 37`.
- Servidor de testes: `http://190.102.41.159` (HTTP liberado só para ele e para `10.0.2.2` em `res/xml/network_security_config.xml`; em produção use sempre `https://`).
- Servidor local pelo emulador: `10.0.2.2:<porta>`.
- **Android 17+**: servidor em rede local (`192.168.x.x`, `10.x.x.x`) exige a permissão `ACCESS_LOCAL_NETWORK`; o app pede no login. Servidor com domínio público não pede.
- Testes unitários: `./gradlew :app:testDebugUnitTest`.
