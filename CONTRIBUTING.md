# Guia de Contribuição — slide-md ⚡

Obrigado pelo interesse em contribuir com o **slide-md**! Este é um projeto de código aberto, livre e focado na comunidade.

---

## 📋 Diretrizes de Desenvolvimento

1. **Java 21 LTS & JavaFX**:
   - Utilize recursos modernos do Java 21 (pattern matching, records, switches expressivos).
   - Mantenha a interface gráfica responsiva: qualquer operação de I/O pesado ou processamento de mídia deve ser executada fora da JavaFX Application Thread.

2. **Zero Dependências Obrigatórias no Cliente**:
   - O aplicativo deve sempre funcionar "out-of-the-box" sem exigir ferramentas externas obrigatórias (ex.: fallbacks nativos via JavaFX MediaPlayer).

3. **Design Minimalista & Distraction-Free**:
   - Mantenha a interface visual limpa e elegante. Elementos de controle secundários devem preferencialmente residir no HUD auto-ocultável ou no painel lateral.

---

## 🛠️ Como Contribuir

1. **Fork o repositório** para a sua conta no GitHub.
2. **Crie uma branch** para sua alteração:
   ```bash
   git checkout -b feature/nome-da-feature
   # ou
   git checkout -b fix/descricao-do-bug
   ```
3. **Escreva testes** em `src/test/java/com/slidemd/` para novas funcionalidades ou correções de parser/serviços.
4. **Verifique se todos os testes passam**:
   ```bash
   ./mvnw test
   ```
5. **Faça commits semânticos** no padrão [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat: adiciona nova diretiva de layout`
   - `fix: corrige alinhamento de imagens no slide`
   - `docs: atualiza guia de atalhos no README`
   - `test: adiciona testes unitários para parser de video`
6. **Envie seu Pull Request** detalhando as alterações e motivação.

---

## 💬 Dúvidas ou Sugestões?
Abra uma [Issue no GitHub](https://github.com/luishperna/slide-md/issues) para discutir novas ideias ou relatar problemas.
