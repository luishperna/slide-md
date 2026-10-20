# slide-md ⚡

<p align="center">
  <img src="./assets/roadster-2026-exterior.jpg" alt="slide-md preview" width="800" style="border-radius: 10px; box-shadow: 0 10px 30px rgba(0,0,0,0.5);" />
</p>

<p align="center">
  <strong>Apresentador de slides desktop moderno, minimalista e fluido a 60 FPS direto do seu Markdown.</strong>
</p>

<p align="center">
  <a href="https://github.com/luishperna/slide-md"><img src="https://img.shields.io/badge/Status-v0.1.0--alpha-yellow?style=for-the-badge" alt="Status Alpha" /></a>
  <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" /></a>
  <a href="https://openjfx.io/"><img src="https://img.shields.io/badge/JavaFX-21.0.5-FF6500?style=for-the-badge&logo=openjdk&logoColor=white" alt="JavaFX 21" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" alt="License MIT" /></a>
  <a href="#-contribuindo"><img src="https://img.shields.io/badge/PRs-Welcome-brightgreen?style=for-the-badge" alt="PRs Welcome" /></a>
</p>

---

> [!IMPORTANT]
> **Aviso de Versão Alpha (v0.1.0-alpha)**:  
> O **slide-md** está atualmente em estágio **Alpha de desenvolvimento aberto**.  
> O instalador executável direto (`.exe` / `.msi`) para usuários finais será disponibilizado para download nas próximas releases após a conclusão dos testes de estabilidade e refinamento de UX.  
> **No momento, o aplicativo pode ser executado, testado e evoluído por desenvolvedores e entusiastas clonando este repositório e rodando via Maven Wrapper ou gerando o Fat JAR.**

---

## 💡 Sobre o Projeto

O **slide-md** é um aplicativo desktop open-source desenvolvido em **Java 21** e **JavaFX 21**, projetado para ler arquivos Markdown e renderizá-los instantaneamente como apresentações visuais de alto impacto, com aceleração por hardware (GPU) e foco absoluto no conteúdo.

Diferente de ferramentas web pesadas ou suítes de escritório tradicionais, o **slide-md** oferece:
- **Distraction-Free**: Interface limpa e minimalista. As barras de controle e menus se ocultam automaticamente quando o mouse fica parado.
- **Transições Cinematográficas**: Animação fluida a 60 FPS com interpolações suaves (`morph`, `slide`, `fade`, `zoom`).
- **Live Preview Integrado**: Editor lateral embutido estilo VS Code com realce de sintaxe em tempo real, autocompletar e paleta de cores inteligente.
- **Multimídia Nativa Sem Fricção**: Suporte a vídeos `.mp4` em qualquer parte do slide (inclusive abaixo de títulos), com corte virtual instantâneo (`[00:10-00:30]`) e junção de múltiplos clipes.
- **Ferramentas de Apresentação**: Caneta laser com rastro luminoso suave dissipativo e atalhos globais intuitivos.

---

## ✨ Recursos em Destaque

| Recurso | Descrição |
| :--- | :--- |
| ⚡ **GPU Acelerada (60 FPS)** | Renderização gráfica nativa por hardware (DirectX, Metal, OpenGL) sem engasgos. |
| 📝 **Editor Lateral Embutido** | Pressione <kbd>M</kbd> ou <kbd>E</kbd> para abrir o painel lateral com highlight sintático, contagem de slides e autocompletar inteligente. |
| 🎨 **Paleta de Cores Flyout** | Seletor de cores flutuante posicionado na lateral do editor que fecha no primeiro clique fora sem bloquear a digitação. |
| 🎬 **Vídeo Inline com Corte** | Posicione vídeos em qualquer ponto do Markdown com cortes inline instantâneos (`video: demo.mp4 [00:05-00:25]`). |
| 🖊️ **Caneta / Rastro Laser** | Pressione <kbd>P</kbd> para ativar o apontador laser com rastro luminoso e dissipação suave em tempo real. Pressione <kbd>C</kbd> para limpar. |
| 🎵 **Música de Fundo & Mute** | Áudio de fundo contínuo sincronizado e atalho <kbd>U</kbd> para silenciar/reativar em qualquer momento. |
| 🔄 **Transição Morph Conectada** | Modo de scroll contínuo e orgânico onde títulos e elementos deslizam conectados entre slides. |
| 📦 **Zero Dependências no Cliente** | Utiliza o motor multimídia do próprio JavaFX com fallback transparente para cortes virtuais. |

