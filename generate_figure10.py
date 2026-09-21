import pymupdf

# SOBER ACADEMIC DESIGN:
# - Harmonious with scientific monographs, UML diagrams and abnTeX2
# - Elegant corporate/academic palette: Slate, Deep Navy, Muted Steel, Neutral Gray
# - High readability, zero saturated/clashing colors (no rainbow, no neon, no aggressive red/orange/pink)

svg_content = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 570" width="900" height="570">
  <defs>
    <style>
      text { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, Helvetica, sans-serif; }
    </style>
  </defs>

  <!-- Clean academic border -->
  <rect width="900" height="570" fill="#ffffff" rx="6" stroke="#94a3b8" stroke-width="1.2"/>

  <!-- ================= 1. ENTRADA (TETOS & POOL) ================= -->
  <g transform="translate(30, 22)">
    <!-- Header Step 1 -->
    <rect x="0" y="0" width="22" height="22" rx="4" fill="#334155"/>
    <text x="11" y="16" font-size="13px" font-weight="bold" fill="#ffffff" text-anchor="middle">1</text>
    <text x="32" y="16" font-size="14.5px" font-weight="bold" fill="#1e293b">Entradas e Tetos Proporcionais</text>
    <text x="500" y="16" font-size="13px" font-weight="bold" fill="#475569">Pool do hemocentro: 2,0 bolsas disponíveis (D1 + D2)</text>

    <!-- Card R1 -->
    <g transform="translate(0, 30)">
      <rect width="405" height="74" rx="6" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1.2"/>
      <rect width="4" height="74" rx="2" fill="#1e3a5f"/>
      <text x="16" y="25" font-size="14px" font-weight="bold" fill="#0f172a">Solicitação R1 (Mais antiga)</text>
      <text x="16" y="46" font-size="12px" fill="#475569">Meta: 1,0 bolsa  •  Decorrido: 50% do prazo (10/20 dias)</text>
      <text x="16" y="65" font-size="13.5px" font-weight="bold" fill="#1e3a5f">Teto Proporcional = 0,5 bolsa</text>
    </g>

    <!-- Card R2 -->
    <g transform="translate(435, 30)">
      <rect width="405" height="74" rx="6" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1.2"/>
      <rect width="4" height="74" rx="2" fill="#476582"/>
      <text x="16" y="25" font-size="14px" font-weight="bold" fill="#0f172a">Solicitação R2 (Mais recente)</text>
      <text x="16" y="46" font-size="12px" fill="#475569">Meta: 2,0 bolsas  •  Decorrido: 30% do prazo (3/10 dias)</text>
      <text x="16" y="65" font-size="13.5px" font-weight="bold" fill="#1e3a5f">Teto Proporcional = 0,6 bolsa</text>
    </g>
  </g>

  <!-- ================= 2. AS DUAS PASSAGENS ================= -->
  <g transform="translate(30, 148)">
    <rect x="0" y="0" width="22" height="22" rx="4" fill="#334155"/>
    <text x="11" y="16" font-size="13px" font-weight="bold" fill="#ffffff" text-anchor="middle">2</text>
    <text x="32" y="16" font-size="14.5px" font-weight="bold" fill="#1e293b">Distribuição em Duas Passagens</text>

    <!-- 1ª Passagem Card -->
    <g transform="translate(0, 28)">
      <rect width="405" height="144" rx="6" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1.2"/>
      <rect width="405" height="28" rx="6" fill="#f1f5f9"/>
      <rect x="0" y="16" width="405" height="12" fill="#f1f5f9"/>
      <line x1="0" y1="28" x2="405" y2="28" stroke="#cbd5e1" stroke-width="1"/>
      <text x="16" y="20" font-size="13px" font-weight="bold" fill="#1e293b">1ª Passagem: Limite pelo Teto</text>

      <!-- Step D1 -->
      <text x="16" y="52" font-size="12.5px" font-weight="bold" fill="#1e293b">• Doação D1 (1,0):</text>
      <text x="142" y="52" font-size="12.5px" fill="#334155"><tspan font-weight="bold" fill="#1e3a5f">0,5</tspan> p/ R1 (teto) + <tspan font-weight="bold" fill="#1e3a5f">0,5</tspan> p/ R2</text>

      <!-- Step D2 -->
      <text x="16" y="78" font-size="12.5px" font-weight="bold" fill="#1e293b">• Doação D2 (1,0):</text>
      <text x="142" y="78" font-size="12.5px" fill="#334155"><tspan font-weight="bold" fill="#1e3a5f">0,1</tspan> p/ R2 (teto)  (R1 represada)</text>

      <!-- Result 1st pass -->
      <line x1="16" y1="96" x2="389" y2="96" stroke="#e2e8f0" stroke-width="1"/>
      <text x="16" y="116" font-size="12px" fill="#475569">Alocado: R1 = <tspan font-weight="bold" fill="#1e293b">0,5</tspan> | R2 = <tspan font-weight="bold" fill="#1e293b">0,6</tspan></text>
      <text x="16" y="133" font-size="12px" font-weight="bold" fill="#475569">Sobra não absorvida: <tspan fill="#1e3a5f">0,9 bolsa</tspan></text>
    </g>

    <!-- 2ª Passagem Card -->
    <g transform="translate(435, 28)">
      <rect width="405" height="144" rx="6" fill="#f8fafc" stroke="#cbd5e1" stroke-width="1.2"/>
      <rect width="405" height="28" rx="6" fill="#f1f5f9"/>
      <rect x="0" y="16" width="405" height="12" fill="#f1f5f9"/>
      <line x1="0" y1="28" x2="405" y2="28" stroke="#cbd5e1" stroke-width="1"/>
      <text x="16" y="20" font-size="13px" font-weight="bold" fill="#1e293b">2ª Passagem: Meta Cheia (FIFO Sobras)</text>

      <!-- Explanation -->
      <text x="16" y="52" font-size="12.5px" font-weight="bold" fill="#1e293b">• Distribuição da Sobra (0,9 bolsa):</text>
      <text x="32" y="75" font-size="12.5px" fill="#334155">→ <tspan font-weight="bold" fill="#1e3a5f">0,5 bolsa</tspan> para R1 (atinge meta cheia de 1,0)</text>
      <text x="32" y="98" font-size="12.5px" fill="#334155">→ <tspan font-weight="bold" fill="#1e3a5f">0,4 bolsa</tspan> para R2 (atinge 1,0 de 2,0)</text>

      <line x1="16" y1="112" x2="389" y2="112" stroke="#e2e8f0" stroke-width="1"/>
      <text x="16" y="132" font-size="12px" font-weight="bold" fill="#1e3a5f">✓ Sobra esgotada com 100% de aproveitamento</text>
    </g>
  </g>

  <!-- ================= 3. RESULTADO FINAL ================= -->
  <g transform="translate(30, 346)">
    <rect x="0" y="0" width="22" height="22" rx="4" fill="#334155"/>
    <text x="11" y="16" font-size="13px" font-weight="bold" fill="#ffffff" text-anchor="middle">3</text>
    <text x="32" y="16" font-size="14.5px" font-weight="bold" fill="#1e293b">Resultado Final da Alocação</text>

    <!-- Barra R1 -->
    <g transform="translate(0, 30)">
      <text x="0" y="15" font-size="13.5px" font-weight="bold" fill="#1e293b">Solicitação R1:</text>
      <text x="120" y="15" font-size="13px" font-weight="bold" fill="#1e3a5f">1,0 / 1,0 bolsa alocada (100% — Concluída)</text>

      <!-- Bar R1 -->
      <g transform="translate(0, 22)">
        <rect width="840" height="26" rx="4" fill="#f8fafc" stroke="#94a3b8" stroke-width="1"/>
        <rect x="0" y="0" width="420" height="26" rx="4" fill="#1e3a5f"/>
        <rect x="410" y="0" width="10" height="26" fill="#1e3a5f"/>
        <text x="210" y="18" font-size="12px" font-weight="bold" fill="#ffffff" text-anchor="middle">1ª Passagem (Teto): 0,5 bolsa</text>

        <rect x="420" y="0" width="420" height="26" rx="4" fill="#476582"/>
        <rect x="420" y="0" width="10" height="26" fill="#476582"/>
        <text x="630" y="18" font-size="12px" font-weight="bold" fill="#ffffff" text-anchor="middle">2ª Passagem (Sobra): 0,5 bolsa</text>
      </g>
    </g>

    <!-- Barra R2 -->
    <g transform="translate(0, 92)">
      <text x="0" y="15" font-size="13.5px" font-weight="bold" fill="#1e293b">Solicitação R2:</text>
      <text x="120" y="15" font-size="13px" font-weight="bold" fill="#334155">1,0 / 2,0 bolsas alocadas (50% — Atendimento proporcional)</text>

      <!-- Bar R2 (840 px total: 0.6 = 252 px, 0.4 = 168 px, 1.0 = 420 px) -->
      <g transform="translate(0, 22)">
        <rect width="840" height="26" rx="4" fill="#f8fafc" stroke="#94a3b8" stroke-width="1"/>
        <rect x="0" y="0" width="252" height="26" rx="4" fill="#1e3a5f"/>
        <rect x="242" y="0" width="10" height="26" fill="#1e3a5f"/>
        <text x="126" y="18" font-size="12px" font-weight="bold" fill="#ffffff" text-anchor="middle">1ª Passagem: 0,6</text>

        <rect x="252" y="0" width="168" height="26" fill="#476582"/>
        <text x="336" y="18" font-size="12px" font-weight="bold" fill="#ffffff" text-anchor="middle">2ª Passagem: 0,4</text>

        <text x="630" y="18" font-size="12px" font-weight="bold" fill="#64748b" text-anchor="middle">Demanda pendente: 1,0 bolsa</text>
      </g>
    </g>

    <!-- Legenda de cores -->
    <g transform="translate(0, 156)">
      <rect x="0" y="2" width="14" height="14" rx="2" fill="#1e3a5f"/>
      <text x="20" y="14" font-size="12px" font-weight="500" fill="#334155">1ª Passagem (Teto proporcional)</text>

      <rect x="240" y="2" width="14" height="14" rx="2" fill="#476582"/>
      <text x="260" y="14" font-size="12px" font-weight="500" fill="#334155">2ª Passagem (Distribuição da sobra)</text>

      <rect x="520" y="2" width="14" height="14" rx="2" fill="#f8fafc" stroke="#94a3b8" stroke-width="1"/>
      <text x="540" y="14" font-size="12px" font-weight="500" fill="#64748b">Demanda pendente (restante da meta)</text>
    </g>
  </g>

</svg>
"""

# Save SVG
with open("tcc-latex/figuras/figura10-exemplo-alocacao-teto.svg", "w", encoding="utf-8") as f:
    f.write(svg_content)

# Render to PNG at 2x scale (1800 x 1140)
doc = pymupdf.open("tcc-latex/figuras/figura10-exemplo-alocacao-teto.svg")
pix = doc[0].get_pixmap(dpi=144)
pix.save("tcc-latex/figuras/figura10-exemplo-alocacao-teto.png")
print("Rendered PNG size:", pix.width, "x", pix.height)
