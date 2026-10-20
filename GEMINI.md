# Guia do Projeto: slide-md

Documento de referência para desenvolvedores e agentes de IA que atuam na evolução, manutenção e empacotamento do **slide-md**.

---

## 🎯 Visão e Objetivos do Projeto

O **slide-md** é um aplicativo desktop open-source desenvolvido em **Java 21** e **JavaFX 21**, projetado para ler arquivos Markdown e renderizá-los como apresentações de slides modernas, minimalistas e com animações fluidas a 60 FPS.

### Princípios Fundamentais:
1. **Performance & Fluidez**: Inicialização rápida, resposta instantânea aos comandos do teclado e transições cinematográficas suaves aceleradas por hardware (GPU).
2. **Minimalismo Visual (Distraction-Free)**: Foco total no conteúdo. Elementos da interface (HUD) se ocultam automaticamente quando o mouse fica parado.
3. **Live Preview Interno**: Editor de Markdown flutuante arrastável na própria janela do aplicativo para ajustes em tempo real sem precisar de editor externo.
4. **Zero Dependências Obrigatórias no Cliente**: O aplicativo deve funcionar "out-of-the-box" em qualquer máquina (Windows, Linux, macOS) sem exigir ferramentas externas como FFmpeg para reprodução ou cortes de vídeo.
5. **Distribuição Nativa**: Compatibilidade com `jpackage` para gerar instaladores (.msi, .deb, .dmg) e executáveis portáteis com associação de arquivo no sistema operacional ("Abrir com...").

---

## 🛠️ Stack Tecnológica

| Componente | Tecnologia | Papel |
| :--- | :--- | :--- |
| **Linguagem** | Java 21 LTS | Pattern matching, records, switches expressivos e performance moderna |
| **Interface Gráfica** | JavaFX 21.0.5 | Renderização com aceleração gráfica nativa por hardware (DirectX/Metal/OpenGL) |
| **Áudio & Vídeo** | JavaFX Media (`MediaPlayer` / `MediaView`) | Reprodução MP4/H.264 nativa com suporte a cortes virtuais instantâneos (`setStartTime` / `setStopTime`) |
| **Parser Markdown** | CommonMark 0.24.0 | Parsing rápido da árvore sintática (AST) de Markdown puro e sem dependências pesadas |
| **Build & Wrapper** | Apache Maven 3.9.9 (`mvnw`) | Gerenciamento de dependências, testes e compilação multiplataforma |
| **Empacotamento** | Maven Shade Plugin + `jpackage` | Fat JAR standalone de ~10MB e geração de executáveis nativos por plataforma |
| **Testes** | JUnit 5 (Jupiter) | Testes unitários para parsers, diretivas e serviços |

---

## 📂 Estrutura do Repositório

