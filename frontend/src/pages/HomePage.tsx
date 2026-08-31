import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { resolvePostLoginPath } from "../routes/roleRouting";
import { OtherCausesSection } from "../components/OtherCausesSection";
import { EligibilitySection } from "../components/EligibilitySection";

type BloodTypeKey = "A+" | "A-" | "B+" | "B-" | "AB+" | "AB-" | "O+" | "O-";

const BLOOD_TYPES: BloodTypeKey[] = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

const DONATE_TO_MAP: Record<BloodTypeKey, BloodTypeKey[]> = {
  "O-": ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"],
  "O+": ["O+", "A+", "B+", "AB+"],
  "A-": ["A+", "A-", "AB+", "AB-"],
  "A+": ["A+", "AB+"],
  "B-": ["B+", "B-", "AB+", "AB-"],
  "B+": ["B+", "AB+"],
  "AB-": ["AB+", "AB-"],
  "AB+": ["AB+"],
};

const RECEIVE_FROM_MAP: Record<BloodTypeKey, BloodTypeKey[]> = {
  "O-": ["O-"],
  "O+": ["O+", "O-"],
  "A-": ["A-", "O-"],
  "A+": ["A+", "A-", "O+", "O-"],
  "B-": ["B-", "O-"],
  "B+": ["B+", "B-", "O+", "O-"],
  "AB-": ["AB-", "A-", "B-", "O-"],
  "AB+": ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"],
};

type BloodTypeDetail = {
  badge: string;
  badgeClass: string;
  rarityBrazil: string;
  coveragePercent: number;
  description: string;
  systemRole: string;
};

const BLOOD_TYPE_DETAILS: Record<BloodTypeKey, BloodTypeDetail> = {
  "O-": {
    badge: "Doador Universal de Hemácias",
    badgeClass: "bg-red-100 text-red-800 border-red-200",
    rarityBrazil: "~3% da população",
    coveragePercent: 100,
    description: "Crucial em UTIs e emergências graves quando não há tempo para tipagem prévia.",
    systemRole: "Item estratégico de prioridade máxima na busca ativa da rede BloodMatch.",
  },
  "O+": {
    badge: "Tipo Mais Comum no Brasil",
    badgeClass: "bg-amber-100 text-amber-800 border-amber-200",
    rarityBrazil: "~36% da população",
    coveragePercent: 82,
    description: "Compatível com todos os receptores Rh positivos (O+, A+, B+, AB+).",
    systemRole: "Representa a maior parcela das solicitações diárias nos hemocentros parceiros.",
  },
  "A+": {
    badge: "Alta Demanda Hospitalar",
    badgeClass: "bg-red-100 text-red-700 border-red-200",
    rarityBrazil: "~34% da população",
    coveragePercent: 42,
    description: "Essencial para pacientes A+ e AB+ em tratamentos de rotina e cirurgias.",
    systemRole: "Importante para manutenção contínua dos estoques cirúrgicos municipais.",
  },
  "A-": {
    badge: "Doador Raro e Estratégico",
    badgeClass: "bg-purple-100 text-purple-800 border-purple-200",
    rarityBrazil: "~3% da população",
    coveragePercent: 85,
    description: "Pode doar para A+, A-, AB+ e AB-. Vital para receptores Rh negativos.",
    systemRole: "Ativa alertas prioritários em doadores cadastrados ao surgir um pedido.",
  },
  "B+": {
    badge: "Especificidade de Grupo B",
    badgeClass: "bg-blue-100 text-blue-800 border-blue-200",
    rarityBrazil: "~8% da população",
    coveragePercent: 12,
    description: "Atende pacientes B+ e AB+. Pode receber doações dos grupos O e B.",
    systemRole: "Garante suporte transfusional constante para tratamentos hematológicos.",
  },
  "B-": {
    badge: "Tipo Sanguíneo Raro",
    badgeClass: "bg-purple-100 text-purple-800 border-purple-200",
    rarityBrazil: "~2% da população",
    coveragePercent: 55,
    description: "Atende B+, B-, AB+ e AB-. Pouquíssimos doadores compatíveis no país.",
    systemRole: "Convocações ativas imediatas via SMS/Push para doadores cadastrados.",
  },
  "AB+": {
    badge: "Receptor Universal",
    badgeClass: "bg-emerald-100 text-emerald-800 border-emerald-200",
    rarityBrazil: "~2.5% da população",
    coveragePercent: 3,
    description: "Pode receber sangue de QUALQUER tipo ABO/Rh com total segurança imunológica.",
    systemRole: "Doadores AB+ são incentivados à doação de plasma e aférese de plaquetas.",
  },
  "AB-": {
    badge: "Tipo Sanguíneo Raríssimo",
    badgeClass: "bg-indigo-100 text-indigo-800 border-indigo-200",
    rarityBrazil: "< 1% da população",
    coveragePercent: 5,
    description: "O grupo ABO com menor número de doadores ativos em território nacional.",
    systemRole: "Alertas de alta sensibilidade para mobilização de doadores raros.",
  },
};

