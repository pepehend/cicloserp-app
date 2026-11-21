# Histórico de versões

O formato segue o [README](README.md#versão-do-app): `MAIOR.MENOR.CORREÇÃO`.

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
