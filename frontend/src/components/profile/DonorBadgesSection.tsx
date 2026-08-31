import { useMemo, useState } from "react";
import { ProfileSection } from "./ProfileSection";

type DonorBadgesSectionProps = {
  bloodType: string | null;
  livesImpacted: number | null;
  daysRemaining: number | null;
  lastDonationDate: string | null;
};

export type BadgeItem = {
  id: string;
  title: string;
  category: string;
  description: string;
  icon: string;
  isUnlocked: boolean;
  progressText: string;
  colorClass: string;
};

export function DonorBadgesSection({
  bloodType,
  livesImpacted,
  daysRemaining,
  lastDonationDate,
}: DonorBadgesSectionProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  const badges = useMemo<BadgeItem[]>(() => {
    const hasDonation = !!lastDonationDate || (typeof livesImpacted === "number" && livesImpacted > 0);
    const lives = livesImpacted ?? 0;
    const canDonateNow = (daysRemaining ?? 0) <= 0;
    const isUniversal = bloodType === "O-";
    const hasBloodType = !!bloodType;

    return [
      {
        id: "first_donation",
        title: "Doador Estreante",
        category: "Marco Inicial",
        description: "Realizou a sua primeira doação de sangue cadastrada no sistema.",
        icon: "social_leaderboard",
        isUnlocked: hasDonation,
        progressText: hasDonation ? "Conquistado!" : "Aguardando 1ª doação",
        colorClass: "from-amber-500 to-orange-600",
      },
      {
        id: "ready_now",
        title: "Pronto para Salvar",
        category: "Status",
        description: "Está com a janela de intervalo de descanso concluída e elegível para doar.",
        icon: "ecg_heart",
        isUnlocked: canDonateNow,
        progressText: canDonateNow ? "Elegível Hoje!" : `Faltam ${daysRemaining} dias`,
        colorClass: "from-emerald-500 to-teal-600",
      },
      {
        id: "hero_guardian",
        title: "Guardião da Vida",
        category: "Impacto Vital",
        description: "Ajudou a salvar 4 ou mais vidas através de suas doações realizadas.",
        icon: "shield_person",
        isUnlocked: lives >= 4,
        progressText: lives >= 4 ? "Conquistado!" : `${Math.min(lives, 4)}/4 vidas`,
        colorClass: "from-blue-500 to-indigo-600",
      },
      {
        id: "configured_profile",
        title: "Perfil Campeão",
        category: "Cadastro",
        description: "Configurou tipo sanguíneo e informações completas no perfil.",
        icon: "workspace_premium",
        isUnlocked: hasBloodType,
        progressText: hasBloodType ? "Perfil Verificado" : "Configure seu tipo",
        colorClass: "from-purple-500 to-violet-700",
      },
      {
        id: "universal_donor",
        title: "Salva-Vidas O-",
        category: "Raridade",
        description: "Possui tipo sanguíneo O-, o doador universal essencial para emergências.",
        icon: "award_star",
        isUnlocked: isUniversal,
        progressText: isUniversal ? "Tipo Universal O-" : "Requer Sangue O-",
        colorClass: "from-red-500 to-rose-700",
      },
      {
        id: "marathon",
        title: "Maratona de Doações",
        category: "Engajamento",
        description: "Alcançou a marca de 3 ou mais doações ou 12 vidas impactadas.",
        icon: "military_tech",
        isUnlocked: lives >= 12,
        progressText: lives >= 12 ? "Conquistado!" : `${Math.min(lives, 12)}/12 vidas`,
        colorClass: "from-amber-400 to-yellow-600",
      },
      {
        id: "gold_donor",
        title: "Doador de Ouro",
        category: "Nível Avançado",
        description: "Acançou o marco extraordinário de 20 ou mais vidas impactadas.",
        icon: "stars",
        isUnlocked: lives >= 20,
        progressText: lives >= 20 ? "Conquistado!" : `${Math.min(lives, 20)}/20 vidas`,
        colorClass: "from-yellow-400 to-amber-600",
      },
      {
        id: "frequent_hero",
        title: "Doador Frequente",
        category: "Constância",
        description: "Mantém hábito de doação regular dentro do intervalo recomendado.",
        icon: "published_with_changes",
        isUnlocked: hasDonation && canDonateNow,
        progressText: hasDonation && canDonateNow ? "Ativo" : "Aguardando janela",
        colorClass: "from-teal-500 to-emerald-700",
      },
      {
        id: "external_hero",
        title: "Anjo Solidário",
        category: "Ação Externa",
        description: "Registrou comprovante de doação externa realizada em hemocentro.",
        icon: "volunteer_activism",
        isUnlocked: hasDonation,
        progressText: hasDonation ? "Comprovado" : "Pendente",
        colorClass: "from-rose-500 to-pink-600",
      },
      {
        id: "critical_responder",
        title: "Socorrista Noturno",
        category: "Emergência",
        description: "Respondeu com prontidão a pedidos de doação em Urgência Crítica.",
        icon: "bolt",
        isUnlocked: lives >= 4,
        progressText: lives >= 4 ? "Prontidão Total" : "Aguardando alerta",
        colorClass: "from-cyan-500 to-blue-700",
      },
      {
        id: "community_guardian",
        title: "Protetor da Comunidade",
        category: "Solidariedade",
        description: "Membro ativo no mapa e na rede comunitária de doação de sangue.",
        icon: "diversity_3",
        isUnlocked: hasBloodType && hasDonation,
        progressText: hasBloodType && hasDonation ? "Membro Ativo" : "Pendente",
        colorClass: "from-indigo-500 to-purple-700",
      },
      {
        id: "legendary_donor",
        title: "Lenda BloodMatch",
        category: "Hall da Fama",
        description: "Alcançou o topo da liderança de doadores da comunidade.",
        icon: "crown",
        isUnlocked: lives >= 40,
        progressText: lives >= 40 ? "Lenda Viga!" : `${Math.min(lives, 40)}/40 vidas`,
        colorClass: "from-[#ae131a] to-[#71090e]",
      },
    ];
  }, [bloodType, livesImpacted, daysRemaining, lastDonationDate]);

  const unlockedCount = badges.filter((b) => b.isUnlocked).length;
  const visibleBadges = isExpanded ? badges : badges.slice(0, 4);

  return (
    <ProfileSection
      icon="military_tech"
      title="Conquistas & Níveis de Doador"
      description={`Você conquistou ${unlockedCount} de ${badges.length} selos de impacto comunitário.`}
    >
      <div className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
          {visibleBadges.map((badge) => (
            <div
              key={badge.id}
              className={`relative flex flex-col justify-between rounded-2xl border p-4 transition-all ${
                badge.isUnlocked
                  ? "bg-white border-[#f5c6c2]/60 shadow-xs hover:shadow-md hover:-translate-y-0.5"
                  : "bg-surface-container-low border-surface-container-high opacity-70"
              }`}
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <div
                    className={`flex h-10 w-10 items-center justify-center rounded-xl text-white shadow-xs ${
                      badge.isUnlocked
                        ? `bg-gradient-to-br ${badge.colorClass}`
                        : "bg-gray-300 text-gray-500"
                    }`}
                  >
                    <span className="material-symbols-outlined text-xl">{badge.icon}</span>
                  </div>

                  <span
                    className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[9px] font-extrabold uppercase tracking-wider ${
                      badge.isUnlocked
                        ? "bg-[#fff2f0] text-primary border border-[#f5c6c2]"
                        : "bg-surface-container-high text-secondary"
                    }`}
                  >
                    {badge.isUnlocked ? (
                      <>
                        <span className="material-symbols-outlined text-[10px]">check_circle</span>
                        {badge.progressText}
                      </>
                    ) : (
                      <>
                        <span className="material-symbols-outlined text-[10px]">lock</span>
                        {badge.progressText}
                      </>
                    )}
                  </span>
                </div>

                <h4 className="font-headline text-sm font-extrabold text-on-surface">
                  {badge.title}
                </h4>
                <p className="mt-1 text-xs text-text-secondary leading-relaxed">
                  {badge.description}
                </p>
              </div>

              <div className="mt-3 pt-2.5 border-t border-surface-container-low flex items-center justify-between text-[10px] font-bold text-secondary">
                <span>{badge.category}</span>
                {badge.isUnlocked && (
                  <span className="text-primary font-extrabold">Desbloqueado ★</span>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Botão de Expandir / Recolher */}
        {badges.length > 4 && (
          <div className="text-center pt-2">
            <button
              type="button"
              onClick={() => setIsExpanded((prev) => !prev)}
              className="inline-flex items-center gap-2 rounded-xl bg-surface-container-low border border-surface-container-high px-5 py-2.5 text-xs font-bold text-on-surface hover:bg-[#fff2f0] hover:text-primary hover:border-[#f5c6c2] transition-all shadow-xs"
            >
              <span className="material-symbols-outlined text-base">
                {isExpanded ? "expand_less" : "expand_more"}
              </span>
              {isExpanded
                ? "Recolher conquistas"
                : `Ver todas as ${badges.length} conquistas (+${badges.length - 4})`}
            </button>
          </div>
        )}
      </div>
    </ProfileSection>
  );
}
