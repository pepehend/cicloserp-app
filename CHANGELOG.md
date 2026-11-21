# Histórico de versões

O formato segue o [README](README.md#versão-do-app): `MAIOR.MENOR.CORREÇÃO`.

## 0.8.0

Servidor mínimo: CiclosERP 3.4.0 (o número do orçamento por empresa vem do CiclosERP 3.8.0; antes disso o app mostra o id).

- Orçamentos (lista, ficha, orçamentos da OS e escolha de orçamento no formulário da OS) usam o número do orçamento da empresa, que começa em 1 em cada empresa.

## 0.7.0

Servidor mínimo: CiclosERP 3.4.0 (o número da OS por empresa vem do CiclosERP 3.7.0; antes disso o app mostra o id).

- A ficha da OS e os textos de compartilhar (WhatsApp) usam o número da OS da empresa, que começa em 1 em cada empresa.

## 0.6.0

Servidor mínimo: CiclosERP 3.4.0 (a empresa na Minha conta aparece com o CiclosERP 3.6.0, multiempresa).

- Minha conta mostra a empresa do usuário. O login é o mesmo: o servidor descobre a empresa pelo e-mail, e todos os dados que o app mostra são só dela.

## 0.5.0

Servidor mínimo: CiclosERP 3.4.0.

- Menu lateral com Início, Agenda, Ordens de serviço, Clientes, Orçamentos, Produtos, Serviços, Equipamentos e Minha conta, conforme as permissões.
- Cadastro, edição e exclusão de clientes (com busca de CNPJ e CEP), produtos, serviços e equipamentos; ficha do cliente e do produto; movimentar estoque.
- Nova ordem de serviço.
- Ficha da OS no padrão do app do técnico do sistema web: detalhes, relatório (fotos, questionários, quilometragem, relato, assinar e enviar, controle de horas), pendências, peças e serviços e orçamentos da OS.
- Sinalização de pendências na OS, no checklist e nos cartões das tarefas.
- Tema claro e escuro com as cores do sistema web.
- Envio do header `X-Api-Token` além do `Authorization` (hospedagens que removem o `Authorization`).

## 0.1.0

- Login por token e módulo de ordens de serviço do técnico (início, agenda, lista e ficha da OS, execução, questionários, fotos, relato e assinatura).