---

## ⌨️ Atalhos de Teclado Globais

| Tecla / Atalho | Ação |
| :--- | :--- |
| <kbd>→</kbd> / <kbd>Espaço</kbd> / <kbd>Page Down</kbd> / <kbd>Enter</kbd> | Avançar para o próximo slide (ou iniciar vídeo pausado) |
| <kbd>←</kbd> / <kbd>Page Up</kbd> / <kbd>Backspace</kbd> | Retornar para o slide anterior |
| <kbd>Home</kbd> / <kbd>End</kbd> | Ir para o Primeiro / Último slide |
| <kbd>Ctrl</kbd> + <kbd>O</kbd> | Abrir arquivo Markdown visualmente |
| <kbd>Ctrl</kbd> + <kbd>S</kbd> | Salvar alterações do editor no disco |
| <kbd>Ctrl</kbd> + <kbd>N</kbd> | Criar uma nova apresentação a partir do modelo |
| <kbd>M</kbd> ou <kbd>E</kbd> | Abrir / Fechar Painel Lateral de Edição |
| <kbd>P</kbd> | Alternar Modo Caneta Laser |
| <kbd>C</kbd> | Limpar traços da caneta laser |
| <kbd>F</kbd> ou <kbd>F11</kbd> | Alternar Modo Tela Cheia (Fullscreen) |
| <kbd>R</kbd> | Recarregar arquivo Markdown do disco |
| <kbd>U</kbd> | Silenciar / Reativar Áudio de Fundo (Mute/Unmute) |
| <kbd>Esc</kbd> | Desativar caneta, fechar editor ou sair da tela cheia |

---

## 📜 Padrão de Sintaxe do Markdown (`slide-md`)

Os slides são delimitados por `---`. No topo de cada slide (ou no frontmatter inicial), podem ser definidas diretivas diretas ou comentários HTML:

```markdown
---
background: [#0a0c10, #161922, 45%]
color: #38bdf8
alignment: left
transition: morph
---

# Tesla Roadster
### O supercarro elétrico projetado para máxima eficiência aerodinâmica

image: ./assets/roadster-2026-exterior.jpg

- **0 a 100 km/h**: 1,9 segundos
- **Velocidade Máxima**: Superior a 400 km/h
- **Autonomia Elétrica**: 1.000 km com uma única carga

---
background: #0d1117
alignment: center

# Demonstração em Alta Velocidade
### Assista ao teste de telemetria em pista aberta

video: ./assets/roadster-video.mp4 [00:30-00:41]

*Clique para reproduzir o clipe*
```

### Diretivas Oficiais Suportadas:
- `background`:
  - **Gradiente em colchetes**: `[#1e293b, #0f172a, 45%]` ou `[#1e293b, #0f172a]`
  - **Cor sólida**: `#0f172a` ou `rgb(15, 23, 42)`
  - **Imagem de fundo**: `url(./assets/foto.jpg)` ou `./assets/foto.jpg`
- `alignment`: `left`, `center`, `right`, `top` ou `down`
- `transition`: `morph` (scroll contínuo conectado), `slide` (deslizamento clássico), `fade` (crossfade suave) ou `zoom`
- `video`: Caminho do arquivo de vídeo `.mp4`. Pode ser inserido no topo ou no corpo do slide:
  - **Corte inline**: adicione o intervalo entre colchetes (ex.: `video: demo.mp4 [00:10-00:30]`)
  - **Junção**: separe múltiplos arquivos por vírgula (ex.: `video: v1.mp4, v2.mp4`)
  - **Reprodução**: O vídeo entra pausado no primeiro frame; o clique para frente inicia a reprodução e o próximo clique avança o slide
