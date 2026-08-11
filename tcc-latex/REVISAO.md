# Mudanças na monografia (revisão editorial e alinhamento ao sistema)

Documento gerado após cruzar o texto da monografia em `tcc-latex/` com o estado atual do código (`ARQUITETURA.md`, `docs/FULFILLMENT_STRATEGY.md`, `pom.xml`).

---

## 0. Ordem dos capítulos (atualização sob orientação)

Ordem vigente:

1. Introdução  
2. Referencial teórico  
3. Trabalhos relacionados  
4. Métodos e recursos  
5. Estudo de caso  
6. Avaliação  
7. Considerações finais  

Arquivos renomeados em `capitulos/` para refletir essa numeração. Referências cruzadas (`Capítulo~N`) e a seção “Organização da monografia” foram atualizadas.

---

## 1. Objetivos da revisão

1. Eliminar redundâncias entre capítulos.
2. Impedir vazamento de conteúdo (teoria no Cap.~5, implementação no Cap.~3, etc.).
3. Alinhar fatos técnicos ao sistema real.
4. Normalizar formatação ABNT: estrangeirismos em itálico, siglas após primeira expansão, títulos em caixa de sentença.
5. Atualizar requisitos/casos de uso e preencher seções que estavam vazias.

---

## 2. Alinhamento monografia × sistema

| Antes (texto) | Situação real | Ajuste |
|---|---|---|
| Java **21** | `pom.xml` → Java **17** | Cap.~2 corrigido |
| “Recomendação **de doadores**” como núcleo | API recomenda **solicitações ao doador** (`GetRecommendedRequestsUseCase`); notificação usa doadores elegíveis | Objetivos, resumo, Cap.~5 e título da §5.6 |
| Cap.~5 sem fulfillment | `fulfilledBloodBags` + FIFO por hemocentro, materializado na escrita | Nova §5.6 detalhada |
| RF/UC incompletos | Existem notify, reschedule, summary, goal/date-limit, cancel, etc. | RF01–RF22 e UC01–UC18 |
| Cap.~5 com `% TODO` | Implementação já existente | Persistência MongoDB, JWT, OpenAPI/springdoc preenchidos |
| Contextos delimitados como fato estrutural | Pacotes em camadas; BC é interpretação de *design* | Linguagem suavizada no Cap.~5 |

---

## 3. Limpeza de papéis por capítulo

| Capítulo | Deve conter | O que foi removido / movido |
|---|---|---|
| **1 Introdução** | Problema, objetivos, justificativa, organização | Definições longas de produto; mini-aula DDD/SOLID |
| **2 Métodos** | Abordagem, instrumentos, ferramentas, cronograma | Definições de DDD/SOLID; pipeline longo que repetia Caps.~3–5 |
| **3 Fundamentação** | OO, SOLID, DDD (teoria) | *Tours* de classes do Bloodmatch (`Party`, `DonationRequest`, métodos) |
| **4 Trabalhos relacionados** | MSL, resultados, lacuna | Fechamento que reexpandia API/REST/DDD |
| **5 Estudo de caso** | Requisitos, modelagem, arquitetura, implementação | Retóricas teóricas; agora aplica Cap.~3 e descreve o código |
| **6 Avaliação** | Plano (O4/O5) | TODOs vazios → plano explícito sem inventar resultados |
| **7 Considerações** | Retorno a O1–O5 | Placeholders honestos (sem afirmar avaliação concluída) |

---

## 4. Formatação e linguagem (ABNT)

### 4.1 Estrangeirismos

Padronizado uso de `\textit{...}` para termos em língua estrangeira mantidos no texto, por exemplo:

- *framework*, *backend*, *software*, *stateless*, *debug*, *beans*, *container*, *branches*, *main*
- *Value Objects*, *Entities*, *Aggregate Root*, *Bounded Context*
- *design* (quando mantido em inglês)
- Princípios SOLID nos subtítulos: `\textit{Single Responsibility Principle} (SRP)`, etc.
- Título da seção DDD: `\textit{Domain-Driven Design} (DDD)`

### 4.2 Siglas (expansão uma vez)

| Sigla | Onde expandir | Depois |
|---|---|---|
| API, RESTful | Cap.~1 (objetivos) | só API / RESTful |
| DDD | Cap.~1 (O1) + título Cap.~3 | só DDD |
| SOLID | Cap.~1 / abertura Cap.~3 | só SOLID |
| OMS, OPAS | Cap.~1 | só OMS / OPAS |
| JWT | Cap.~2 (ferramentas) | só JWT |
| LTS, JVM | Cap.~2 (Java) | só LTS / JVM |
| FIFO, NoSQL | lista de siglas + Cap.~5 | só FIFO / NoSQL |

Lista de siglas em `main.tex` atualizada com **FIFO** e **NoSQL**.

### 4.3 Títulos e subtítulos

- Capítulos e seções em **caixa de sentença** (português), sem Title Case inglês solto.
- Subtítulos de princípios SOLID e da seção DDD com inglês em itálico.
- Cap.~5 renomeado para refletir escopo real: *Estudo de caso: requisitos, modelagem e implementação*.
- §5.6: *Recomendação de solicitações e cumprimento de metas* (não “motor de recomendação de doadores”).

### 4.4 Outros

- “softwares” → “sistemas” onde cabia.
- “Organização Mundial **de** Saúde” unificado para **da** Saúde (OMS).
- Resumo/Abstract alinhados a Java~17, recomendação de solicitações e FIFO.

---

## 5. Cap.~5 — conteúdo novo ou expandido

1. **RF** ampliados (meta de bolsas, prazo, cancelamento, recomendações, notificação, reagendamento, resumo do doador, distância de recomendação, etc.).
2. **RNF** com concorrência otimista (`@Version`).
3. **UC** alinhados aos diagramas + UC de notify / reschedule / summary.
4. **Arquitetura em camadas** conforme o repositório: `domain`, `application/usecase`, `interfaces/rest`, `infra/persistence`, `infra/security`.
5. **Fulfillment**: algoritmo FIFO, `fulfilledBloodBags` materializado na conclusão de doação; leituras só consomem o campo.
6. **Recomendação** vs **notificação** distinguidas com clareza.

---

## 6. Arquivos tocados

```
tcc-latex/main.tex
tcc-latex/capitulos/01-introducao.tex
tcc-latex/capitulos/02-metodos.tex
tcc-latex/capitulos/03-fundamentacao.tex
tcc-latex/capitulos/04-trabalhos-relacionados.tex
tcc-latex/capitulos/05-estudo-de-caso.tex
tcc-latex/capitulos/06-avaliacao.tex
tcc-latex/capitulos/07-consideracoes.tex
tcc-latex/REVISAO.md          ← este arquivo
```

Figuras em `figuras/` **não** foram alteradas; apenas o texto que as referencia.

---

## 7. Pendências conscientes (não inventadas)

- **Cap.~6**: plano de avaliação escrito; resultados finais dependem da suíte de testes e da conclusão da 2ª etapa.
- **Cap.~7**: estrutura amarrada a O1–O5; texto definitivo após Cap.~6.
- **Cronogramas (Quadros 1 e 2)**: reconstruídos em LaTeX a partir do fluxo do trabalho; validar datas reais com os autores se divergirem do plano oficial.

---

## 8. Como recompilar

```powershell
cd tcc-latex
pdflatex main
bibtex main
pdflatex main
pdflatex main
```
