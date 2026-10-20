// Tela: entrar e cadastrar na mesma página (troca de formulário)

const formLogin = document.getElementById('form-login');
const formCadastro = document.getElementById('form-cadastro');
const msg = document.getElementById('msg');

function mostrarModo(modo) {
  const cadastro = modo === 'cadastro';
  formLogin.hidden = cadastro;
  formCadastro.hidden = !cadastro;
  msg.textContent = '';
  msg.className = 'msg';
  document.title = (cadastro ? 'Cadastrar' : 'Entrar') + ' — EstoqueHosp';
}

// Botões "Cadastrar" / "Entrar" que trocam o formulário
document.querySelectorAll('[data-modo]').forEach((botao) => {
  botao.addEventListener('click', () => mostrarModo(botao.dataset.modo));
});

// Seleção do perfil de acesso (nos dois formulários)
document.querySelectorAll('.roles').forEach((grupo) => {
  const botoes = grupo.querySelectorAll('.role');
  botoes.forEach((botao) => {
    botao.addEventListener('click', () => {
      botoes.forEach((b) => b.classList.remove('on'));
      botao.classList.add('on');
    });
  });
});

function mostrarMsg(texto, erro) {
  msg.textContent = texto;
  msg.className = 'msg ' + (erro ? 'erro' : 'ok');
}

formCadastro.addEventListener('submit', (e) => {
  e.preventDefault();

  const senha = document.getElementById('pw-cad').value;
  const confirma = document.getElementById('pw2').value;

  if (senha.length < 6) {
    mostrarMsg('A senha precisa ter pelo menos 6 caracteres.', true);
    return;
  }
  if (senha !== confirma) {
    mostrarMsg('As senhas não conferem. Digite a mesma senha nos dois campos.', true);
    return;
  }

  const dados = {
    perfil: formCadastro.querySelector('.role.on').textContent,
    nome: document.getElementById('nome').value.trim(),
    matricula: document.getElementById('mat-cad').value.trim(),
    email: document.getElementById('email').value.trim(),
    senha,
  };

  // TODO: enviar para a API (ex.: POST /api/usuarios) com `dados`
  console.log('Cadastro (demonstração):', { ...dados, senha: '***' });

  mostrarMsg('Conta criada! Faça login para continuar.', false);
  setTimeout(() => mostrarModo('login'), 1200);
});

// Abre direto no cadastro quando vier de inicio.html?modo=cadastro
if (new URLSearchParams(window.location.search).get('modo') === 'cadastro') {
  mostrarModo('cadastro');
}