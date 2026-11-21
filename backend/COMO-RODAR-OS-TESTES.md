<!-- Autores: Carlos Eduardo Marins Fonseca, Gabriel Figueira Albasini -->

# Como rodar os testes do back-end

Guia rápido para quem vai executar os testes do back-end. Para entender o que cada teste verifica, veja o `README-TESTES.md` (nesta mesma pasta).

## 1. Preparação (uma vez só)

**Atualizar o projeto:**
```bash
git checkout main
git pull
```

**Entrar na pasta do back-end** (onde está o `pom.xml`). Todos os comandos abaixo são executados aqui:
```bash
cd backend
```

**Conferir as versões:**
```bash
java -version
mvn -version
```
As duas precisam mostrar **Java 25**. Se o `mvn -version` mostrar outro Java, o erro será `release version 25 not supported`. Nesse caso, ajuste a variável `JAVA_HOME` para o JDK 25.

> A primeira execução demora mais porque o Maven baixa as bibliotecas. É preciso ter internet.

## 2. Escolha o cenário

### Cenário A — Sem banco (mais simples)
Roda todos os testes que não dependem do MongoDB. Os testes com banco, e também o grupo `Permissoes` (que sobe o servidor de sockets), aparecem como **ignorados (skipped)**, o que é normal.

```bash
mvn test
```

### Cenário B — Completo, com banco
Roda tudo, inclusive os testes que gravam no MongoDB. Precisa da `MONGO_URI` (a mesma usada pelo `ConexaoTest`). Os testes só mexem no banco `estoquehosp_teste` e **não tocam nos dados reais**.

PowerShell (Windows):
```powershell
$env:MONGO_URI="cole-a-uri-aqui"; mvn test
```

CMD (Windows):
```cmd
set MONGO_URI=cole-a-uri-aqui && mvn test
```

Linux / macOS:
```bash
MONGO_URI="cole-a-uri-aqui" mvn test
```

> Nunca coloque a `MONGO_URI` em arquivo que vá para o Git.

### Cenário C — Só o arquivo novo (`ServicosTest`)
```bash
mvn test -Dtest=ServicosTest*
```
Para incluir o banco, defina a `MONGO_URI` antes, como no Cenário B.

### Cenário D — Só um grupo
Os grupos são: `Materiais`, `Movimentacoes`, `Usuarios`, `Permissoes` e `Historico`. Exemplo com `Materiais`:

Linux / macOS / Git Bash:
```bash
mvn test -Dtest='ServicosTest$Materiais'
```

PowerShell:
```powershell
mvn test "-Dtest=ServicosTest`$Materiais"
```

CMD:
```cmd
mvn test -Dtest=ServicosTest$Materiais
```
Se esse formato der trabalho, use o Cenário C.

## 3. Como ler o resultado

No final aparece algo como:
```
Tests run: 90, Failures: 0, Errors: 0, Skipped: 40
BUILD SUCCESS
```

| Resultado | Significado |
|---|---|
| `BUILD SUCCESS` | Tudo que rodou passou |
| `Skipped` maior que 0 | Testes com banco ignorados por falta da `MONGO_URI` (normal no Cenário A) |
| `Failures` maior que 0 | Um teste falhou: o resultado do código foi diferente do esperado |
| `Errors` maior que 0 | O teste quebrou com exceção (inclui erro de conexão com o banco) |
| `BUILD FAILURE` com `COMPILATION ERROR` | Erro de compilação (mostra o arquivo e a linha) |

Os números acima são só um exemplo; os seus podem ser diferentes.

O relatório detalhado fica em `backend/target/surefire-reports/`.

## 4. Resultado esperado

Com a `MONGO_URI` definida (Cenário B), todos os testes devem passar: **119 testes, 0 falhas**. O teste `camposAusentes`, que antes apontava um bug de campos nulos no `MovimentacaoServico.java`, já passa, porque a correção está aplicada (veja o `README-TESTES.md`, seção 5).

## 5. Se algo falhar

Copie e mande ao responsável pelos testes:
1. O comando usado (sem a `MONGO_URI`).
2. A saída do `java -version` e do `mvn -version`.
3. As linhas do erro (a partir de `[ERROR]`) e o nome do teste que falhou.

## 6. Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `mvn` não é reconhecido | Maven não instalado ou fora do PATH | Instalar o Maven e abrir um terminal novo |
| `release version 25 not supported` | Maven usando um JDK mais antigo | Apontar `JAVA_HOME` para o JDK 25 |
| `No tests were executed` | Nome do teste digitado errado ou pasta errada | Conferir se está em `backend` e se o nome está correto |
| Testes com banco dão erro de conexão | `MONGO_URI` errada ou o IP não liberado no Atlas | Conferir a URI e a lista de IPs permitidos no Atlas |
| Muitos testes `Skipped` | `MONGO_URI` não definida no terminal atual | Normal no Cenário A; para rodar com banco, usar o Cenário B |
