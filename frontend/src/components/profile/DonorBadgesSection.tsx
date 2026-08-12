import { useMemo } from "react";
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
  const badges = useMemo<BadgeItem[]>(() => {
    const hasDonation = !!lastDonationDate || (typeof livesImpacted === "number" && livesImpacted > 0);
    const lives = livesImpacted ?? 0;
    const canDonateNow = (daysRemaining ?? 0) <= 0;
    const isUniversal = bloodType === "O-";

    return [
      {
        id: "first_donation",
        title: "Doador Estreante",
        category: "Março Inicial",
        description: "Realizou a sua primeira doação de sangue cadastrada.",
        icon: "social_leaderboard",
        isUnlocked: hasDonation,
        progressText: hasDonation ? "Conquistado!" : "Aguardando 1ª doação",
        colorClass: "from-amber-500 to-orange-600",
      },
      {
        id: "universal_donor",
        title: "Salva-Vidas O-",
        category: "Raridade",
        description: "Possui tipo sanguíneo O-, o doador universal de emergências.",
        icon: "award_star",
        isUnlocked: isUniversal,
        progressText: isUniversal ? "Tipo Universal O-" : "Requer Sangue O-",
        colorClass: "from-red-500 to-rose-700",
      },
      {
        id: "marathon",
        title: "Maratona de Doações",
        category: "Engajamento",
        description: "Alcançou o marco de 3 ou mais doações ou 12 vidas impactadas.",
        icon: "military_tech",
        isUnlocked: lives >= 12,
        progressText: lives >= 12 ? "Conquistado!" : `${Math.min(lives, 12)}/12 vidas`,
        colorClass: "from-amber-400 to-yellow-600",
      },
      {
        id: "ready_now",
        title: "Pronto para Salvar",
        category: "Status",
        description: "Está com a janela de intervalo concluída e pronto para doar.",
        icon: "ecg_heart",
        isUnlocked: canDonateNow,
        progressText: canDonateNow ? "Elegível Hoje!" : `Faltam ${daysRemaining} dias`,
        colorClass: "from-emerald-500 to-teal-600",
      },
      {
        id: "hero_guardian",
        title: "Guardião da Vida",
        category: "Impacto Alto",
        description: "Ajudou a salvar 4 ou mais vidas através das suas doações.",
        icon: "shield_person",
        isUnlocked: lives >= 4,
        progressText: lives >= 4 ? "Conquistado!" : `${Math.min(lives, 4)}/4 vidas`,
        colorClass: "from-blue-500 to-indigo-600",
      },
      {
        id: "configured_profile",
        title: "Perfil Campeão",
        category: "Cadastro",
        description: "Configurou tipo sanguíneo e preferências completas de doação.",
        icon: "workspace_premium",
        isUnlocked: !!bloodType,
        progressText: bloodType ? "Perfil Verificado" : "Configure seu tipo",
        colorClass: "from-purple-500 to-violet-700",
      },
    ];
  }, [bloodType, livesImpacted, daysRemaining, lastDonationDate]);

  const unlockedCount = badges.filter((b) => b.isUnlocked).length;

  return (
    <ProfileSection
      icon="military_tech"
      title="Conquistas & Níveis de Doador"
      description={`Você conquistou ${unlockedCount} de ${badges.length} selos de impacto.`}
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
        {badges.map((badge) => (
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
                  className={`flex h-11 w-11 items-center justify-center rounded-xl text-white shadow-xs ${
                    badge.isUnlocked
                      ? `bg-gradient-to-br ${badge.colorClass}`
                      : "bg-gray-300 text-gray-500"
                  }`}
                >
                  <span className="material-symbols-outlined text-2xl">{badge.icon}</span>
                </div>

                <span
                  className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wider ${
                    badge.isUnlocked
                      ? "bg-[#fff2f0] text-primary border border-[#f5c6c2]"
                      : "bg-surface-container-high text-secondary"
                  }`}
                >
                  {badge.isUnlocked ? (
                    <>
                      <span className="material-symbols-outlined text-xs">check_circle</span>
                      {badge.progressText}
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-xs">lock</span>
                      {badge.progressText}
                    </>
                  )}
                </span>
              </div>

              <h4 className="font-headline text-base font-extrabold text-on-surface">
                {badge.title}
              </h4>
              <p className="mt-1 text-xs text-text-secondary leading-relaxed">
                {badge.description}
              </p>
            </div>

            <div className="mt-3 pt-2.5 border-t border-surface-container-low flex items-center justify-between text-[11px] font-bold text-secondary">
              <span>{badge.category}</span>
              {badge.isUnlocked && (
                <span className="text-primary font-extrabold">Desbloqueado ★</span>
              )}
            </div>
          </div>
        ))}
      </div>
    </ProfileSection>
  );
}
