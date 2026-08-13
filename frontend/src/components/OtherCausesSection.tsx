import { useState } from "react";

export type CauseId = "aferese" | "medula" | "orgaos";

interface CauseDetail {
  id: CauseId;
  title: string;
  subtitle: string;
  badge: string;
  icon: string;
  accentColor: "red" | "purple" | "emerald";
  summary: string;
  highlightText: string;
  whatIsIt: string;
  howItWorks: string;
  frequencyOrTime: string;
  requirements: string[];
  whyItMatters: string;
  faqs: { question: string; answer: string }[];
  stepByStep: { step: string; title: string; desc: string }[];
  callToAction: string;
}

const CAUSES_DATA: CauseDetail[] = [
  {
    id: "aferese",
    title: "Doação por Aférese",
    subtitle: "Plaquetas & Componentes Específicos",
    badge: "1 Doação = 8 Pacientes Beneficiados",
    icon: "biotech",
    accentColor: "red",
    summary:
      "Procedimento automatizado em que apenas o componente necessário (como plaquetas ou plasma) é retirado, devolvendo o restante do sangue ao doador.",
    highlightText:
      "Uma única doação por aférese fornece a quantidade de plaquetas obtida em 6 a 8 doações convencionais de sangue total, reduzindo o risco de reações transfusionais em pacientes oncológicos.",
    whatIsIt:
      "A aférese é uma tecnologia avançada de doação onde o sangue total passa por uma máquina separadora de componentes. Ela retira seletivamente o que é necessário (geralmente plaquetas) e retorna as hemácias e plasma para o seu corpo pela mesma veia ou no outro braço.",
    howItWorks:
      "Durante cerca de 60 a 90 minutos, você fica acomodado confortavelmente enquanto uma centrífuga estéril de uso único separa as plaquetas. Como o corpo repõe as plaquetas em até 48 horas, o tempo de recuperação é muito rápido.",
    frequencyOrTime: "Você pode doar plaquetas a cada 3 a 7 dias, até o limite de 24 vezes ao ano!",
    requirements: [
      "Estar em bom estado geral de saúde e pesar mais de 50 kg",
      "Contagem adequada de plaquetas no sangue (avaliada antes da coleta)",
      "Ter veias calibrosas e acessíveis nos dois braços (ou em um braço para equipamentos de acesso único)",
      "Não ter tomado anti-inflamatórios ou aspirina nos últimos 5 a 7 dias",
      "Atender aos demais critérios básicos de doação de sangue",
    ],
    whyItMatters:
      "Pacientes em tratamento de leucemia, quimioterapia grave, grandes cirurgias e queimaduras necessitam urgentemente de transfusões diárias de plaquetas para evitar hemorragias letais.",
    faqs: [
      {
        question: "A doação por aférese enfraquece mais o corpo?",
        answer:
          "Não. Como as hemácias (glóbulos vermelhos) retornam para o seu corpo, você não perde oxigenação. A reposição de plaquetas pelo organismo é extremamente rápida.",
      },
      {
        question: "Dói mais do que a doação normal?",
        answer:
          "A picada da agulha é idêntica à doação comum de sangue. O procedimento apenas leva um pouco mais de tempo (cerca de 60 a 90 minutos), durante os quais você pode assistir a vídeos ou ouvir música.",
      },
    ],
    stepByStep: [
      { step: "01", title: "Triagem & Contagem", desc: "Coleta-se uma pequena amostra para verificar suas taxas de plaquetas e veias." },
      { step: "02", title: "Conexão à Máquina", desc: "Utiliza-se material 100% descartável em ambiente totalmente estéril." },
      { step: "03", title: "Separação Automática", desc: "A máquina separa as plaquetas enquanto você descansa confortavelmente." },
      { step: "04", title: "Recuperação Rápida", desc: "Após um lanche caprichado, você retoma suas atividades normais no mesmo dia." },
    ],
    callToAction: "Agendar Avaliação para Aférese no Hemocentro",
  },
  {
    id: "medula",
    title: "Doação de Medula Óssea",
    subtitle: "Registro REDOME & Teste HLA",
    badge: "1 em 100.000 de Compatibilidade",
    icon: "genetics",
    accentColor: "purple",
    summary:
      "O cadastro no REDOME é o primeiro passo para salvar pacientes com leucemias e anemias graves que necessitam de um transplante compatível.",
    highlightText:
      "Cadastrar-se exige apenas a coleta de uma amostra de 5 ml de sangue para o teste de histocompatibilidade (HLA). Você só doará a medula caso no futuro surja um paciente 100% compatível.",
    whatIsIt:
      "A medula óssea é o tecido gelatinoso localizado no interior dos ossos grandes (conhecida popularmente como 'tutano'). É a fábrica das células do sangue. Para muitos pacientes com doenças do sangue, o transplante é a única chance de cura.",
    howItWorks:
      "Ao se cadastrar no hemocentro, seus dados genéticos ficam no REDOME. Se um dia você for compatível com um paciente (nacional ou internacional), você será contatado para confirmar seu interesse e passar por exames detalhados.",
    frequencyOrTime: "O cadastro é feito apenas uma vez na vida. Mantenha sempre seus dados atualizados no REDOME!",
    requirements: [
      "Ter entre 18 e 35 anos de idade no momento do cadastro",
      "Estar em bom estado geral de saúde",
      "Não possuir doenças infecciosas transmissíveis pelo sangue (HIV, Hepatite B/C)",
      "Não apresentar histórico de doenças autoimunes ou câncer",
      "Apresentar documento de identidade oficial com foto",
    ],
    whyItMatters:
      "A chance de encontrar um doador compatível na família é de apenas 25%. Para 75% dos pacientes, a esperança vem dos doadores voluntários cadastrados no REDOME.",
    faqs: [
      {
        question: "Doar medula óssea deixa a pessoa paralisada?",
        answer:
          "Mito total! A medula óssea fica dentro dos ossos (como a bacia) e NÃO tem relação com a palha/medula espinhal (sistema nervoso). O procedimento é seguro e a medula se regenera em poucas semanas.",
      },
      {
        question: "Como é feita a doação de medula quando há compatibilidade?",
        answer:
          "Pode ser feita por punção nos ossos da bacia (sob anestesia) ou por coleta periférica por sangue (semelhante à aférese, usando medicação estimulante por alguns dias).",
      },
    ],
    stepByStep: [
      { step: "01", title: "Cadastro no Hemocentro", desc: "Preencha a ficha do REDOME e colete apenas 5 ml de sangue." },
      { step: "02", title: "Mapeamento Genético", desc: "Seu perfil HLA é cadastrado no banco nacional e internacional." },
      { step: "03", title: "Chamado de Compatibilidade", desc: "Se houver 'match', o REDOME entra em contato com você." },
      { step: "04", title: "Doação & Cura", desc: "Você realiza o procedimento com acompanhamento médico integral." },
    ],
    callToAction: "Ver Hemocentros com Cadastro REDOME",
  },
  {
    id: "orgaos",
    title: "Doação de Órgãos e Tecidos",
    subtitle: "Conversa com a Família & Legado",
    badge: "1 Doador Salva até 8 Vidas",
    icon: "favorite",
    accentColor: "emerald",
    summary:
      "A doação de órgãos e tecidos pode salvar vidas de pessoas na lista de espera. No Brasil, o ato depende exclusivamente da autorização familiar.",
    highlightText:
      "Não existe cadastro oficial em cartório ou documento que substitua o consentimento da sua família. O passo mais transformador que você pode dar hoje é conversar abertamente com seus familiares sobre seu desejo.",
    whatIsIt:
      "Trata-se do ato de doar órgãos (coração, pulmões, fígado, rins, pâncreas e intestinos) e tecidos (córneas, ossos, pele e válvulas cardíacas) para transplante em pacientes com insuficiência terminal de órgãos.",
    howItWorks:
      "Pode ocorrer pós-morte (quando confirmada a morte encefálica por exames clínicos rigorosos) ou em vida (doação de um dos rins, parte do fígado ou pulmão para familiares de até 4º grau ou autorização judicial).",
    frequencyOrTime: "Manifeste seu desejo hoje mesmo. Uma conversa pode transformar o futuro de muitas famílias.",
    requirements: [
      "Ter avisado explicitamente sua família sobre sua vontade de ser doador de órgãos",
      "Para doação pós-morte: Diagnóstico confirmado de Morte Encefálica por médicos especialistas",
      "Para doação em vida: Ser maior de idade, capaz juridicamente e ter compatibilidade imunológica",
      "Ausência de infecções graves não controladas ou tumores malignos metastáticos",
    ],
    whyItMatters:
      "Milhares de brasileiros aguardam na lista única de transplantes. Uma única pessoa doadora de órgãos e tecidos pode devolver a visão, o sopro de vida e a saúde a mais de 8 pacientes.",
    faqs: [
      {
        question: "Preciso registrar minha intenção de doador de órgãos em cartório?",
        answer:
          "Não é necessário. No Brasil, pela Lei dos Transplantes, a autorização final SEMPRE é dada pelos parentes diretos (cônjuge ou consanguíneos até 2º grau). Por isso, conversar com a família é o único método efetivo.",
      },
      {
        question: "O corpo do doador fica deformado após a doação?",
        answer:
          "Não. A remoção dos órgãos é uma cirurgia feita com todo o respeito e rigor ético. O corpo é recomposto perfeitamente para o sepultamento tradicional sem qualquer alteração na aparência externa.",
      },
    ],
    stepByStep: [
      { step: "01", title: "Decisão Pessoal", desc: "Reflita sobre a importância de doar vida e transformar histórias." },
      { step: "02", title: "Conversa com a Família", desc: "Avise seus pais, filhos, cônjuge e irmãos sobre o seu desejo." },
      { step: "03", title: "Respeito à Vontade", desc: "No momento oportuno, sua família autorizará a doação em seu nome." },
      { step: "04", title: "Renascimento de Vidas", desc: "Seu gesto proporciona uma nova oportunidade a quem mais precisa." },
    ],
    callToAction: "Compartilhar Meu Desejo com a Família",
  },
];

