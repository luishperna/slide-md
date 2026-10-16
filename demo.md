---
bg: linear-gradient(to bottom right, #090d16, #1e293b)
align: center
transition: slide
---

# slide-md
### Apresentações fluidas e rápidas com Markdown e Java

Pressione **Espaço** ou **Seta Direita** para navegar

---
bg: #0f172a
align: left
transition: slide
---

# Atalhos de Teclado

Navegação simples e sem distrações:

- **Seta Direita / Espaço / Page Down**: Próximo slide
- **Seta Esquerda / Page Up**: Slide anterior
- **M ou E**: Abrir/fechar **Editor Flutuante** de Markdown
- **F ou F11**: Alternar **Tela Cheia**
- **R**: Recarregar arquivo do disco
- **Esc**: Fechar editor ou sair da tela cheia

---
bg: #1e1e2e
align: center
transition: fade
---

# Estilização com Markdown

> "A simplicidade é o último grau de sofisticação." — Leonardo da Vinci

Você pode usar:
- Texto em **negrito**, *itálico* ou `código inline`
- Listas ordenadas e marcadas
- Blocos de código destacados

```java
// Performance em 60 FPS com aceleração por hardware
SlideView slide = new SlideView(markdownContent);
slide.renderWithFluidTransitions();
```

---
bg: #042f2e
align: left
transition: zoom
---

# Suporte a Cores e Fundos

Você pode personalizar cada slide individualmente através do cabeçalho:

`bg: #042f2e`
`align: left`
`transition: zoom`

Também é possível usar gradientes CSS ou imagens de fundo:
`bg: url(./minha-imagem.jpg)`

---
bg: #111827
align: center
transition: slide
---

# Recursos de Vídeo & FFmpeg

Edite vídeos diretamente no Markdown sem esforço:

`video: ./meu-video.mp4`
`cut: 00:10 - 00:45`

O **slide-md** executa o corte instantâneo via FFmpeg sem reprocessar áudio/vídeo e armazena em cache local para máxima velocidade!
