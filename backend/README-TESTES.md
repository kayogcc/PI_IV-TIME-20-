<!-- Autores: Carlos Eduardo RA: 25020992, Gabriel Albasini RA: 25019916 -->

# Testes automatizados do back-end — EstoqueHosp

Este documento explica os testes adicionados em `ServicosTest.java`: o que foi criado, o que cada grupo verifica e como executar.

## 1. Visão geral

O projeto já tinha estes testes:

| Arquivo | O que cobre |
|---|---|
| `ConexaoTest.java` | Grava e lê um documento no Atlas |
| `banco/ConexaoMongoTest.java` | Validação da URI de conexão (CT-01 a CT-06) |
| `http/SessoesTest.java` | Abrir, recuperar e fechar sessões |
| `servico/RegrasTest.java` | Hash de senha e cálculo de alertas |

Faltavam as regras de negócio principais. Elas foram reunidas em **um único arquivo**, para não multiplicar classes e pastas:

```
backend/src/test/java/br/puc/estoquehosp/servico/ServicosTest.java
```

O arquivo é organizado em cinco classes internas (`@Nested`), uma por assunto: **Materiais, Movimentações, Usuários, Permissões e Histórico**. O relatório do Maven mostra cada grupo separado.

Nenhum arquivo do projeto precisou ser alterado para os testes funcionarem (veja a seção 5 sobre uma correção recomendada).

## 2. Dois tipos de teste

**Testes sem banco.** Cobrem validações que o código faz *antes* de acessar o MongoDB (campo vazio, quantidade inválida, perfil errado etc.). Os serviços são criados com repositório `null`. Rodam em qualquer computador.

**Testes com banco.** Cobrem o que depende de dados reais: duplicidade, saldo, login, histórico. Cada um começa chamando `ambiente()`, que:

1. lê a variável de ambiente `MONGO_URI`;
2. se ela **não existir**, o teste é **ignorado** (não falha), como no `ConexaoTest`;
3. se existir, conecta no banco **`estoquehosp_teste`** e esvazia as coleções `materiais`, `movimentacoes` e `usuarios`.

O banco de teste é separado do banco real (`estoquehosp`), então os dados de verdade não são afetados.

## 3. O que cada grupo testa

### Materiais
| Cenário | Banco? |
|---|---|
| Nome obrigatório (nulo, vazio, só espaços, 1 letra) e máximo de 100 caracteres | não |
| Quantidade mínima inválida (nula, negativa, acima de 100.000) | não |
| Código de barras inválido (curto, com espaço, com símbolo) | não |
| Validade inválida (formato errado, mês 13, texto) | não |
| Lote acima de 30 caracteres | não |
| Cadastro válido: nome com espaços limpos, saldo inicial zero, material recuperável por id | sim |
| Nome duplicado recusado (ignora maiúsculas e acentos) | sim |
| Código de barras duplicado recusado | sim |
| Nome e código reaproveitáveis depois de inativar o material | sim |
| Edição aceita o próprio nome, mas não o de outro material | sim |
| Material inativo some da lista e não pode ser editado | sim |
| Buscar id inexistente ou inválido retorna 404 | sim |
| Busca na lista ignora acentos e maiúsculas, por nome ou código | sim |

### Movimentações
| Cenário | Banco? |
|---|---|
| Tipo, origem e local inválidos; material em branco; quantidade 0, negativa, nula ou acima do limite | não |
| Campos ausentes (tipo, origem, local) devem retornar 400 | não |
| Centro Cirúrgico não pode registrar entrada (403) | não |
| Entrada aumenta o saldo e devolve o registro | sim |
| Saída diminui o saldo | sim |
| Saída maior que o saldo é recusada (409) e não altera saldo nem histórico | sim |
| Saída do saldo exato é permitida | sim |
| Cada local (Almoxarifado A, B, Centro Cirúrgico) tem saldo próprio | sim |
| Material encontrado por código de barras ou por nome; inexistente dá 404 | sim |
| Nome ambíguo (ex.: "luva" com "Luva P" e "Luva M") não movimenta nada | sim |
| Material inativo não pode ser movimentado | sim |
| Centro Cirúrgico pode registrar saída | sim |
| 8 saídas simultâneas com estoque de 10 un.: só 3 passam e o saldo nunca fica negativo | sim |

### Usuários e login
| Cenário | Banco? |
|---|---|
| Nome inválido (sem sobrenome, com número, com símbolo) | não |
| Matrícula inválida (curta, com espaço, com símbolo) | não |
| Senha inválida (curta, sem número, sem letra, acima de 72 caracteres) | não |
| Perfil inválido ou ausente | não |
| Cadastro com cada um dos três perfis | sim |
| Senha guardada como hash (`pbkdf2$...`), nunca em texto | sim |
| Matrícula repetida recusada, mesmo trocando maiúsculas | sim |
| Login correto abre sessão válida | sim |
| Login não diferencia maiúsculas na matrícula | sim |
| Login incorreto retorna 401, com a mesma mensagem para "senha errada" e "usuário inexistente" | sim |
| Sair invalida o token | sim |

