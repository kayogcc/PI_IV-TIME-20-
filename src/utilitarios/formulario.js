// Mensagem de erro/sucesso de formulário, inserida antes de `referencia`.
// Devolve { erro, sucesso, limpar } para usar na validação.
export function criarAviso(form, referencia) {
  const aviso = document.createElement('p')
  aviso.setAttribute('role', 'alert')
  aviso.hidden = true
  referencia.parentNode.insertBefore(aviso, referencia)

  function mostrar(classe, mensagem) {
    aviso.className = classe
    aviso.textContent = mensagem
    aviso.hidden = false
  }

  return {
    erro(mensagem, campo) {
      mostrar('erro', mensagem)
      if (campo) {
        campo.setAttribute('aria-invalid', 'true')
        campo.focus()
      }
    },
    sucesso(mensagem) {
      mostrar('sucesso', mensagem)
    },
    limpar() {
      aviso.hidden = true
      aviso.textContent = ''
      form.querySelectorAll('[aria-invalid]').forEach((campo) => campo.removeAttribute('aria-invalid'))
    }
  }
}

// Usuário ou matrícula: obrigatório, sem espaços, só letras, números, ponto, hífen e sublinhado.
export function matriculaValida(valor) {
  return /^[\p{L}\d._-]{3,30}$/u.test(valor)
}