export function OtherCausesSection() {
  const [selectedCause, setSelectedCause] = useState<CauseDetail | null>(null);

  const getThemeClasses = (color: "red" | "purple" | "emerald") => {
    switch (color) {
      case "red":
        return {
          cardBorder: "border-red-200/80 hover:border-red-400",
          badgeBg: "bg-red-100 text-red-800 border-red-200",
          iconBg: "bg-red-50 text-red-600 group-hover:bg-red-600 group-hover:text-white",
          accentText: "text-red-600",
          btnBg: "bg-red-600 hover:bg-red-700 text-white",
          lightBg: "bg-red-50/60 border-red-100",
        };
      case "purple":
        return {
          cardBorder: "border-purple-200/80 hover:border-purple-400",
          badgeBg: "bg-purple-100 text-purple-800 border-purple-200",
          iconBg: "bg-purple-50 text-purple-600 group-hover:bg-purple-600 group-hover:text-white",
          accentText: "text-purple-600",
          btnBg: "bg-purple-600 hover:bg-purple-700 text-white",
          lightBg: "bg-purple-50/60 border-purple-100",
        };
      case "emerald":
        return {
          cardBorder: "border-emerald-200/80 hover:border-emerald-400",
          badgeBg: "bg-emerald-100 text-emerald-800 border-emerald-200",
          iconBg: "bg-emerald-50 text-emerald-600 group-hover:bg-emerald-600 group-hover:text-white",
          accentText: "text-emerald-600",
          btnBg: "bg-emerald-600 hover:bg-emerald-700 text-white",
          lightBg: "bg-emerald-50/60 border-emerald-100",
        };
    }
  };

  return (
    <section id="outras-causas" className="py-20 bg-surface border-t border-gray-200/60 relative overflow-hidden">
      {/* Background visual accents */}
      <div className="absolute top-1/4 -right-20 w-96 h-96 bg-red-100/40 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-10 -left-20 w-96 h-96 bg-purple-100/30 rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-4">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-red-100/90 border border-red-200 text-primary text-xs font-bold uppercase tracking-wider shadow-xs">
            <span className="material-symbols-outlined text-sm">health_and_safety</span>
            Outras Causas de Alto Impacto
          </div>

          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold headline-font text-on-surface tracking-tight">
            Saiba como ajudar a <span className="text-primary underline decoration-red-300 decoration-wavy underline-offset-8">salvar mais vidas</span>
          </h2>

          <p className="text-base sm:text-lg text-secondary font-normal leading-relaxed">
            Além da doação tradicional de sangue total, você pode multiplicar seu impacto no sistema de saúde público e privado. Conheça estas três causas essenciais e saiba como participar.
          </p>
        </div>

        {/* 3 Cause Cards Grid */}
        <div className="grid md:grid-cols-3 gap-8">
          {CAUSES_DATA.map((cause) => {
            const theme = getThemeClasses(cause.accentColor);
            return (
              <div
                key={cause.id}
                className={`bg-white rounded-3xl p-7 border ${theme.cardBorder} shadow-sm hover:shadow-xl transition-all duration-300 flex flex-col justify-between group relative`}
              >
                <div className="space-y-5">
                  {/* Top Badge & Icon */}
                  <div className="flex items-center justify-between">
                    <div className={`w-14 h-14 rounded-2xl ${theme.iconBg} flex items-center justify-center transition-all duration-300 shadow-xs`}>
                      <span className="material-symbols-outlined text-3xl">{cause.icon}</span>
                    </div>
                    <span className={`px-3 py-1 rounded-full text-[11px] font-extrabold border ${theme.badgeBg}`}>
                      {cause.badge}
                    </span>
                  </div>

                  {/* Titles */}
                  <div>
                    <h3 className="text-2xl font-extrabold headline-font text-gray-900 group-hover:text-primary transition-colors">
                      {cause.title}
                    </h3>
                    <p className={`text-xs font-semibold ${theme.accentText} mt-1`}>{cause.subtitle}</p>
                  </div>

                  {/* Summary */}
                  <p className="text-sm text-secondary leading-relaxed">{cause.summary}</p>

                  {/* Highlight Callout */}
                  <div className={`p-4 rounded-2xl border ${theme.lightBg} text-xs text-gray-800 space-y-1`}>
                    <p className="font-bold flex items-center gap-1 text-gray-900">
                      <span className="material-symbols-outlined text-base">auto_awesome</span>
                      Destaque de Impacto
                    </p>
                    <p className="text-gray-700 leading-snug">{cause.highlightText}</p>
                  </div>
                </div>

                {/* Card CTA Action */}
                <div className="pt-6 mt-6 border-t border-gray-100 flex items-center justify-between">
                  <span className="text-xs font-bold text-gray-500">Guia Completo & Requisitos</span>
                  <button
                    type="button"
                    onClick={() => setSelectedCause(cause)}
                    className={`px-4 py-2.5 rounded-xl text-xs font-extrabold ${theme.btnBg} transition-all duration-200 flex items-center gap-1.5 shadow-sm hover:scale-[1.03] active:scale-[0.97]`}
                  >
                    <span>Saber Mais</span>
                    <span className="material-symbols-outlined text-sm">arrow_forward</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {/* Bottom Callout Banner for Family Conversation */}
        <div className="mt-14 bg-gradient-to-r from-gray-900 via-gray-800 to-gray-900 rounded-3xl p-8 text-white shadow-xl flex flex-col md:flex-row items-center justify-between gap-6 border border-gray-700/50">
          <div className="flex items-center gap-5 text-center md:text-left">
            <div className="w-14 h-14 rounded-2xl bg-white/10 text-red-400 flex items-center justify-center shrink-0 border border-white/10">
              <span className="material-symbols-outlined text-3xl">groups</span>
            </div>
            <div>
              <h4 className="text-xl font-extrabold headline-font">Multiplique o Bem na Sua Comunidade</h4>
              <p className="text-sm text-gray-300 mt-1 max-w-2xl">
                O passo mais simples e poderoso é a conscientização. Converse com seus amigos e familiares sobre aférese, medula e doação de órgãos hoje mesmo.
              </p>
            </div>
          </div>
          <a
            href="#compatibilidade"
            className="px-6 py-3.5 rounded-2xl bg-white text-gray-900 hover:bg-gray-100 font-bold text-sm transition-all shrink-0 shadow-md flex items-center gap-2"
          >
            <span className="material-symbols-outlined text-primary text-base">bloodtype</span>
            <span>Ver Compatibilidade Sanguínea</span>
          </a>
        </div>
      </div>

      {/* Modal de Detalhes Informativos */}
      {selectedCause && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-3xl w-full max-h-[90vh] overflow-y-auto shadow-2xl border border-gray-200 my-8">
            {/* Modal Header */}
            <div className="sticky top-0 bg-white/95 backdrop-blur-md px-6 py-5 border-b border-gray-100 flex items-center justify-between z-20">
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 rounded-xl ${getThemeClasses(selectedCause.accentColor).iconBg} flex items-center justify-center`}>
                  <span className="material-symbols-outlined text-2xl">{selectedCause.icon}</span>
                </div>
                <div>
                  <h3 className="text-xl font-extrabold headline-font text-gray-900">
                    {selectedCause.title}
                  </h3>
                  <p className="text-xs text-gray-500 font-medium">{selectedCause.subtitle}</p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setSelectedCause(null)}
                className="w-9 h-9 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 flex items-center justify-center transition-colors"
                title="Fechar"
              >
                <span className="material-symbols-outlined text-xl">close</span>
              </button>
            </div>

            {/* Modal Content Body */}
            <div className="p-6 sm:p-8 space-y-8 text-on-surface">
              {/* Highlight Banner */}
              <div className={`p-5 rounded-2xl border ${getThemeClasses(selectedCause.accentColor).lightBg} flex items-start gap-3`}>
                <span className="material-symbols-outlined text-2xl text-primary shrink-0 mt-0.5">verified</span>
                <div>
                  <p className="font-extrabold text-sm text-gray-900">Ponto Chave de Conscientização</p>
                  <p className="text-xs text-gray-700 mt-1 leading-relaxed">{selectedCause.highlightText}</p>
                </div>
              </div>

              {/* What is it & How it works */}
              <div className="grid md:grid-cols-2 gap-6">
                <div className="bg-surface-container-low p-5 rounded-2xl border border-gray-200/60 space-y-2">
                  <h4 className="font-extrabold text-sm text-gray-900 flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-primary text-lg">help</span>
                    O Que É?
                  </h4>
                  <p className="text-xs text-secondary leading-relaxed">{selectedCause.whatIsIt}</p>
                </div>

                <div className="bg-surface-container-low p-5 rounded-2xl border border-gray-200/60 space-y-2">
                  <h4 className="font-extrabold text-sm text-gray-900 flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-primary text-lg">settings_suggest</span>
                    Como Funciona?
                  </h4>
                  <p className="text-xs text-secondary leading-relaxed">{selectedCause.howItWorks}</p>
                  <p className="text-[11px] font-bold text-primary pt-1">⏱️ {selectedCause.frequencyOrTime}</p>
                </div>
              </div>

              {/* Requirements List */}
              <div className="space-y-3">
                <h4 className="font-extrabold text-base text-gray-900 flex items-center gap-2">
                  <span className="material-symbols-outlined text-emerald-600">fact_check</span>
                  Requisitos Principais para Doação
                </h4>
                <div className="grid sm:grid-cols-1 gap-2.5">
                  {selectedCause.requirements.map((req, idx) => (
                    <div key={idx} className="flex items-start gap-2.5 text-xs text-gray-700 bg-gray-50 p-3 rounded-xl border border-gray-200/50">
                      <span className="material-symbols-outlined text-emerald-600 text-sm shrink-0 mt-0.5">check_circle</span>
                      <span>{req}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Step by Step Guide */}
              <div className="space-y-3">
                <h4 className="font-extrabold text-base text-gray-900 flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary">route</span>
                  Passo a Passo de Como Participar
                </h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                  {selectedCause.stepByStep.map((step) => (
                    <div key={step.step} className="bg-white p-4 rounded-2xl border border-gray-200 shadow-xs space-y-1.5">
                      <span className="px-2.5 py-0.5 rounded-lg bg-red-50 text-primary font-extrabold text-xs inline-block">
                        {step.step}
                      </span>
                      <h5 className="font-bold text-xs text-gray-900">{step.title}</h5>
                      <p className="text-[11px] text-gray-500 leading-snug">{step.desc}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Why It Matters */}
              <div className="bg-gradient-to-r from-red-50 via-red-50/40 to-surface p-5 rounded-2xl border border-red-100 space-y-2">
                <h4 className="font-extrabold text-sm text-primary flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-lg">volunteer_activism</span>
                  Por Que Seu Gesto É Fundamental?
                </h4>
                <p className="text-xs text-gray-700 leading-relaxed">{selectedCause.whyItMatters}</p>
              </div>

              {/* FAQs */}
              <div className="space-y-3">
                <h4 className="font-extrabold text-base text-gray-900 flex items-center gap-2">
                  <span className="material-symbols-outlined text-amber-600">quiz</span>
                  Perguntas Frequentes (Mitos & Verdades)
                </h4>
                <div className="space-y-3">
                  {selectedCause.faqs.map((faq, idx) => (
                    <div key={idx} className="bg-surface-container-low p-4 rounded-2xl border border-gray-200/60 space-y-1">
                      <p className="font-bold text-xs text-gray-900 flex items-center gap-1.5">
                        <span className="text-primary">Q:</span> {faq.question}
                      </p>
                      <p className="text-xs text-secondary leading-relaxed pl-5">{faq.answer}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="sticky bottom-0 bg-white/95 backdrop-blur-md px-6 py-4 border-t border-gray-100 flex flex-col sm:flex-row items-center justify-between gap-3 z-20">
              <span className="text-xs text-gray-500 font-medium text-center sm:text-left">
                Procure o hemocentro parceiro mais próximo para orientações oficiais.
              </span>
              <button
                type="button"
                onClick={() => setSelectedCause(null)}
                className={`w-full sm:w-auto px-6 py-3 rounded-xl text-xs font-bold ${getThemeClasses(selectedCause.accentColor).btnBg} transition-all shadow-md`}
              >
                Concluí a Leitura
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