- `image`: Caminho da imagem em destaque ou inline (`image: ./assets/diagrama.png`)
- `color`: Cor temática principal para títulos e marcadores de tópicos (`#ef4444`, `#38bdf8`, etc.). Clareia harmoniosamente conforme o nível de `#` (H2, H3...) aumenta
- `sound`: Caminho ou URL de áudio/música de fundo (`.mp3`, `.wav`, `.aac`)

---

## 🛠️ Stack Tecnológica

| Componente | Tecnologia | Finalidade |
| :--- | :--- | :--- |
| **Linguagem** | Java 21 LTS | Pattern matching, records, switches expressivos e JVM moderna |
| **Interface Gráfica** | JavaFX 21.0.5 | Aceleração gráfica por GPU (DirectX, Metal, OpenGL) |
| **Editor de Código** | RichTextFX 0.11.7 | Edição com destaque de sintaxe e autocomplete de baixa latência |
| **Parser Markdown** | CommonMark 0.24.0 | Processamento rápido da Árvore Sintática (AST) padrão CommonMark |
| **Áudio & Vídeo** | JavaFX Media (`MediaPlayer` / `MediaView`) | Reprodução MP4/H.264 nativa com suporte a cortes virtuais |
| **Multimídia Externa** | FFmpeg (Opcional) | Localização dinâmica via `FFmpegLocator` para cortes e junções em disco |
| **Build & Packaging**| Apache Maven 3.9 + `jpackage` | Compilação portátil via `mvnw`, Fat JAR standalone (~10MB) |
| **Testes** | JUnit 5 Jupiter | Suíte com mais de 60 testes automatizados para parsers e serviços |

---

## 📂 Estrutura do Repositório

```
slide-md/
├── .mvn/wrapper/                   # Maven Wrapper multiplataforma portátil
├── examples/                       # Apresentações completas de demonstração
│   ├── README.md                   # Catálogo de apresentações de exemplo
│   ├── demo.md                     # Demonstração oficial completa de recursos e atalhos
│   └── tesla-roadster/             # Keynote de hipercarro elétrico (com mídia de alta resolução)
│       ├── tesla-roadster.md       # Slides formatados (Keynote comercial minimalista)
│       ├── README.md               # Fontes apuradas (Reuters, CNN, Tesla) e notas técnicas
│       └── assets/                 # Fotos de estúdio e clipe de vídeo integrado
├── package/
│   └── windows/
│       └── file-associations.properties  # Registro de extensão .md no SO
├── src/
│   ├── main/
│   │   ├── java/com/slidemd/
│   │   │   ├── SlideMdApp.java           # Entrypoint JavaFX, menus, atalhos globais e SplitPane
│   │   │   ├── MainLauncher.java         # Launcher compatível com Shaded Fat JAR
│   │   │   ├── animation/
│   │   │   │   └── TransitionManager.java# Transições fluidas (morph, slide, fade, zoom)
│   │   │   ├── audio/
│   │   │   │   └── AudioManager.java     # Gerenciamento de trilha sonora de fundo e mute
│   │   │   ├── model/
│   │   │   │   ├── Alignment.java        # Enum de alinhamento
│   │   │   │   ├── Slide.java            # Modelo de dados do slide individual
│   │   │   │   ├── SlideDeck.java        # Coleção de slides da apresentação
│   │   │   │   └── VideoSpec.java        # Especificação de corte, junção e vídeo
│   │   │   ├── parser/
│   │   │   │   └── MarkdownSlideParser.java # Divisão por '---', frontmatter e diretivas
│   │   │   ├── ui/
│   │   │   │   ├── AppTitleBar.java      # Barra de título e controle integrada
│   │   │   │   ├── DirectiveAutocompletePopup.java # Autocomplete inteligente de diretivas
│   │   │   │   ├── HUDOverlay.java       # HUD flutuante auto-hide
│   │   │   │   ├── LaserPenOverlay.java  # Rastro laser acelerado por GPU
│   │   │   │   ├── MarkdownNodeRenderer.java # Mapeamento AST CommonMark -> Nós JavaFX
│   │   │   │   ├── SideEditorPanel.java  # Painel lateral integrado estilo VS Code
│   │   │   │   ├── SlideColorPickerPopup.java # Seletor de cores flutuante lateral
│   │   │   │   └── SlideView.java        # Canvas visual de renderização do slide
│   │   │   └── video/
│   │   │       ├── FFmpegLocator.java    # Localizador dinâmico de FFmpeg multiplataforma
│   │   │       └── FFmpegService.java    # Processamento de cortes e junções em cache
│   │   └── resources/
│   │       └── css/                      # Folhas de estilo do editor e scrollbars
│   └── test/java/com/slidemd/            # 60+ Testes unitários JUnit 5
├── pom.xml                         # Configuração Maven e dependências
├── LICENSE                         # Licença livre MIT
└── README.md                       # Documentação do projeto
```