### Permissões por perfil
Estes testes sobem o sistema como ele roda de verdade, numa porta livre do próprio computador: **API HTTP → `PonteSocket` → servidor de sockets (`AceitadoraDeConexao` e `SupervisoraDeConexao`) → serviços**. Quem confere o login e o perfil (401/403) é a `SupervisoraDeConexao`. Precisam da `MONGO_URI` só para criar os serviços (usam o banco de teste e não gravam nada); sem ela, o grupo é ignorado.

| Cenário | Resultado esperado |
|---|---|
| Rotas protegidas sem token ou com token falso | 401 |
| `GET /api/eu` com login | 200 com os dados do usuário |
| Logout invalida o token | 204, e depois 401 |
| Rota inexistente / método errado | 404 / 405 |
| Centro Cirúrgico tenta cadastrar, editar ou inativar material | 403 |
| Almoxarifado e Gestão cadastram material | passam da permissão (caem na validação 400) |
| Gestão tenta registrar movimentação | 403 |
| Almoxarifado e Centro Cirúrgico registram movimentação | passam da permissão (400) |
| Todos os perfis consultam o histórico | passam da permissão (400 por `dias` inválido) |

Nesses casos, "passa da permissão" é verificado pelo código 400. Se o perfil fosse bloqueado, a resposta seria 403, antes de qualquer validação.

### Histórico
| Cenário | Banco? |
|---|---|
| Parâmetro `dias` inválido (0, -1, 366, texto, decimal) | não |
| Entrada e saída aparecem no histórico com material, tipo, quantidade, local, origem e usuário, do mais recente ao mais antigo | sim |
| Filtro por material | sim |
| Filtro por período (padrão de 30 dias e valores personalizados) | sim |
| Período e material combinados | sim |

## 4. Como executar

Dentro da pasta `backend`:

```bash
# Só os testes que não precisam de banco (MONGO_URI não definida):
mvn test

# Todos os testes, incluindo os que usam o banco de teste:
# Windows (PowerShell)
$env:MONGO_URI="sua-uri-do-atlas"; mvn test
# Linux / macOS
MONGO_URI="sua-uri-do-atlas" mvn test

# Só este arquivo:
mvn test -Dtest=ServicosTest
```

Sem `MONGO_URI`, o Maven mostra os testes com banco como **ignorados (skipped)**. Isso é esperado e não indica erro.

## 5. Correção recomendada no código (bug encontrado pelos testes)

O teste `camposAusentes` revela um problema em `MovimentacaoServico.registrar`: se a requisição chegar sem `tipo`, `origem` ou `local`, o `List.of(...).contains(null)` lança `NullPointerException`, e a API responde **500** em vez de **400**.

Correção (adicionar `dados.xxx() == null ||` antes de cada `contains`):

```java
if (dados.tipo() == null || !TIPOS.contains(dados.tipo())) throw ErroApi.requisicaoInvalida("Tipo inválido. Use Entrada ou Saída.");
if (dados.origem() == null || !ORIGENS.contains(dados.origem())) throw ErroApi.requisicaoInvalida("Origem inválida. Use Código de barras ou Manual.");
// ...
if (dados.local() == null || !LOCAIS.contains(dados.local())) throw ErroApi.requisicaoInvalida("Selecione um local válido.");
```

Essa correção **já está aplicada** no `MovimentacaoServico.java` da `main`, e o teste passa.

## 6. Versão do Java e o Maven

- **Maven** (`mvn`) é a ferramenta que compila o projeto, baixa as bibliotecas e roda os testes. O arquivo `backend/pom.xml` é a configuração dele.
- O `pom.xml` define `maven.compiler.release` como **25**, e o README do projeto cita Java 25 (LTS). Com essa configuração, compilar com um JDK mais antigo (por exemplo o 21) falha com o erro `release version 25 not supported`.
- Os testes novos **não exigem Java 25**: usam apenas recursos que existem desde o Java 21 ou antes.

Para executar, há duas opções:

1. **Instalar o JDK 25** (mantém o projeto como está). Confirme com `java -version` e `mvn -version`; as duas devem mostrar 25.
2. **Usar o JDK 21**, trocando no `pom.xml` o valor `25` por `21` em `maven.compiler.release`. Isso só funciona se nenhum arquivo do projeto usar recurso exclusivo do Java 22 ou superior; a forma de saber é rodar `mvn test` e ver se compila. Combine a troca com o grupo antes de alterar o `pom.xml`.

## 7. Limitações

- Os testes foram executados com o banco de teste: **119 testes do back-end, todos passando** (incluindo os que já existiam).
- Não são testados botões nem telas do front-end; o foco é a regra de negócio do back-end.
- `AlertaServico` não recebeu testes novos: os cálculos de alerta já são cobertos pelo `RegrasTest`. O `toString` seguro dos pedidos do servidor de sockets é coberto pelo `ToStringSeguroTest`.
