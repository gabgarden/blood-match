import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { resolvePostLoginPath } from "../routes/roleRouting";
import { OtherCausesSection } from "../components/OtherCausesSection";

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

export default function HomePage() {
  const { isAuthenticated, roles } = useAuth();
  const [selectedBloodType, setSelectedBloodType] = useState<BloodTypeKey>("O-");

  const dashboardPath = resolvePostLoginPath(roles);
  const canDonateTo = DONATE_TO_MAP[selectedBloodType];
  const canReceiveFrom = RECEIVE_FROM_MAP[selectedBloodType];

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

              {/* Right Column Graphic / Cards */}
              <div className="lg:col-span-5 relative">
                <div className="relative mx-auto max-w-md lg:max-w-none">
                  {/* Decorative background glow */}
                  <div className="absolute -top-6 -left-6 w-72 h-72 bg-red-400/20 rounded-full blur-3xl pointer-events-none" />
                  <div className="absolute -bottom-6 -right-6 w-72 h-72 bg-red-600/10 rounded-full blur-3xl pointer-events-none" />

                  {/* Glass Card Showcase */}
                  <div className="relative bg-white/90 backdrop-blur-xl rounded-3xl p-6 sm:p-8 shadow-2xl border border-white/60 space-y-6">
                    <div className="flex items-center justify-between pb-4 border-b border-gray-100">
                      <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-2xl bg-red-100 text-primary flex items-center justify-center font-bold text-xl">
                          O-
                        </div>
                        <div>
                          <h3 className="font-bold text-gray-900">Doador Universal</h3>
                          <p className="text-xs text-gray-500">Compatível com todos os grupos</p>
                        </div>
                      </div>
                      <span className="px-3 py-1 bg-red-50 text-primary text-xs font-extrabold rounded-full animate-pulse">
                        Urgência Alta
                      </span>
                    </div>

                    <div className="bg-surface-container-low rounded-2xl p-4 space-y-3">
                      <div className="flex justify-between items-center text-xs font-semibold text-gray-600">
                        <span>Status dos Estoques de Sangue</span>
                        <span className="text-red-600 font-bold">Nível Crítico</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2.5 overflow-hidden">
                        <div className="bg-red-600 h-2.5 rounded-full w-[35%]" />
                      </div>
                      <p className="text-[11px] text-gray-500 flex items-center gap-1">
                        <span className="material-symbols-outlined text-sm text-red-500">warning</span>
                        Hemocentros da região necessitam doações O- e A- esta semana.
                      </p>
                    </div>

                    <div className="space-y-3 pt-2">
                      <div className="flex items-center gap-3 p-3 bg-red-50/50 rounded-xl border border-red-100">
                        <span className="material-symbols-outlined text-primary">notifications_active</span>
                        <div className="text-xs">
                          <p className="font-bold text-gray-900">Notificações Diretas</p>
                          <p className="text-gray-600">Receba alertas quando seu sangue puder salvar alguém.</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3 p-3 bg-blue-50/50 rounded-xl border border-blue-100">
                        <span className="material-symbols-outlined text-blue-600">verified</span>
                        <div className="text-xs">
                          <p className="font-bold text-gray-900">Validação Oficial</p>
                          <p className="text-gray-600">Registre doações e ganhe histórico verificado.</p>
                        </div>
                      </div>
                    </div>

                    <Link
                      to="/register"
                      className="block w-full text-center py-3 bg-gray-900 hover:bg-black text-white font-semibold text-sm rounded-xl transition-all shadow-md"
                    >
                      Cadastrar Tipo Sanguíneo
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

        {/* Interactive Section: Compatibilidade Sanguínea */}
        <section id="compatibilidade" className="py-20 bg-white border-y border-gray-200/60">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-3xl mx-auto mb-14">
              <h2 className="text-xs font-bold text-primary uppercase tracking-widest mb-2">
                Guia Biológico de Doação
              </h2>
              <p className="text-3xl sm:text-4xl font-extrabold headline-font text-on-surface">
                Descubra com quem você é compatível
              </p>
              <p className="mt-4 text-base text-secondary">
                Clique no seu tipo sanguíneo abaixo para ver para quem você pode doar e de quem pode receber sangue.
              </p>
            </div>

            {/* Selector Grid */}
            <div className="flex flex-wrap justify-center gap-3 mb-10">
              {BLOOD_TYPES.map((bt) => {
                const isSelected = selectedBloodType === bt;
                return (
                  <button
                    key={bt}
                    onClick={() => setSelectedBloodType(bt)}
                    className={`px-5 py-3 rounded-2xl font-extrabold text-base transition-all duration-200 flex items-center gap-1.5 shadow-xs ${
                      isSelected
                        ? "bg-primary text-white scale-110 shadow-md ring-4 ring-red-100"
                        : "bg-surface-container-low text-gray-700 hover:bg-red-50 hover:text-primary"
                    }`}
                  >
                    <span className="material-symbols-outlined text-lg">water_drop</span>
                    <span>{bt}</span>
                  </button>
                );
              })}
            </div>

            {/* Results Display */}
            <div className="max-w-4xl mx-auto grid md:grid-cols-2 gap-8">
              {/* Can donate to */}
              <div className="bg-surface p-6 sm:p-8 rounded-3xl border border-gray-200/80 shadow-sm space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-red-100 text-primary flex items-center justify-center">
                    <span className="material-symbols-outlined">output</span>
                  </div>
                  <div>
                    <h3 className="font-bold text-lg text-gray-900">Pode Doar Para</h3>
                    <p className="text-xs text-gray-500">Receptores compatíveis para {selectedBloodType}</p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2 pt-2">
                  {canDonateTo.map((bt) => (
                    <span
                      key={bt}
                      className="px-4 py-2 rounded-xl bg-white border border-red-200 text-primary font-bold text-sm shadow-xs"
                    >
                      {bt}
                    </span>
                  ))}
                </div>

                {selectedBloodType === "O-" && (
                  <p className="text-xs text-red-700 font-semibold bg-red-50 p-3 rounded-xl border border-red-100">
                    💡 O- é o doador universal de hemácias! Seu sangue pode ser transfundido em qualquer pessoa em situações de emergência.
                  </p>
                )}
              </div>

              {/* Can receive from */}
              <div className="bg-surface p-6 sm:p-8 rounded-3xl border border-gray-200/80 shadow-sm space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
                    <span className="material-symbols-outlined">input</span>
                  </div>
                  <div>
                    <h3 className="font-bold text-lg text-gray-900">Pode Receber De</h3>
                    <p className="text-xs text-gray-500">Doadores compatíveis para {selectedBloodType}</p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2 pt-2">
                  {canReceiveFrom.map((bt) => (
                    <span
                      key={bt}
                      className="px-4 py-2 rounded-xl bg-white border border-emerald-200 text-emerald-800 font-bold text-sm shadow-xs"
                    >
                      {bt}
                    </span>
                  ))}
                </div>

                {selectedBloodType === "AB+" && (
                  <p className="text-xs text-emerald-700 font-semibold bg-emerald-50 p-3 rounded-xl border border-emerald-100">
                    💡 AB+ é o receptor universal! Pode receber sangue de qualquer tipo ABO/Rh com segurança.
                  </p>
                )}
              </div>
            </div>
          </div>
        </section>

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