export default function HomePage() {
  const { isAuthenticated, roles } = useAuth();
  const [selectedBloodType, setSelectedBloodType] = useState<BloodTypeKey>("O-");

  const dashboardPath = resolvePostLoginPath(roles);
  const canDonateTo = DONATE_TO_MAP[selectedBloodType];
  const canReceiveFrom = RECEIVE_FROM_MAP[selectedBloodType];
  const currentDetail = BLOOD_TYPE_DETAILS[selectedBloodType];

  return (
    <div className="min-h-screen bg-surface font-body text-on-surface flex flex-col">
      {/* Top Banner for authenticated users */}
      {isAuthenticated && (
        <div className="bg-primary text-on-primary py-2 px-4 text-center text-sm font-medium flex items-center justify-center gap-3 shadow-inner">
          <span className="material-symbols-outlined text-lg">account_circle</span>
          <span>Você já está conectado à sua conta.</span>
          <Link
            to={dashboardPath}
            className="underline font-bold hover:text-red-100 transition-colors inline-flex items-center gap-1"
          >
            Ir para a Central do Doador
            <span className="material-symbols-outlined text-sm">arrow_forward</span>
          </Link>
        </div>
      )}

      {/* Header / Navbar */}
      <header className="sticky top-0 z-40 glass-panel border-b border-gray-200/60 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 sm:h-20 flex items-center justify-between gap-2">
          <Link to="/" className="flex items-center gap-2 sm:gap-3 group shrink-0">
            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl sm:rounded-2xl bg-pulse-gradient flex items-center justify-center shadow-md group-hover:scale-105 transition-transform">
              <span className="material-symbols-outlined text-white text-xl sm:text-2xl">bloodtype</span>
            </div>
            <div className="flex flex-col">
              <span className="text-xl sm:text-2xl font-extrabold headline-font tracking-tight text-primary">
                BloodMatch
              </span>
              <span className="text-[9px] sm:text-[10px] uppercase font-bold tracking-widest text-secondary -mt-1">
                Conectando Vidas
              </span>
            </div>
          </Link>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center space-x-8 font-medium text-sm text-gray-700">
            <a href="#como-funciona" className="hover:text-primary transition-colors">
              Como Funciona
            </a>
            <a href="#quem-pode-doar" className="hover:text-primary transition-colors">
              Quem pode doar
            </a>
            <a href="#compatibilidade" className="hover:text-primary transition-colors">
              Compatibilidade
            </a>
            <a href="#vantagens" className="hover:text-primary transition-colors">
              Por Que Doar
            </a>
            <a href="#outras-causas" className="hover:text-primary transition-colors font-semibold text-primary">
              Outras Causas
            </a>
          </nav>

          {/* Action Buttons: Login & Register */}
          <div className="flex items-center gap-1.5 sm:gap-3 shrink-0">
            <Link
              to="/login"
              id="home-login-btn"
              className="px-3 py-2 sm:px-5 sm:py-2.5 rounded-xl font-semibold text-xs sm:text-sm text-primary hover:bg-red-50 border border-transparent hover:border-red-200 transition-all duration-200"
            >
              Entrar
            </Link>
            <Link
              to="/register"
              id="home-register-btn"
              className="px-3 py-2 sm:px-5 sm:py-2.5 rounded-xl font-semibold text-xs sm:text-sm text-white bg-pulse-gradient shadow-md hover:shadow-lg hover:opacity-95 transition-all duration-200 flex items-center gap-1 sm:gap-2 whitespace-nowrap"
            >
              <span>Cadastrar</span>
              <span className="hidden sm:inline">-se</span>
              <span className="material-symbols-outlined text-base sm:text-lg">person_add</span>
            </Link>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-grow">
        {/* Hero Section */}
        <section className="relative overflow-hidden pt-8 pb-16 lg:pt-20 lg:pb-28 bg-gradient-to-b from-red-50/50 via-surface to-surface">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid lg:grid-cols-12 gap-8 lg:gap-12 items-center">
              {/* Left Column Text */}
              <div className="lg:col-span-7 space-y-5 text-center lg:text-left">
                <div className="inline-flex items-center justify-center gap-1.5 px-3 py-1.5 sm:px-4 sm:py-2 rounded-full bg-red-100/90 border border-red-200 text-primary text-[11px] sm:text-xs font-bold uppercase tracking-wider shadow-xs max-w-full">
                  <span className="material-symbols-outlined text-sm sm:text-base shrink-0">favorite</span>
                  <span className="truncate sm:whitespace-normal">Plataforma Inteligente de Doação de Sangue</span>
                </div>

                <h1 className="text-3xl sm:text-5xl lg:text-6xl font-extrabold headline-font text-on-surface leading-tight sm:leading-[1.15]">
                  Sua doação faz o sangue certo chegar a quem{" "}
                  <span className="inline-block text-primary underline decoration-red-300 decoration-wavy underline-offset-4 sm:underline-offset-8">
                    mais precisa
                  </span>.
                </h1>

                <p className="text-sm sm:text-lg text-secondary max-w-2xl mx-auto lg:mx-0 font-normal leading-relaxed">
                  O BloodMatch conecta doadores voluntários diretamente a solicitações urgentes de sangue e hemocentros. Receba alertas de compatibilidade, agende doações e acompanhe o impacto real da sua solidariedade.
                </p>

                {/* Hero CTAs */}
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-center lg:justify-start gap-3 sm:gap-4 pt-2">
                  <Link
                    to="/register"
                    className="w-full sm:w-auto px-6 py-3.5 sm:px-8 sm:py-4 rounded-xl font-bold text-white bg-pulse-gradient shadow-lg hover:shadow-red-500/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 text-sm sm:text-base"
                  >
                    <span>Quero Ser Doador</span>
                    <span className="material-symbols-outlined text-lg">arrow_forward</span>
                  </Link>

                  <Link
                    to="/login"
                    className="w-full sm:w-auto px-6 py-3.5 sm:px-8 sm:py-4 rounded-xl font-bold text-gray-800 bg-white border border-gray-200 shadow-sm hover:bg-gray-50 hover:border-gray-300 transition-all flex items-center justify-center gap-2 text-sm sm:text-base"
                  >
                    <span className="material-symbols-outlined text-primary text-lg">login</span>
                    <span>Já Tenho Conta</span>
                  </Link>
                </div>

                {/* Trust Badges */}
                <div className="pt-6 border-t border-gray-200/80 grid grid-cols-3 gap-2 sm:gap-4 text-center lg:text-left">
                  <div className="bg-white/60 sm:bg-transparent p-2 sm:p-0 rounded-xl">
                    <p className="text-xl sm:text-2xl lg:text-3xl font-extrabold headline-font text-primary">100%</p>
                    <p className="text-[10px] sm:text-xs text-secondary font-medium leading-tight mt-0.5">Voluntário & Gratuito</p>
                  </div>
                  <div className="bg-white/60 sm:bg-transparent p-2 sm:p-0 rounded-xl">
                    <p className="text-xl sm:text-2xl lg:text-3xl font-extrabold headline-font text-primary">Em Tempo Real</p>
                    <p className="text-[10px] sm:text-xs text-secondary font-medium leading-tight mt-0.5">Alertas Urgentes</p>
                  </div>
                  <div className="bg-white/60 sm:bg-transparent p-2 sm:p-0 rounded-xl">
                    <p className="text-xl sm:text-2xl lg:text-3xl font-extrabold headline-font text-primary">ABO / Rh</p>
                    <p className="text-[10px] sm:text-xs text-secondary font-medium leading-tight mt-0.5">Match Inteligente</p>
                  </div>
                </div>
              </div>

              {/* Right Column Interactive Compatibility Simulator Card */}
              <div id="compatibilidade" className="lg:col-span-5 relative scroll-mt-24">
                <div className="relative mx-auto max-w-md lg:max-w-none">
                  {/* Decorative background glow */}
                  <div className="absolute -top-6 -left-6 w-72 h-72 bg-red-400/20 rounded-full blur-3xl pointer-events-none" />
                  <div className="absolute -bottom-6 -right-6 w-72 h-72 bg-red-600/10 rounded-full blur-3xl pointer-events-none" />

                  {/* Interactive Compatibility Card */}
                  <div className="relative bg-white/95 backdrop-blur-xl rounded-3xl p-6 sm:p-7 shadow-2xl border border-white/60 space-y-5">
                    {/* Header */}
                    <div className="flex items-center justify-between gap-2 pb-3 border-b border-gray-100">
                      <div>
                        <div className="flex items-center gap-1.5 text-xs font-bold text-primary uppercase tracking-wider">
                          <span className="material-symbols-outlined text-base">analytics</span>
                          <span>Simulador de Compatibilidade</span>
                        </div>
                        <h3 className="font-extrabold text-base text-gray-900 mt-0.5">
                          Selecione seu Tipo Sanguíneo:
                        </h3>
                      </div>
                      <span className="px-2.5 py-1 text-[11px] font-bold rounded-full bg-red-50 text-primary border border-red-100 shrink-0">
                        {currentDetail.rarityBrazil}
                      </span>
                    </div>

                    {/* Interactive Blood Type Selector Pills */}
                    <div className="grid grid-cols-4 gap-1.5">
                      {BLOOD_TYPES.map((bt) => {
                        const isSelected = selectedBloodType === bt;
                        return (
                          <button
                            key={bt}
                            onClick={() => setSelectedBloodType(bt)}
                            type="button"
                            className={`py-2 rounded-xl font-black text-xs transition-all duration-200 flex items-center justify-center gap-1 ${
                              isSelected
                                ? "bg-primary text-white shadow-md scale-105 ring-2 ring-red-300"
                                : "bg-gray-100/80 text-gray-700 hover:bg-red-50 hover:text-primary"
                            }`}
                          >
                            <span>{bt}</span>
                          </button>
                        );
                      })}
                    </div>

                    {/* Selected Blood Type Main Showcase */}
                    <div className="bg-surface-container-low rounded-2xl p-4 space-y-3.5 border border-gray-100">
                      <div className="flex items-start gap-3">
                        <div className="w-12 h-12 rounded-2xl bg-pulse-gradient text-white flex items-center justify-center font-black text-xl shadow-md shrink-0">
                          {selectedBloodType}
                        </div>
                        <div className="space-y-1">
                          <span className={`inline-block px-2.5 py-0.5 text-[10px] font-extrabold rounded-full border ${currentDetail.badgeClass}`}>
                            {currentDetail.badge}
                          </span>
                          <p className="text-xs text-gray-600 font-medium leading-tight">
                            {currentDetail.description}
                          </p>
                        </div>
                      </div>

                      {/* Coverage Progress Bar */}
                      <div className="space-y-1.5 pt-1">
                        <div className="flex justify-between items-center text-[11px] font-bold text-gray-700">
                          <span>Alcance Transfusional na População</span>
                          <span className="text-primary font-black">{currentDetail.coveragePercent}%</span>
                        </div>
                        <div className="w-full bg-gray-200/80 rounded-full h-2 overflow-hidden">
                          <div
                            className="bg-primary h-2 rounded-full transition-all duration-500"
                            style={{ width: `${currentDetail.coveragePercent}%` }}
                          />
                        </div>
                      </div>

                      {/* System Role Note */}
                      <div className="flex items-start gap-2 text-[11px] text-gray-600 bg-white/80 p-2.5 rounded-xl border border-gray-200/60">
                        <span className="material-symbols-outlined text-primary text-sm shrink-0 mt-0.5">verified</span>
                        <span className="leading-tight">{currentDetail.systemRole}</span>
                      </div>
                    </div>

                    {/* Compatibility Quick Grid */}
                    <div className="grid grid-cols-2 gap-3 text-xs">
                      {/* Can donate to */}
                      <div className="p-3 bg-red-50/60 rounded-2xl border border-red-100/80 space-y-1.5">
                        <span className="text-[10px] font-bold uppercase text-primary tracking-wider block">
                          Pode Doar Para ({canDonateTo.length})
                        </span>
                        <div className="flex flex-wrap gap-1">
                          {canDonateTo.map((bt) => (
                            <span
                              key={bt}
                              className="px-2 py-0.5 rounded-md bg-white text-primary font-extrabold text-[11px] border border-red-200 shadow-xs"
                            >
                              {bt}
                            </span>
                          ))}
                        </div>
                      </div>

                      {/* Can receive from */}
                      <div className="p-3 bg-emerald-50/60 rounded-2xl border border-emerald-100/80 space-y-1.5">
                        <span className="text-[10px] font-bold uppercase text-emerald-700 tracking-wider block">
                          Pode Receber De ({canReceiveFrom.length})
                        </span>
                        <div className="flex flex-wrap gap-1">
                          {canReceiveFrom.map((bt) => (
                            <span
                              key={bt}
                              className="px-2 py-0.5 rounded-md bg-white text-emerald-800 font-extrabold text-[11px] border border-emerald-200 shadow-xs"
                            >
                              {bt}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>

                    {/* CTA Button */}
                    <Link
                      to="/register"
                      className="w-full text-center py-3 bg-gray-900 hover:bg-black text-white font-bold text-xs sm:text-sm rounded-xl transition-all shadow-md flex items-center justify-center gap-2 group"
                    >
                      <span>Cadastrar como Doador {selectedBloodType}</span>
                      <span className="material-symbols-outlined text-base group-hover:translate-x-1 transition-transform">
                        arrow_forward
                      </span>
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Section: Como Funciona */}
        <section id="como-funciona" className="py-20 bg-surface">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-3xl mx-auto mb-16">
              <h2 className="text-xs font-bold text-primary uppercase tracking-widest mb-2">Simples & Rápido</h2>
              <p className="text-3xl sm:text-4xl font-extrabold headline-font text-on-surface">
                Como o BloodMatch funciona para você
              </p>
              <p className="mt-4 text-base text-secondary">
                Simplificamos cada etapa para que doadores voluntários possam responder rapidamente a pedidos de doação.
              </p>
            </div>

            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
              {/* Step 1 */}
              <div className="bg-white rounded-2xl p-6 border border-gray-200/70 shadow-sm hover:shadow-md transition-all relative group">
                <div className="w-12 h-12 rounded-xl bg-red-50 text-primary font-extrabold text-xl flex items-center justify-center mb-6 group-hover:bg-primary group-hover:text-white transition-colors">
                  01
                </div>
                <h3 className="text-lg font-bold text-gray-900 mb-2">Crie seu Perfil</h3>
                <p className="text-sm text-secondary leading-relaxed">
                  Cadastre-se informando seu tipo sanguíneo (ABO/Rh) e sua cidade para conectar-se aos hemocentros locais.
                </p>
              </div>

              {/* Step 2 */}
              <div className="bg-white rounded-2xl p-6 border border-gray-200/70 shadow-sm hover:shadow-md transition-all relative group">
                <div className="w-12 h-12 rounded-xl bg-red-50 text-primary font-extrabold text-xl flex items-center justify-center mb-6 group-hover:bg-primary group-hover:text-white transition-colors">
                  02
                </div>
                <h3 className="text-lg font-bold text-gray-900 mb-2">Receba Alertas</h3>
                <p className="text-sm text-secondary leading-relaxed">
                  Quando um hospital ou hemocentro precisar de doações do seu tipo, você será notificado em tempo real.
                </p>
              </div>

              {/* Step 3 */}
              <div className="bg-white rounded-2xl p-6 border border-gray-200/70 shadow-sm hover:shadow-md transition-all relative group">
                <div className="w-12 h-12 rounded-xl bg-red-50 text-primary font-extrabold text-xl flex items-center justify-center mb-6 group-hover:bg-primary group-hover:text-white transition-colors">
                  03
                </div>
                <h3 className="text-lg font-bold text-gray-900 mb-2">Agende a Doação</h3>
                <p className="text-sm text-secondary leading-relaxed">
                  Escolha o melhor horário e localização no hemocentro parceiro para realizar sua doação com segurança.
                </p>
              </div>

              {/* Step 4 */}
              <div className="bg-white rounded-2xl p-6 border border-gray-200/70 shadow-sm hover:shadow-md transition-all relative group">
                <div className="w-12 h-12 rounded-xl bg-red-50 text-primary font-extrabold text-xl flex items-center justify-center mb-6 group-hover:bg-primary group-hover:text-white transition-colors">
                  04
                </div>
                <h3 className="text-lg font-bold text-gray-900 mb-2">Acompanhe seu Impacto</h3>
                <p className="text-sm text-secondary leading-relaxed">
                  Registre suas doações, veja o número de vidas impactadas e mantenha seu histórico de doador atualizado.
                </p>
              </div>
            </div>
          </div>
        </section>

        <EligibilitySection />

        {/* Section: Vantagens */}
        <section id="vantagens" className="py-20 bg-surface">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-3xl mx-auto mb-16">
              <h2 className="text-xs font-bold text-primary uppercase tracking-widest mb-2">Por Que Usar</h2>
              <p className="text-3xl sm:text-4xl font-extrabold headline-font text-on-surface">
                Por que se cadastrar no BloodMatch?
              </p>
              <p className="mt-4 text-base text-secondary">
                Tecnologia voltada à preservação da vida e apoio direto à rede hospitalar e de doadores.
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-8">
              <div className="bg-white p-8 rounded-3xl border border-gray-200/80 shadow-sm space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-red-50 text-primary flex items-center justify-center text-2xl font-bold">
                  <span className="material-symbols-outlined">notifications_active</span>
                </div>
                <h3 className="text-xl font-bold text-gray-900">Alertas Sem Spam</h3>
                <p className="text-sm text-secondary leading-relaxed">
                  Você só recebe notificações quando realmente houver solicitações compatíveis com seu tipo sanguíneo e região.
                </p>
              </div>

              <div className="bg-white p-8 rounded-3xl border border-gray-200/80 shadow-sm space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center text-2xl font-bold">
                  <span className="material-symbols-outlined">verified_user</span>
                </div>
                <h3 className="text-xl font-bold text-gray-900">Segurança de Dados</h3>
                <p className="text-sm text-secondary leading-relaxed">
                  Seus dados pessoais e de saúde são protegidos com sigilo e usados exclusivamente para conexões de doação.
                </p>
              </div>

              <div className="bg-white p-8 rounded-3xl border border-gray-200/80 shadow-sm space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center text-2xl font-bold">
                  <span className="material-symbols-outlined">military_tech</span>
                </div>
                <h3 className="text-xl font-bold text-gray-900">Gamificação & Medalhas</h3>
                <p className="text-sm text-secondary leading-relaxed">
                  Acompanhe seu histórico de doações, ganhe conquistas e visualize diretamente quantas vidas você ajudou a salvar.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Section: Outras Causas Informativo */}
        <OtherCausesSection />

        {/* Section: Call to Action Banner */}
        <section className="py-16 bg-gradient-to-r from-red-800 via-primary to-red-700 text-white relative overflow-hidden">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10 text-center space-y-6">
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold headline-font tracking-tight">
              Pronto para salvar vidas com apenas alguns cliques?
            </h2>
            <p className="text-red-100 max-w-2xl mx-auto text-base sm:text-lg">
              Cadastre-se gratuitamente como doador ou faça login para acessar suas solicitações e agendamentos.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
              <Link
                to="/register"
                className="w-full sm:w-auto px-8 py-4 rounded-xl font-bold text-primary bg-white hover:bg-gray-100 transition-all shadow-lg text-base flex items-center justify-center gap-2"
              >
                <span>Criar Conta de Doador</span>
                <span className="material-symbols-outlined">person_add</span>
              </Link>
              <Link
                to="/login"
                className="w-full sm:w-auto px-8 py-4 rounded-xl font-bold text-white border-2 border-white/80 hover:bg-white/10 transition-all text-base flex items-center justify-center gap-2"
              >
                <span>Fazer Login</span>
                <span className="material-symbols-outlined">login</span>
              </Link>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-400 py-12 border-t border-gray-800 text-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          <div className="space-y-4">
            <div className="flex items-center gap-2 text-white font-extrabold text-xl headline-font">
              <span className="material-symbols-outlined text-primary text-2xl">bloodtype</span>
              BloodMatch
            </div>
            <p className="text-xs leading-relaxed text-gray-400">
              Conectando doadores de sangue e hemocentros para salvar vidas em todo o país.
            </p>
          </div>

          <div>
            <h4 className="text-white font-bold text-sm mb-3">Navegação</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <a href="#como-funciona" className="hover:text-white transition-colors">
                  Como Funciona
                </a>
              </li>
              <li>
                <a href="#quem-pode-doar" className="hover:text-white transition-colors">
                  Quem pode doar
                </a>
              </li>
              <li>
                <a href="#compatibilidade" className="hover:text-white transition-colors">
                  Compatibilidade ABO/Rh
                </a>
              </li>
              <li>
                <a href="#vantagens" className="hover:text-white transition-colors">
                  Por Que Doar
                </a>
              </li>
              <li>
                <a href="#outras-causas" className="hover:text-white transition-colors">
                  Outras Causas (Aférese, Medula, Órgãos)
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="text-white font-bold text-sm mb-3">Acesso</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/login" className="hover:text-white transition-colors">
                  Área de Login
                </Link>
              </li>
              <li>
                <Link to="/register" className="hover:text-white transition-colors">
                  Cadastro de Doador
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="text-white font-bold text-sm mb-3">Contato & Suporte</h4>
            <p className="text-xs text-gray-400 leading-relaxed mb-2">
              Dúvidas ou apoio técnico para hemocentros?
            </p>
            <a
              href="mailto:contato@bloodmatch.org"
              className="text-xs text-red-400 font-bold hover:underline inline-flex items-center gap-1"
            >
              <span className="material-symbols-outlined text-sm">mail</span>
              contato@bloodmatch.org
            </a>
          </div>
        </div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 border-t border-gray-800 text-center text-xs text-gray-500">
          <p>© {new Date().getFullYear()} BloodMatch. Todos os direitos reservados. Projeto social de incentivo à doação de sangue.</p>
        </div>
      </footer>
    </div>
  );
}
