# Exemplos do slide-md

Esta pasta reúne apresentações prontas para uso, demonstração e estudo das diretivas e recursos do **slide-md**.

---

## 📁 Apresentações Disponíveis

### 1. Demonstração Geral de Recursos
* **Arquivo**: `examples/demo.md`
* **Estilo**: Demonstração oficial completa dos recursos da aplicação.
* **Recursos demonstrados**:
  * Guia completo de atalhos globais de teclado (caneta laser, editor lateral, fullscreen, recarregar).
  * Demonstração prática do modo `transition: morph` a 60 FPS com scroll contínuo.
  * Catálogo de todas as 7 diretivas suportadas (`background`, `alignment`, `color`, `transition`, `video`, `image`, `sound`).
  * Recursos multimídia nativos com corte inline e junção sem FFmpeg obrigatório.
  * Comandos de linha de comando (CLI) e empacotamento.
* **Como executar**:
  ```powershell
  .\mvnw.cmd javafx:run -Djavafx.args="examples/demo.md"
  ```

---

### 2. [Tesla Roadster Keynote 2026](tesla-roadster/)
* **Arquivo**: `examples/tesla-roadster/tesla-roadster.md`
* **Estilo**: Keynote comercial minimalista de lançamento de hipercarro elétrico.
* **Recursos demonstrados**:
  * Gradientes profundos obsidian acelerados por hardware (`[#070b19, #14223d, #080e22, 45%]`).
  * Alternância dinâmica de alinhamento (`center`, `left`, `right`).
  * Vídeo MP4 embutido com corte virtual instantâneo (`[00:30-00:41]`).
  * Imagens em alta definição e citações/referências factuais.
* **Como executar**:
  ```powershell
  .\mvnw.cmd javafx:run -Djavafx.args="examples/tesla-roadster/tesla-roadster.md"
  ```
