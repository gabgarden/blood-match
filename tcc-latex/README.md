# TCC em LaTeX — Normas ABNT (NBR 14724:2024 / vigente em 2026)

Conversão da monografia de **Gabriel Silveira de Azevedo** e **Mateus Carvalho Costa Nogueira** para LaTeX com a classe [`abntex2`](https://ctan.org/pkg/abntex2), alinhada à **ABNT NBR 14724:2024** (trabalhos acadêmicos), **NBR 10520** (citações) e **NBR 6023** (referências).

## Estrutura

```
tcc-latex/
├── main.tex
├── referencias.bib
├── REVISAO.md
├── README.md
├── capitulos/
│   ├── 01-introducao.tex
│   ├── 02-fundamentacao.tex      # Referencial teórico
│   ├── 03-trabalhos-relacionados.tex
│   ├── 04-metodos.tex
│   ├── 05-estudo-de-caso.tex
│   ├── 06-avaliacao.tex
│   └── 07-consideracoes.tex
├── apendices/
│   ├── A-regras-dominio.tex
│   ├── B-agregados.tex
│   ├── C-fulfillment-matching.tex
│   ├── D-dip-caso-uso.tex
│   └── codigo/                    # trechos Java das listagens
└── figuras/
```

Ordem dos capítulos (orientação da banca):

1. Introdução  
2. Referencial teórico  
3. Trabalhos relacionados  
4. Métodos e recursos  
5. Estudo de caso  
6. Avaliação  
7. Considerações finais  

Apêndices (código-fonte elaborado pelos autores — NBR 14724):

- **A** — tipagem sanguínea e elegibilidade do doador  
- **B** — invariantes dos agregados `DonationRequest` e `Donation`  
- **C** — alocação FIFO e matching de doadores  
- **D** — inversão de dependência (repositório + caso de uso)  

## O que já está pronto

- Capa, folha de rosto, folha de aprovação, listas, resumo/abstract e sumário
- Capítulos **1 a 7** preenchidos (avaliação e considerações finais com resultados da suíte de testes)
- Apêndices **A–D** com listagens dos trechos de código mais relevantes (domínio, agregados, FIFO/matching e DIP)
- Referências em BibTeX com citações autor-data (`\citeonline{...}`)
- Fontes de ilustrações no formato exigido pela NBR 14724:2024  
  (`Elaborado pelos próprios autores`)

## Requisitos

1. [TeX Live](https://tug.org/texlive/) ou [MiKTeX](https://miktex.org/)
2. Pacotes: `abntex2`, `biblatex`, `biblatex-abnt`, `biber`, `newtx`, `graphicx`, `booktabs`, `hyperref`, `enumitem`

No **MiKTeX** (não use `tlmgr`):

```powershell
miktex packages install abntex2 biblatex biblatex-abnt biber newtx
```

No **TeX Live**:

```bash
tlmgr install abntex2 biblatex biblatex-abnt biber newtx
```

## Compilação

O projeto utiliza `biblatex` com estilo `abnt` e motor `biber` (em vez de `bibtex`).

Na pasta `tcc-latex/`:

```powershell
pdflatex main
biber main
pdflatex main
pdflatex main
```

Ou com `latexmk`:

```powershell
latexmk -pdf main.tex
```

No VS Code / Cursor: extensão **LaTeX Workshop** configurada com a receita `pdflatex -> biber -> pdflatex -> pdflatex`.

## Figuras e quadros

As imagens do PDF original não foram embutidas automaticamente. Para cada placeholder `[Inserir Figura/Gráfico/Quadro…]`:

1. Exporte a imagem do PDF (ou do original Word/draw.io)
2. Salve em `figuras/` (ex.: `figuras/figura01-processo-tcc.png`)
3. Substitua o `\fbox{...}` por:

```latex
\includegraphics[width=0.9\textwidth]{figura01-processo-tcc}
```

## Ajustes ABNT 2024 neste projeto

| Item | Como está no projeto |
|------|----------------------|
| Margens 3 cm (esq./sup.) e 2 cm (dir./inf.) | `geometry` + `oneside` (anverso) |
| Espaçamento 1,5 | Padrão `abntex2` |
| Fonte 12 pt Times | `newtxtext`/`newtxmath` |
| Seções primárias em página nova | `\chapter` inicia página |
| Fonte em ilustrações | Comando `\fonte{...}` |
| Terminologia “seção” na organização | Seção 1.4 atualizada (NBR 14724:2024) |
| Citações autor-data | `abntex2cite` estilo `alf` |
| Recuo de parágrafo | 1,25 cm |

> **Nota sobre margens:** com `twoside`, páginas pares invertem esquerda/direita (verso). Para TCC entregue em PDF (só anverso), usa-se `oneside` para manter **sempre** 3 cm à esquerda e 2 cm à direita em todas as páginas.

> Observação: a classe oficial `abntex2` no CTAN ainda documenta a NBR 14724:2011; as mudanças da revisão 2024 foram aplicadas manualmente neste template (fonte das ilustrações, nomenclatura de seções na organização do texto, etc.).

## Preencher após a defesa

- Datas e nomes na **folha de aprovação**
- Dedicatória / agradecimentos / epígrafe (opcional; basta descomentar no `main.tex` se desejar)
- Conteúdo das seções marcadas com `% TODO` nos capítulos 5–7
