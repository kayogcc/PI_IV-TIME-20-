// Tela: criar conta

import { criarAviso, matriculaValida } from '../utilitarios/formulario.js';

// Ajuste aqui conforme a API do back-end (Spring Boot)
const ENDPOINT_CADASTRO = '/api/usuarios';
const PAGINA_LOGIN = '/entrar.html';
const TAMANHO_MINIMO_SENHA = 8;
const TAMANHO_MAXIMO_SENHA = 72;
const TAMANHO_MAXIMO_NOME = 100;

// ⚠️ Protótipo: enquanto o back-end não existe, a conta é salva só neste navegador.
// Quando a API estiver pronta, troque para false.
const SEM_BACKEND = true;
const CHAVE_CONTAS = 'estoquehosp:contas';

// Texto do botão (como aparece na tela) -> valor enviado para a API + explicação do perfil
const PERFIS = {
  'Almoxarifado': {
    codigo: 'ALMOXARIFADO',
    dica: 'Almoxarifado cadastra materiais e registra entradas e saídas.',
  },
  'Centro Cirúrgico': {
    codigo: 'CENTRO_CIRURGICO',
    dica: 'Centro Cirúrgico consulta materiais e registra saídas.',
  },
  'Gestão': {
    codigo: 'GESTAO',
    dica: 'Gestão acessa usuários, limites de alerta, dashboard e histórico.',
  },
};

const form = document.getElementById('form-cadastro');
const campoNome = document.getElementById('nome');
const campoMatricula = document.getElementById('mat');
const campoSenha = document.getElementById('pw');
const campoConfirmacao = document.getElementById('pw2');
const botoesPerfil = document.querySelectorAll('#roles .role');
const dicaPerfil = form.querySelector('.dica');
const botaoCriar = document.getElementById('criar');

// Mensagem de erro (criada aqui para não precisar mexer no HTML)
const aviso = criarAviso(form, botaoCriar);
const mostrarErro = aviso.erro;
const limparErro = aviso.limpar;

function perfilSelecionado() {
  const botao = document.querySelector('#roles .role.on');
  return botao.textContent.trim();
}

function validar() {
  const nome = campoNome.value.trim();
  const matricula = campoMatricula.value.trim();

  if (nome.split(/\s+/).length < 2) {
    return { mensagem: 'Informe nome e sobrenome.', campo: campoNome };
  }
  // Só letras (com acento), espaço, apóstrofo e hífen — ex.: "Ana D'Ávila Souza-Lima"
  if (!/^[\p{L}' -]+$/u.test(nome)) {
    return { mensagem: 'O nome deve conter apenas letras.', campo: campoNome };
  }
  if (nome.length > TAMANHO_MAXIMO_NOME) {
    return { mensagem: `O nome pode ter no máximo ${TAMANHO_MAXIMO_NOME} caracteres.`, campo: campoNome };
  }
  if (!matriculaValida(matricula)) {
    return {
      mensagem: 'Usuário ou matrícula inválido: use de 3 a 30 letras, números, ponto, hífen ou sublinhado, sem espaços.',
      campo: campoMatricula,
    };
  }
  if (campoSenha.value.length < TAMANHO_MINIMO_SENHA) {
    return {
      mensagem: `A senha precisa ter pelo menos ${TAMANHO_MINIMO_SENHA} caracteres.`,
      campo: campoSenha,
    };
  }
  if (campoSenha.value.length > TAMANHO_MAXIMO_SENHA) {
    return { mensagem: `A senha pode ter no máximo ${TAMANHO_MAXIMO_SENHA} caracteres.`, campo: campoSenha };
  }
  if (!/[\p{L}]/u.test(campoSenha.value) || !/\d/.test(campoSenha.value)) {
    return { mensagem: 'A senha precisa ter pelo menos uma letra e um número.', campo: campoSenha };
  }
  if (campoSenha.value !== campoConfirmacao.value) {
    return { mensagem: 'As senhas não conferem.', campo: campoConfirmacao };
  }
  return null;
}

async function lerMensagemDaApi(resposta) {
  try {
    const dados = await resposta.json();
    return dados.message || dados.mensagem || null;
  } catch {
    return null;
  }
}

function aoEnviar(evento) {
  evento.preventDefault();
  limparErro();

  const problema = validar();
  if (problema) {
    mostrarErro(problema.mensagem, problema.campo);
    return;
  }
  enviarCadastro();
}

function lerContasLocais() {
  try {
    const contas = JSON.parse(localStorage.getItem(CHAVE_CONTAS));
    return Array.isArray(contas) ? contas : [];
  } catch {
    return [];
  }
}

// Salva a conta no localStorage (sem a senha). Devolve false se a matrícula já existir.
function salvarContaLocal(conta) {
  const contas = lerContasLocais();
  const matricula = conta.matricula.toLowerCase();
  if (contas.some((c) => c.matricula.toLowerCase() === matricula)) return false;
  contas.push(conta);
  localStorage.setItem(CHAVE_CONTAS, JSON.stringify(contas));
  return true;
}

async function enviarCadastro() {
  if (SEM_BACKEND) {
    const salvou = salvarContaLocal({
      nome: campoNome.value.trim().replace(/\s+/g, ' '),
      matricula: campoMatricula.value.trim(),
      perfil: PERFIS[perfilSelecionado()].codigo,
    });
    if (!salvou) {
      mostrarErro('Já existe uma conta com esse usuário ou matrícula.', campoMatricula);
      return;
    }
    window.location.href = PAGINA_LOGIN;
    return;
  }

  const textoOriginal = botaoCriar.textContent;
  botaoCriar.disabled = true;
  botaoCriar.textContent = 'Criando conta...';

  try {
    const resposta = await fetch(ENDPOINT_CADASTRO, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nome: campoNome.value.trim().replace(/\s+/g, ' '),
        matricula: campoMatricula.value.trim(),
        perfil: PERFIS[perfilSelecionado()].codigo,
        senha: campoSenha.value,
      }),
    });

    if (resposta.ok) {
      window.location.href = PAGINA_LOGIN;
      return;
    }

    if (resposta.status === 409) {
      mostrarErro('Já existe uma conta com esse usuário ou matrícula.', campoMatricula);
    } else {
      const mensagem = await lerMensagemDaApi(resposta);
      mostrarErro(mensagem || 'Não foi possível criar a conta. Tente novamente.');
    }
  } catch {
    mostrarErro('Não foi possível conectar ao servidor. Verifique a conexão e tente novamente.');
  }

  botaoCriar.disabled = false;
  botaoCriar.textContent = textoOriginal;
}

function escolherPerfil(botao) {
  botoesPerfil.forEach((b) => b.classList.toggle('on', b === botao));
  dicaPerfil.textContent = PERFIS[botao.textContent.trim()].dica;
}

botoesPerfil.forEach((botao) => botao.addEventListener('click', () => escolherPerfil(botao)));
form.addEventListener('submit', aoEnviar);
form.addEventListener('input', limparErro);
