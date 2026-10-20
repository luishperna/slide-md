---
background: [#070b19, #14223d, #080e22, 45%]
color: #ffffff
alignment: center
transition: morph
---

# slide-md
### Apresentações fluidas, minimalistas e modernas em Markdown e Java

> Pressione **Espaço** ou **Seta Direita** para iniciar o tour

- **Aceleração Gráfica a 60 FPS** · Transições fluidas aceleradas por hardware GPU (DirectX, Metal, OpenGL)
- **Experiência Sem Distrações** · HUD flutuante com auto-ocultamento quando o mouse fica parado
- **Zero Dependências Obrigatórias** · Reprodução e cortes virtuais de vídeo nativos no JavaFX

---
background: [#080d1a, #16264c, #0a1122, 45%]
color: #ffffff
alignment: left

# Atalhos de Teclado
### Controles simples e intuitivos diretamente na ponta dos dedos

- **Seta Direita / Espaço / Page Down / Enter** · Próximo slide (ou inicia vídeo pendente)
- **Seta Esquerda / Page Up / Backspace** · Slide anterior
- **P** · Alternar **Caneta Laser / Rastro Translúcido** *(pressione **C** para limpar traços)*
- **M ou E** · Abrir/fechar **Painel Lateral do Editor** de Markdown
- **Ctrl + O** (*Cmd + O*) · Abrir qualquer apresentação `.md` pelo seletor visual
- **Ctrl + S** (*Cmd + S*) · Salvar alterações diretamente no arquivo em disco
- **F ou F11** · Alternar **Tela Cheia** sem bordas
- **R** · Recarregar apresentação do disco em tempo real
- **U** · Silenciar / Reativar trilha de áudio (Mute/Unmute)
- **Esc** · Desativar caneta laser, fechar editor ou sair da tela cheia

---
background: [#130812, #291228, #100715, 45%]
color: #ffffff
alignment: right

# Modo Transformar (Morph)
### Scroll contínuo e orgânico entre slides conectados

- **Efeito Parallax Contínuo** · Slides e elementos deslizam conectados de um lado para o outro
- **Aceleração por Hardware** · Física fluida de interpolação em spline a 60 FPS estáveis
- **Estilos de Transição Suportados** · `morph`, `slide`, `fade`, `zoom`

> *Ative por slide ou globalmente com `transition: morph`*

---
background: [#050f1c, #0e2742, #071526, 45%]
color: #ffffff
alignment: left

# Diretrizes Oficiais Limpas
### Configuração prática sem poluir a pureza do Markdown

- `background: [#1e293b, #0f172a, 45%]` · Gradiente em colchetes com ângulo, cor sólida ou imagem
- `alignment: center`, `left`, `right`, `top`, `down` · Posicionamento do conteúdo do slide
- `transition: morph`, `slide`, `fade`, `zoom` · Estilo de animação entre slides
- `color: #ef4444` · Cor temática de títulos (clareia suavemente por nível de `#`) e marcadores
- `video: demo.mp4 [00:10-00:30]` · Corte virtual de vídeo inline e junção por vírgula
- `image: ./assets/foto.jpg` · Imagem de destaque com sombra suave projetada
- `sound: ./audio/tema.mp3` · Trilha de áudio por slide ou global com normalização

---
background: [#080a18, #181b36, #090c1e, 45%]
color: #ffffff
alignment: right

# Multimídia 100% Nativa
### Reprodução instantânea sem ferramentas pesadas no cliente

- **Corte Virtual Instantâneo** · `video: demo.mp4 [00:10-00:30]` toca na hora sem esperar transcode
- **Junção de Vídeos** · Concatene múltiplos arquivos no mesmo slide: `video: v1.mp4, v2.mp4`
- **Controle pelo Apresentador** · O vídeo entra pausado no 1º frame; mais 1 clique toca; próximo avança
- **Trilha Sonora Contínua** · Passe `--music audio.mp3` pelo terminal para música contínua na apresentação

> *Inclui higienização automática com FFmpeg para vídeos com formatos não convencionais.*

---
background: [#070b19, #14223d, #080e22, 45%]
color: #ffffff
alignment: left

# Editor Lateral Integrado
### Ajustes em tempo real sem precisar de editor externo

- **Painel Lateral Integrado (VS Code Style)** · Pressione <kbd>M</kbd> ou <kbd>E</kbd> para abrir o editor
- **Autocomplete de Diretivas** · Sugestões inteligentes ao digitar `background:`, `transition:`, etc.
- **Seletor de Cores Flutuante** · Paleta visual de cores para temas rápidos
- **Divisor Arrastável** · Redimensione o editor livremente ou puxe até a borda para fechar

> *Alterações são visualizadas na hora; aperte **Ctrl + S** para gravar no disco.*

---
background: [#080d1a, #16264c, #0a1122, 45%]
color: #ffffff
alignment: right

# Comandos de Terminal (CLI)
### Flexibilidade para desenvolvedores e linha de comando

- **Executar Aplicação**: `.\mvnw.cmd javafx:run`
- **Abrir Arquivo Específico**: `.\mvnw.cmd javafx:run -Djavafx.args="caminho/slides.md"`
- **Trilha de Fundo**: `.\mvnw.cmd javafx:run -Djavafx.args="slides.md --music tema.mp3"`
- **Rodar Testes**: `.\mvnw.cmd test`
- **Gerar Fat JAR (~10MB)**: `.\mvnw.cmd package -DskipTests`
- **Executar Standalone**: `java -jar target/slide-md-1.0.0-SNAPSHOT.jar slides.md`

---
background: [#070b19, #14223d, #080e22, 45%]
color: #ffffff
alignment: center

# Obrigado!
### Comece a criar apresentações modernas e sem distrações

- **Código Aberto** · Licença livre e permissiva MIT
- **Repositório no GitHub** · [github.com/luishperna/slide-md](https://github.com/luishperna/slide-md)

> Desenvolvido em Java 21, JavaFX, CommonMark e RichTextFX por **Luís Perna**
