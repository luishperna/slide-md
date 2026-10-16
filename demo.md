---
bg: linear-gradient(to bottom right, #090d16, #1e293b)
align: center
transition: morph

slide 2:
  bg: #0f172a
  align: left
---

# slide-md
### Apresentações fluidas e rápidas com Markdown e Java

Pressione **Espaço** ou **Seta Direita** para navegar

---

# Atalhos de Teclado

Navegação simples e sem distrações:

- **Seta Direita / Espaço / Page Down**: Próximo slide
- **Seta Esquerda / Page Up**: Slide anterior
- **M ou E**: Abrir/fechar **Editor Flutuante** de Markdown
- **Ctrl + O**: Abrir apresentação visualmente
- **F ou F11**: Alternar **Tela Cheia**
- **R**: Recarregar arquivo do disco
- **Esc**: Fechar editor ou sair da tela cheia

---
bg: #1e1e2e

# Modo Transformar (Morph) 🪄

Os elementos da tela entram e saem com fluidez cinematográfica:

- Cada título, texto e item de lista se move de forma escalonada
- Sensação idêntica ao recurso **Transformar do PowerPoint**
- Transição ativada globalmente com `transition: morph`!

---
bg: #042f2e
align: left

# Estilização com Markdown

> "A simplicidade é o último grau de sofisticação." — Leonardo da Vinci

Você pode usar:
- Texto em **negrito**, *itálico* ou `código inline`
- Listas ordenadas e marcadas
- Blocos de código destacados

```java
// Efeito Transformar ativo em 60 FPS com aceleração por GPU
transitionManager.transition(container, oldView, nextView, true, "morph", () -> {});
```

---
bg: #111827
align: center

# Suporte a Vídeo 100% Nativo em Java

Sem precisar instalar FFmpeg ou ferramentas externas:

- O JavaFX reproduz vídeos MP4/H.264 com aceleração por hardware
- **Corte virtual instantâneo (0ms)**:
  `cut: 00:10 - 00:45`
- Zero espera de processamento e zero gasto de espaço extra em disco!
