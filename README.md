# PI_IV-TIME-20-
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
| Java 17+ | Linguagem de programação principal |
| Spring Boot | Framework da API REST em camadas |
| MongoDB | Banco de dados NoSQL orientado a documentos |
| Spring Data MongoDB | Integração entre Java e MongoDB |
| Spring Security | Autenticação e controle de acesso por perfil |
| Maven | Gerenciamento de dependências e build |
| React | Front-end web responsivo (leitura de código de barras pela câmera do celular) |
| JUnit | Testes unitários e de integração |
| Git + GitHub | Controle de versão e repositório |
| GitHub Projects | Gerenciamento de tarefas e apontamento de esforço |
| VSCode | IDE de desenvolvimento |

## Instruções para Execução do Sistema

### Pré-requisitos

- Java JDK 17 ou superior instalado
- Maven instalado
- MongoDB instalado e em execução (porta padrão `27017`)
- Node.js e npm instalados (front-end)

### Configuração do Banco de Dados

1. Inicie o MongoDB. O banco do projeto é criado no primeiro uso; para criá-lo manualmente no `mongosh`:

```javascript
use estoquehosp
```

2. Configure a conexão no arquivo `src/main/resources/application.properties`:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/estoquehosp
```

### Executando o Sistema

1. Clone o repositório: <https://github.com/kayogcc/PI_IV-TIME-20-.git>

2. Execute a API (back-end):

```bash
mvn spring-boot:run
```

3. Execute o front-end:

```bash
npm install
npm start
```

4. Após o login, o sistema libera as telas conforme o perfil:

    - **Almoxarifado** — cadastro de materiais e registro de entradas e saídas
    - **Centro Cirúrgico** — consulta de materiais e registro de saídas
    - **Gestão** — usuários, limites de alerta, dashboard e histórico


