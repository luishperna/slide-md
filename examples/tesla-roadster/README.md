# Apresentação: Tesla Roadster (Keynote 2026)

Exemplo completo de apresentação de produto em formato **Keynote de Alta Conversão**, desenvolvido para demonstrar o potencial estético e performático do **slide-md**.

---

## 🏎️ Sobre a Apresentação

Esta apresentação reúne as informações oficiais e factuais divulgadas durante a apresentação técnica da nova geração do **Tesla Roadster**:

* **Evento**: Apresentação oficial da nova geração do Tesla Roadster
* **Data**: 15 de outubro de 2026
* **Local**: Instalações de teste da SpaceX Starbase, Texas, EUA
* **Fabricante**: Tesla, Inc. em colaboração técnica com a SpaceX
* **Fontes jornalísticas e de apuração**: Reuters, CNN Brasil, The Verge e [tesla.com/roadster](https://www.tesla.com/roadster)

---

## 📊 Especificações Técnicas e Validação Factual

| Métrica / Recurso | Dado Divulgado | Status de Validação |
| :--- | :--- | :--- |
| **0 a 96 km/h** | Menos de 1 segundo | Anunciado no evento com pacote especial SpaceX |
| **Velocidade Máxima** | Acima de 370 km/h (> 230 mph) | Especificação de projeto para versão de alto rendimento |
| **Tração** | Tri-Motor AWD 100% Elétrico | Vetorização dinâmica de torque instantâneo |
| **Propulsores a Gás Frio** | Jatos de ar pressurizado (nitrogênio) | Recurso conceitual demonstrado em protótipo de testes |
| **Teto Panorâmico** | Vidro com fibra de carbono removível | Design aerodinâmico conversível |
| **Previsão de Produção** | 2028 | Estimativa anunciada pela fabricante, sujeita à homologação |

---

## 🎨 Recursos Visuais e Diretrizes Utilizadas

- **Gradientes Profundos**:
  - `Midnight Cosmic`: `[#070b19, #14223d, #080e22, 45%]`
  - `Velocity Crimson`: `[#130812, #291228, #100715, 45%]`
  - `SpaceX Cyan`: `[#050f1c, #0e2742, #071526, 45%]`
- **Transição Contínua**: `transition: morph` (scroll contínuo e orgânico a 60 FPS)
- **Alternância Dinâmica de Alinhamento**: `center` na capa e encerramento; `left` e `right` alternados nos tópicos
- **Vídeo Nativo com Corte Instantâneo**: `video: ./assets/roadster-video.mp4 [00:30-00:41]`
- **Citações e Destaques**: `> *Referência: ...*`

---

## 🚀 Como Executar este Exemplo

Na raiz do repositório, execute:

```powershell
.\mvnw.cmd javafx:run -Djavafx.args="examples/tesla-roadster/tesla-roadster.md"
```

Ou no Linux/macOS:

```bash
./mvnw javafx:run -Djavafx.args="examples/tesla-roadster/tesla-roadster.md"
```
