import { AppCard } from "../ui";

export function RequestGuide() {
  const steps = [
    {
      number: 1,
      title: "Tipo Sanguíneo do Paciente",
      description:
        "Selecione o tipo sanguíneo necessário. O sistema fará a compatibilização inteligente e notificará automaticamente doadores compatíveis da região.",
    },
    {
      number: 2,
      title: "Hemocentro de Atendimento",
      description:
        "Escolha a instituição cadastrada onde a coleta será feita. As doações recebidas no local abaterão a meta da requisição por ordem de chegada (FIFO).",
    },
    {
      number: 3,
      title: "Impacto da Urgência",
      description:
        "Requisições com urgência Crítica ganham destaque prioritário no mapa e no topo da lista de recomendações dos doadores elegíveis.",
    },
  ];

  return (
    <AppCard className="p-8 bg-surface-container-low border-0">
      <h4 className="font-headline font-extrabold text-xl mb-6 flex items-center gap-2">
        <span className="material-symbols-outlined text-[#ae131a]">lightbulb</span>
        Guia de Precisão
      </h4>

      <div className="space-y-6">
        {steps.map((step) => (
          <div key={step.number} className="flex gap-4">
            <div className="w-10 h-10 shrink-0 bg-white rounded-full flex items-center justify-center text-[#ae131a] font-bold">
              {step.number}
            </div>
            <div>
              <p className="text-sm font-bold text-on-surface mb-1">{step.title}</p>
              <p className="text-xs text-secondary leading-relaxed">{step.description}</p>
            </div>
          </div>
        ))}
      </div>
    </AppCard>
  );
}
