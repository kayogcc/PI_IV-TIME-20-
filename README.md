# EstoqueHosp — Plataforma de Automação e Gestão de Estoque Hospitalar

**Projeto Integrador IV — Engenharia de Software**
**PUC Campinas — 2026**

## Descrição do Projeto

O **EstoqueHosp** é uma plataforma modular de automação e gestão de estoque hospitalar, voltada a hospitais de pequeno e médio porte e a centros cirúrgicos. Hoje esses hospitais controlam materiais com papel, planilhas e memória, o que gera erros de registro, dificuldade para acompanhar validades e atraso na percepção de estoque baixo, às vezes só notado quando o material já é necessário na cirurgia. As soluções automatizadas completas, como RFID, têm custo alto e deixam os hospitais menores de fora.

O projeto integra conhecimentos de **Programação em Java**, **banco de dados NoSQL (MongoDB)** e **API REST**. O núcleo é uma API em camadas (controller, serviço e repositório) que recebe as movimentações de estoque e aplica as mesmas regras de negócio, qualquer que seja a origem da leitura. O sistema contempla dois módulos principais:

- **Módulo de Gestão:** login com perfis de acesso (Almoxarifado, Centro Cirúrgico e Gestão), cadastro, edição, inativação e busca de materiais (lote, validade, quantidade mínima) e gestão de usuários.
- **Módulo de Movimentação:** entrada e saída por código de barras (câmera do celular) ou registro manual, com validação de saldo, histórico de movimentações, alertas de estoque baixo e validade próxima e dashboard essencial.

### Níveis de automação

O hospital começa no nível Bronze e pode evoluir sem trocar de sistema nem perder o histórico:

| Nível | Captura | Situação |
|---|---|---|
| **Bronze** | Código de barras pelo celular, sem hardware adicional | **MVP deste projeto** |
| **Prata** | Sensores de peso ou visão computacional | Evolução futura |
| **Ouro** | RFID | Evolução futura |

### Objetivos

- Registrar 100% das movimentações do MVP pela plataforma.
- Reduzir erros de registro e itens vencidos por falta de acompanhamento.
- Perceber estoque baixo antes da falta.
- Diminuir o tempo gasto em conferência manual.

### Cronograma (10 semanas)

| Semana | Etapa |
|---|---|
| 1–2 | Levantamento de requisitos, modelagem das coleções no MongoDB e arquitetura da API |
| 3–4 | Prototipação (12 telas) e validação com usuários |
| 5–6 | Back-end do MVP (cadastro de materiais e movimentações) |
| 7 | Front-end do MVP e leitura de código de barras pela câmera |
| 8 | Alertas de estoque baixo e validade próxima; dashboard |
| 9 | Testes automatizados (JUnit) e ajustes |
| 10 | Documentação, apresentação e planejamento dos níveis Prata e Ouro |

## Integrantes

| Nome | GitHub |
|---|---|
| *Bruno Lobo de Jesus* | *brunolobo-jesus* |
| *Carlos Eduardo Marins Fonseca* | *Cadu-Marins* |
| *Gabriel Figueira Albasini* | *GabrielAlbasini* |
| *Kayo Gabriel* | *kayogcc* |
| *Leonardo Fonseca de Oliveira* | *leo-fonseca-oliveira* |

## Tecnologias Utilizadas

| Tecnologia / Ferramenta | Descrição |
|---|---|
| Java 25 (LTS) | Linguagem de programação principal do back-end |
| HttpServer (JDK) | Servidor da API REST, sem frameworks externos |
| MongoDB Atlas | Banco de dados NoSQL orientado a documentos, hospedado na nuvem |
| MongoDB Java Driver | Conexão entre o Java e o MongoDB |
| Gson | Conversão entre objetos Java e JSON |
| Maven | Gerenciamento de dependências e build do back-end |
| JUnit 5 | Testes automatizados |
| HTML, CSS e JavaScript | Front-end web responsivo |
| Vite | Servidor de desenvolvimento e build do front-end |
| Git + GitHub | Controle de versão e repositório |
| GitHub Projects | Gerenciamento de tarefas e apontamento de esforço |
| VSCode | IDE de desenvolvimento |

## Instruções para Execução do Sistema

### Pré-requisitos

- Java JDK 25 (LTS) ou superior
- Maven
- Node.js 20.19 ou superior e npm
- Acesso ao projeto do grupo no MongoDB Atlas

### Configuração do Banco de Dados

O banco fica no **MongoDB Atlas**. A string de conexão contém usuário e senha, por isso **nunca é colocada no código nem no repositório**: ela é lida da variável de ambiente `MONGO_URI`.

1. No Atlas, crie o seu usuário em **Database Access** (permissão *Read and write to any database*).
2. Em **Network Access**, libere o seu IP atual (**Add Current IP Address**).
3. Em **Connect → Drivers → Java**, copie a connection string e substitua `<db_password>` pela sua senha.
4. Crie a variável de ambiente `MONGO_URI` com essa string. No Windows (PowerShell), uma linha por vez:

```powershell
$uri = Read-Host "Cole a connection string do Atlas"
[Environment]::SetEnvironmentVariable("MONGO_URI", $uri, "User")
Remove-Variable uri
```

5. Feche e abra o terminal (ou o VSCode) para a variável passar a valer.

Para criar/preparar as coleções no Atlas, faça backup dos dados existentes e execute o script a partir da raiz do projeto:

```powershell
mongosh "<URI_DO_ATLAS>" --file backend/scripts/configurar-atlas.js
```

Substitua `<URI_DO_ATLAS>` localmente pela URI do Atlas; não a salve no repositório nem a envie no chat. O script configura `usuarios`, `materiais` e `movimentacoes`, aplica validadores e cria índices. Coleções novas recebem validação estrita; coleções existentes ficam inicialmente em `moderate/warn` para não bloquear dados antigos. Revise os documentos e logs antes de mudar as coleções existentes para `strict/error`. A aplicação também cria os índices necessários ao iniciar.

### Executando o Sistema

1. Clone o repositório: <https://github.com/kayogcc/PI_IV-TIME-20-.git>

2. Teste a conexão com o banco (back-end):

```bash
cd backend
mvn test
```

O resultado esperado é `BUILD SUCCESS`. O teste grava e lê um documento no banco `estoquehosp_teste`, separado dos dados reais.

3. Na raiz do projeto, instale as dependências do front-end (só na primeira vez) e inicie o sistema:

```bash
npm ci
npm start
```

O `npm start` sobe a API (`http://localhost:8080/api`) e o front-end juntos, e o navegador abre na tela inicial. Para desligar tudo, aperte **Ctrl+C**.

<details>
<summary>Rodar a API e o front-end separados (dois terminais)</summary>

```bash
# Terminal 1 — API (para mudar a porta, defina a variável PORTA)
cd backend
mvn compile exec:java

# Terminal 2 — front-end, na raiz do projeto
npm run dev
```

O Vite repassa as chamadas para `/api` à API Java.
</details>

> Todas as telas usam a API: não há mais dados de exemplo no front-end.

### API REST

Todas as respostas são JSON. Erros vêm como `{ "mensagem": "..." }`. Exceto cadastro e login, as rotas exigem o header `Authorization: Bearer <token>`, com o token devolvido pelo login (vale 8 horas).

| Método | Rota | Quem pode | Descrição |
|---|---|---|---|
| POST | `/api/usuarios` | público | Cria conta: `{ nome, matricula, perfil, senha }`. 409 se a matrícula já existir |
| POST | `/api/login` | público | `{ matricula, senha }` → `{ token, usuario }` |
| POST | `/api/logout` | logado | Encerra a sessão |
| GET | `/api/eu` | logado | Usuário do token |
| GET | `/api/materiais?busca=` | logado | Lista materiais ativos (busca por nome ou código de barras) |
| GET | `/api/materiais/{id}` | logado | Detalhe do material, com saldo por local |
| POST | `/api/materiais` | Almoxarifado, Gestão | `{ nome, codigoBarras, lote, validade: "AAAA-MM-DD", quantidadeMinima }` |
| PUT | `/api/materiais/{id}` | Almoxarifado, Gestão | Edita (mesmo corpo do POST) |
| DELETE | `/api/materiais/{id}` | Almoxarifado, Gestão | Inativa o material (o histórico é mantido) |
| POST | `/api/movimentacoes` | Almoxarifado, Centro Cirúrgico (só saída) | `{ material, tipo, origem, quantidade, local }`. 409 se o saldo no local for insuficiente |
| GET | `/api/movimentacoes?dias=30&materialId=` | logado | Histórico, mais recentes primeiro |
| GET | `/api/alertas` | logado | Estoque baixo e validade próxima (até 30 dias), mais urgentes primeiro |
| GET | `/api/dashboard` | logado | Totais e entradas/saídas dos últimos 7 dias |

Valores aceitos: `perfil` = `ALMOXARIFADO`, `CENTRO_CIRURGICO` ou `GESTAO`; `tipo` = `Entrada` ou `Saída`; `origem` = `Código de barras` ou `Manual`; `local` = `Almoxarifado A`, `Almoxarifado B` ou `Centro Cirúrgico`. Em movimentações, `material` pode ser o código de barras ou o nome do material.

O código segue as camadas `controlador` → `servico` → `repositorio`, em `backend/src/main/java/br/puc/estoquehosp/`.

4. Após o login, o sistema libera as telas conforme o perfil:

    - **Almoxarifado** — cadastro de materiais e registro de entradas e saídas
    - **Centro Cirúrgico** — consulta de materiais e registro de saídas
    - **Gestão** — usuários, limites de alerta, dashboard e histórico