```
slide-md/
├── .mvn/wrapper/               # Maven Wrapper portátil
├── assets/                     # Mídias de apresentação e demonstração oficial
│   ├── demo-preview.png        # Poster / screenshot em alta resolução
│   └── slide-md-demo.mp4       # Vídeo de demonstração do aplicativo
├── package/
│   └── windows/
│       └── file-associations.properties  # Registro de extensão (.md) para instaladores
├── src/
│   ├── main/java/com/slidemd/
│   │   ├── SlideMdApp.java     # Entrypoint JavaFX, ciclo de vida, atalhos globais, Drag & Drop
│   │   ├── MainLauncher.java   # Wrapper main para compatibilidade com Shaded Fat JAR
│   │   ├── model/
│   │   │   ├── Alignment.java  # Enum de alinhamento (LEFT, CENTER, RIGHT)
│   │   │   ├── Slide.java      # Modelo de dados de um slide individual
│   │   │   ├── SlideDeck.java  # Coleção de slides da apresentação
│   │   │   └── VideoSpec.java  # Especificação de vídeo (corte, junção, loop, autoplay)
│   │   ├── parser/
│   │   │   └── MarkdownSlideParser.java  # Divisão por '---', extração de frontmatter/diretivas
│   │   ├── animation/
│   │   │   └── TransitionManager.java    # Transições animadas fluidas (slide, fade, zoom)
│   │   ├── ui/
│   │   │   ├── AppTitleBar.java          # Barra de título e controle integrada (modo dock e navegação)
│   │   │   ├── LaserPenOverlay.java      # Camada acelerada de rastro/caneta laser translúcida
│   │   │   ├── SlideView.java            # Canvas visual do slide (fundo, vídeo, Markdown)
│   │   │   ├── MarkdownNodeRenderer.java # Mapeia nós CommonMark para nós estilizados JavaFX
│   │   │   ├── SideEditorPanel.java      # Painel lateral de edição integrado (estilo VS Code)
│   │   │   └── HUDOverlay.java           # Barra de controle flutuante com auto-hide
│   │   └── video/
│   │       ├── FFmpegLocator.java        # Localizador dinâmico de FFmpeg em múltiplos SOs
│   │       └── FFmpegService.java        # Processador de vídeo (cortes/junções em disco)
│   └── test/java/com/slidemd/
│       ├── parser/MarkdownSlideParserTest.java # Testes de parsing e diretivas
├── examples/
│   ├── demo.md                 # Apresentação de demonstração (todas as diretivas e atalhos)
│   └── tesla-roadster/         # Keynote comercial minimalista (com mídia)
│       ├── tesla-roadster.md
│       └── README.md
├── pom.xml                     # Configurações do Maven e dependências
├── LICENSE                     # Licença MIT
└── README.md                   # Documentação do usuário final
```

---

## 📜 Padrão de Sintaxe do Markdown (`slide-md`)

Slides são delimitados por `---`. No topo de cada slide (ou no frontmatter global inicial), podem ser definidas diretivas diretas (`chave: valor`) ou comentários HTML (`<!-- chave: valor -->`):

```markdown
---
background: [#090d16, #1e293b, 45%]
alignment: center
transition: morph
---

# Título Principal
Subtítulo elegante

---
background: [#0f172a, #1e293b, 50%]
alignment: left
video: ./assets/demo.mp4 [00:05-00:25]
---

# Slide com Vídeo
- O corte funciona inline entre colchetes: `[00:05-00:25]`
- Múltiplos vídeos concatenados são separados por vírgula: `video: v1.mp4, v2.mp4`
- O vídeo entra pausado e toca ao dar mais um clique para frente
```

### Diretivas Suportadas (5 Diretrizes Oficiais Limpas):
- `background`:
  - **Gradiente em colchetes**: `[#1e293b, #segunda_cor, #terceira_cor, porcentagem_da_direcao]` (ex.: `[#1e293b, #0f172a, 45%]`, `[#1e293b, #0f172a, 50%]`, `[#1e293b, #0f172a, to right]` ou simplesmente `[#1e293b, #0f172a]`).
  - Cor hexadecimal sólida (`#1e293b`) ou imagem (`url(foto.jpg)` ou `./foto.jpg`).
- `alignment`: `center`, `left`, `right`, `top`, `down` (aceita também `bottom` e `up`).
- `transition`: `morph` (scroll contínuo e orgânico onde os slides e elementos deslizam conectados), `slide` (deslizamento clássico), `fade` (crossfade), `zoom` (zoom sutil).
- `video`: Caminho do arquivo de vídeo `.mp4`.
  - **Corte inline**: adicione o intervalo entre colchetes na frente do arquivo (ex.: `video: demo.mp4 [00:10-00:45]`).
  - **Junção**: separe múltiplos vídeos por vírgula na mesma linha (ex.: `video: v1.mp4, v2.mp4`).
  - **Reprodução**: O vídeo não toca sozinho (autoplay sempre falso). O primeiro clique para frente inicia o vídeo; o próximo clique avança para o slide seguinte.
  - **Ciclo**: Toca uma vez até o fim (loop sempre falso).
