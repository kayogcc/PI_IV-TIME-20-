// Tela: criar conta

// Ajuste aqui conforme a API do back-end (Spring Boot)
const ENDPOINT_CADASTRO = '/api/usuarios';
const PAGINA_LOGIN = '/entrar.html';
const TAMANHO_MINIMO_SENHA = 8;

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
const erro = document.createElement('p');
erro.className = 'erro';
erro.setAttribute('role', 'alert');
erro.hidden = true;
form.insertBefore(erro, botaoCriar);

function perfilSelecionado() {
  const botao = document.querySelector('#roles .role.on');
  return botao.textContent.trim();
}

function mostrarErro(mensagem, campo) {
  erro.textContent = mensagem;
  erro.hidden = false;
  if (campo) {
    campo.setAttribute('aria-invalid', 'true');
    campo.focus();
  }
}

function limparErro() {
  erro.hidden = true;
  erro.textContent = '';
  form.querySelectorAll('[aria-invalid]').forEach((campo) => campo.removeAttribute('aria-invalid'));
}

function validar() {
  const nome = campoNome.value.trim();
  const matricula = campoMatricula.value.trim();

  if (nome.split(/\s+/).length < 2) {
    return { mensagem: 'Informe nome e sobrenome.', campo: campoNome };
  }
  if (!matricula || /\s/.test(matricula)) {
    return { mensagem: 'Informe seu usuário ou matrícula, sem espaços.', campo: campoMatricula };
  }
  if (campoSenha.value.length < TAMANHO_MINIMO_SENHA) {
    return {
      mensagem: `A senha precisa ter pelo menos ${TAMANHO_MINIMO_SENHA} caracteres.`,
      campo: campoSenha,
    };
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

async function enviarCadastro() {
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
