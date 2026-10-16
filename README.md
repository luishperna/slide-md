# slide-md ⚡

> **Apresentações modernas, fluidas e rápidas em Java a partir de arquivos Markdown.**

O **slide-md** é um aplicativo desktop construído em **Java 21** e **JavaFX** com aceleração por hardware (60 FPS), projetado para criar e apresentar slides elegantes com o mínimo de atrito.

---

## ✨ Recursos

- 🚀 **Leve & Performático**: Executa nativamente em Java 21 com renderização fluida e aceleração por GPU.
- 🎨 **Estética Minimalista**: Interface limpa, moderna, sem poluição visual. Os controles (HUD) se ocultam automaticamente quando o mouse fica parado.
- 🪟 **Janela Flutuante de Markdown (Live Preview)**: Pressione a tecla `M` ou clique em `MD` para abrir um editor interno translúcido e ver suas alterações refletidas nos slides em tempo real!
- 🎬 **Edição de Vídeo Integrada (FFmpeg)**:
  - Reprodução nativa de vídeos MP4 nos slides.
  - **Corte instantâneo** (`cut: 00:05 - 00:30`): corta trechos de vídeo sem re-encoding (em milissegundos) e com cache automático.
  - **Junção de clipes** (`join: video1.mp4, video2.mp4`).
- ⌨️ **Navegação Intuitiva por Teclado**: Passagem de slides como se fosse um reprodutor de vídeo interativo.
- 🪄 **Transições Fluidas**: Suporte a animações suaves de `slide`, `fade` e `zoom` com curvas de interpolação cinematográficas.

---

## ⌨️ Atalhos de Teclado

| Tecla | Ação |
| :--- | :--- |
| <kbd>→</kbd> / <kbd>Espaço</kbd> / <kbd>Page Down</kbd> / <kbd>Enter</kbd> | Próximo slide |
| <kbd>←</kbd> / <kbd>Page Up</kbd> / <kbd>Backspace</kbd> | Slide anterior |
| <kbd>Home</kbd> / <kbd>End</kbd> | Primeiro / Último slide |
| <kbd>M</kbd> ou <kbd>E</kbd> | Abrir / Fechar o Editor Flutuante de Markdown |
| <kbd>F</kbd> ou <kbd>F11</kbd> | Alternar Tela Cheia (Fullscreen) |
| <kbd>R</kbd> | Recarregar arquivo do disco |
| <kbd>Esc</kbd> | Fechar editor flutuante ou sair de tela cheia |

---

## 📝 Sintaxe Markdown do Slide

Cada slide é separado por `---`. No topo de cada slide, você pode definir diretivas de estilo e mídia:

```markdown
---
bg: linear-gradient(to bottom right, #090d16, #1e293b)
align: center
transition: slide
---

# Meu Primeiro Slide
Subtítulo elegante e minimalista.

---
bg: #0f172a
align: left
---

# Slide com Lista e Código

- Ponto 1 com **destaque**
- Ponto 2 com `código inline`

```java
System.out.println("Hello, slide-md!");
```

---
bg: #111827
align: center
video: ./assets/demo.mp4
cut: 00:10 - 00:35
loop: true
---

# Slide com Vídeo Cortado
O trecho dos 10s aos 35s será cortado automaticamente via FFmpeg e reproduzido aqui.
```

### Diretivas Disponíveis:
- `bg:` Cor hexadecimal (`#0f172a`), gradiente CSS (`linear-gradient(...)`) ou caminho/URL de imagem.
- `align:` `center`, `left` ou `right`.
- `transition:` `slide` (deslizamento), `fade` (dissolvência) ou `zoom` (escala suave).
- `video:` Caminho para arquivo de vídeo `.mp4`.
- `cut:` Intervalo de corte (ex.: `00:05 - 00:25` ou `5 - 25`).
- `join:` Lista de vídeos separados por vírgula para junção instantânea.
- `loop:` `true` ou `false` (reprodução contínua do vídeo).

---

## 🚀 Como Executar

### Pré-requisitos
- **Java 21** LTS instalado.
- **FFmpeg** instalado (opcional, para cortes e junções automáticas de vídeos).

### Executar em Desenvolvimento:
```powershell
./mvnw javafx:run
```

Ou especificando um arquivo Markdown customizado:
```powershell
./mvnw javafx:run -Djavafx.args="meu-arquivo.md"
```

### Gerar Executável (Fat JAR):
```powershell
./mvnw package -DskipTests
```
O arquivo standalone gerado ficará em `target/slide-md-1.0.0-SNAPSHOT.jar` (~10MB, contendo todas as dependências e o JavaFX nativo).

Para executar diretamente:
```powershell
java -jar target/slide-md-1.0.0-SNAPSHOT.jar [caminho/apresentacao.md]
```
Você também pode associar a extensão `.md` ao executável para abrir suas apresentações com duplo clique!