---

## 🚀 Como Executar (Guia para Devs & Usuários)

### Pré-requisitos
- **Java 21 LTS** ou superior instalado ([Download OpenJDK](https://adoptium.net/temurin/releases/?version=21)).
- Git instalado.

### 1. Clonar o Repositório
```bash
git clone https://github.com/luishperna/slide-md.git
cd slide-md
```

### 2. Executar em Modo de Desenvolvimento
O projeto inclui o Maven Wrapper (`mvnw`), portanto **não é necessário ter o Maven instalado globalmente**.

**No Windows (PowerShell / CMD):**
```powershell
.\mvnw.cmd javafx:run
```

**No Linux / macOS (Terminal):**
```bash
chmod +x ./mvnw
./mvnw javafx:run
```

### 3. Abrir um Arquivo Markdown Específico
Você pode passar o caminho de qualquer arquivo Markdown diretamente pela linha de comando:

```powershell
# Demonstração completa de recursos e atalhos
.\mvnw.cmd javafx:run -Djavafx.args="examples/demo.md"

# Keynote comercial do Tesla Roadster 2026
.\mvnw.cmd javafx:run -Djavafx.args="examples/tesla-roadster/tesla-roadster.md"
```

Com trilha sonora contínua de fundo:
```powershell
.\mvnw.cmd javafx:run -Djavafx.args="examples/demo.md --music ./audio/theme.mp3"
```

### 4. Executar os Testes Automatizados
```powershell
.\mvnw.cmd test
```

### 5. Gerar o Fat JAR Standalone (~10MB)
Para gerar um único arquivo `.jar` executável contendo todas as dependências embutidas:

```powershell
.\mvnw.cmd package -DskipTests
```

O arquivo standalone será gerado em:
```
target/slide-md-1.0.0-SNAPSHOT.jar
```

Você pode executá-lo diretamente em qualquer computador com Java 21:
```powershell
java -jar target/slide-md-1.0.0-SNAPSHOT.jar examples/demo.md
```

---

## 🗺️ Roadmap para Próximas Versões

- [ ] Lançamento oficial de instaladores nativos assinados (`.exe`, `.msi`, `.deb`, `.dmg`) via GitHub Actions.
- [ ] Exportação de slides para **PDF de alta resolução** e **HTML estático interativo**.
- [ ] Modo Moderador / Apresentador (*Presenter View*) com tela dupla para notas do orador e cronômetro.
- [ ] Galeria de temas visuais minimalistas pré-configurados.
- [ ] Suporte a equações matemáticas renderizadas via LaTeX/KaTeX.

---

## 🤝 Contribuindo

Contribuições da comunidade são muito bem-vindas! Se você deseja sugerir novos recursos, corrigir bugs ou melhorar a documentação:

1. Faça um **Fork** do repositório.
2. Crie uma branch para a sua feature (`git checkout -b feature/minha-melhoria`).
3. Siga o padrão de [Conventional Commits](https://www.conventionalcommits.org/) (ex.: `feat: adiciona transicao flip`, `fix: corrige alinhamento de listas`).
4. Certifique-se de que todos os testes passem com `.\mvnw.cmd test`.
5. Abra um **Pull Request**.

---

## 📄 Licença

Este projeto é distribuído sob a licença livre e permissiva **MIT**.  
Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.

---

<p align="center">
  Desenvolvido com ☕ e código aberto por <strong>Luís Perna</strong> e contribuidores.
</p>
