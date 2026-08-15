const CAN_DONATE = [
  {
    icon: "cake",
    title: "16 a 69 anos",
    text: "16–17 anos com autorização dos responsáveis.",
  },
  {
    icon: "monitor_weight",
    title: "Peso mínimo 50 kg",
    text: "Abaixo disso a doação não é considerada segura.",
  },
  {
    icon: "favorite",
    title: "Estar saudável no dia",
    text: "Alimentado, hidratado e sem sintomas de doença aguda.",
  },
  {
    icon: "badge",
    title: "Documento original com foto",
    text: "Leve um documento oficial original no dia da doação.",
  },
  {
    icon: "calendar_month",
    title: "Intervalo de cerca de 90 dias",
    text: "Política atual do sistema: cerca de 90 dias desde a última doação de sangue total.",
  },
];

const CANNOT_DONATE = [
  {
    icon: "sick",
    title: "Gripe, febre ou infecção ativa",
    text: "Aguarde a recuperação; o prazo é definido na triagem.",
  },
  {
    icon: "pregnant_woman",
    title: "Gravidez / puerpério recente",
    text: "Há adiamento durante a gestação e após o parto.",
  },
  {
    icon: "ink_pen",
    title: "Tatuagem, piercing ou cirurgia recente",
    text: "Prazos típicos de adiamento; o hemocentro confirma na triagem.",
  },
  {
    icon: "warning",
    title: "Comportamentos de risco / DST em investigação",
    text: "Exposição a infecções transmissíveis pelo sangue pode impedir a doação.",
  },
  {
    icon: "water_loss",
    title: "Anemia ou peso abaixo de 50 kg",
    text: "Hemoglobina baixa ou peso insuficiente adiam ou impedem a doação.",
  },
];

export function EligibilitySection() {
  return (
    <section id="quem-pode-doar" className="py-20 bg-gradient-to-b from-white to-surface scroll-mt-24">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-12">
          <h2 className="text-xs font-bold text-primary uppercase tracking-widest mb-2">Critérios gerais</h2>
          <p className="text-3xl sm:text-4xl font-extrabold headline-font text-on-surface">Quem pode doar</p>
          <p className="mt-4 text-base text-secondary">
            Resumo conservador das regras brasileiras mais comuns. Não substitui orientação médica.
          </p>
        </div>

        <div className="grid lg:grid-cols-2 gap-6">
          <article className="rounded-[2rem] border border-emerald-100 bg-white p-6 sm:p-8 shadow-sm">
            <div className="flex items-center gap-3 mb-6">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700">
                <span className="material-symbols-outlined text-2xl">check_circle</span>
              </div>
              <div>
                <h3 className="font-headline text-xl font-extrabold text-on-surface">Pode doar</h3>
                <p className="text-xs text-secondary">Exemplos alinhados ao Ministério da Saúde</p>
              </div>
            </div>

            <ul className="space-y-4">
              {CAN_DONATE.map((item) => (
                <li key={item.title} className="flex gap-3">
                  <span className="material-symbols-outlined text-emerald-600 shrink-0">{item.icon}</span>
                  <div>
                    <p className="text-sm font-bold text-on-surface">{item.title}</p>
                    <p className="text-xs text-secondary leading-relaxed">{item.text}</p>
                  </div>
                </li>
              ))}
            </ul>
          </article>

          <article className="rounded-[2rem] border border-red-100 bg-white p-6 sm:p-8 shadow-sm">
            <div className="flex items-center gap-3 mb-6">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-red-50 text-primary">
                <span className="material-symbols-outlined text-2xl">block</span>
              </div>
              <div>
                <h3 className="font-headline text-xl font-extrabold text-on-surface">Não pode doar</h3>
                <p className="text-xs text-secondary">Impedimentos temporários ou permanentes</p>
              </div>
            </div>

            <ul className="space-y-4">
              {CANNOT_DONATE.map((item) => (
                <li key={item.title} className="flex gap-3">
                  <span className="material-symbols-outlined text-primary shrink-0">{item.icon}</span>
                  <div>
                    <p className="text-sm font-bold text-on-surface">{item.title}</p>
                    <p className="text-xs text-secondary leading-relaxed">{item.text}</p>
                  </div>
                </li>
              ))}
            </ul>
          </article>
        </div>

        <p className="mt-8 text-center text-xs text-secondary max-w-2xl mx-auto leading-relaxed">
          A triagem final é sempre feita no hemocentro.
        </p>
      </div>
    </section>
  );
}