- `image`: Caminho relativo ou URL da imagem do slide ou inline (`image: ./assets/foto.jpg`).
- `color`: Cor temática principal para títulos e marcadores de tópicos (`#ef4444`, `#38bdf8`, `red`). Conforme a quantidade de `#` aumenta (H2, H3...), a cor clareia suavemente em direção ao branco/tom pastel. O ponto do tópico (`•`) recebe a cor base.
- `sound`: Caminho relativo, absoluto ou URL para arquivo de áudio de fundo (`.mp3`, `.wav`, `.aac`). Loop sempre falso e volume padrão normalizado em 1.0.
- **Comentários HTML**: `<!-- background: [#1e293b, #0f172a] -->` continuam suportados para configurar o slide sem aparecer na apresentação.

---

## ⌨️ Atalhos de Teclado Globais

| Atalho | Ação |
| :--- | :--- |
| <kbd>→</kbd> / <kbd>Espaço</kbd> / <kbd>Page Down</kbd> / <kbd>Enter</kbd> | Próximo slide |
| <kbd>←</kbd> / <kbd>Page Up</kbd> / <kbd>Backspace</kbd> | Slide anterior |
| <kbd>Home</kbd> / <kbd>End</kbd> | Primeiro / Último slide |
| <kbd>Ctrl</kbd> + <kbd>O</kbd> (<kbd>Cmd</kbd> + <kbd>O</kbd> no Mac) | Abrir arquivo Markdown visualmente |
| <kbd>M</kbd> ou <kbd>E</kbd> | Alternar Painel Lateral do Editor |
| <kbd>P</kbd> | Alternar Modo Caneta / Rastro Laser |
| <kbd>C</kbd> | Limpar traços da caneta |
| <kbd>F</kbd> ou <kbd>F11</kbd> | Alternar Tela Cheia |
| <kbd>R</kbd> | Recarregar arquivo do disco |
| <kbd>U</kbd> | Silenciar / Reativar Áudio (Mute/Unmute) |
| <kbd>Esc</kbd> | Desativar caneta, fechar painel lateral ou sair da tela cheia |

---

## 🚀 Comandos de Desenvolvimento e Build

Todos os comandos devem ser executados a partir da raiz do projeto:

```powershell
# 1. Executar testes automatizados
.\mvnw.cmd test

# 2. Executar aplicação em modo de desenvolvimento
.\mvnw.cmd javafx:run

# 3. Executar abrindo um arquivo específico
.\mvnw.cmd javafx:run -Djavafx.args="examples/demo.md"

# 4. Executar com música de fundo contínua (flag --music)
.\mvnw.cmd javafx:run -Djavafx.args="examples/demo.md --music ./audio/theme.mp3"

# 5. Gerar Fat JAR standalone (~10MB executável)
.\mvnw.cmd package -DskipTests

# 6. Executar o Fat JAR com música de fundo
java -jar target\slide-md-1.0.0-SNAPSHOT.jar examples/demo.md --music soundtrack.mp3

# 7. Gerar pacote nativo do Windows com JRE embutido (jpackage)
jpackage --type app-image --dest dist --name slide-md --input target --main-jar slide-md-1.0.0-SNAPSHOT.jar --main-class com.slidemd.MainLauncher
```

---

## 🛡️ Diretrizes para Contribuições e Agentes de IA

1. **Nunca utilize caminhos absolutos ou específicos de uma máquina**:
   - Sempre utilize caminhos relativos ou faça uso do `FFmpegLocator` e diretórios do usuário (`~/.slide-md`).
2. **Preserve a independência de dependências externas no cliente**:
   - Recursos multimídia devem ter sempre fallback para funcionalidades nativas do JavaFX (ex.: cortes virtuais via `setStartTime`/`setStopTime`).
3. **Mantenha a UI responsiva**:
   - Qualquer I/O pesado (leitura de arquivos grandes, chamadas de processo externo, parsing assíncrono) deve ser despachado fora da Thread JavaFX Application (`Platform.runLater` para atualizar nós visuais).
4. **Respeite o Design Minimalista**:
   - Evite adicionar barras de ferramentas pesadas ou excesso de botões na tela principal. O HUD deve permanecer elegante, auto-ocultável e focado.
5. **Sempre verifique via testes**:
   - Ao modificar a lógica de parsing ou manipulação de slides, adicione os testes correspondentes em `src/test/java/com/slidemd/`.
