import { Link } from "react-router-dom";
import { BackButton } from "../ui/BackButton";

type DonorDashboardTopbarProps = {
  title: string;
  onLogout: () => void;
  showBackButton?: boolean;
};

export function DonorDashboardTopbar({ title, onLogout, showBackButton = true }: DonorDashboardTopbarProps) {
  return (
    <header className="fixed top-0 left-0 right-0 z-30 flex h-16 items-center justify-between border-b border-surface-container-high bg-[#f9f9fb]/90 px-4 backdrop-blur-md lg:left-64 lg:px-8">
      <div className="flex items-center gap-2">
        {showBackButton && <BackButton variant="pill" fallbackPath="/dashboard" />}
        <h2 className="font-headline text-lg sm:text-xl font-bold text-primary truncate">{title}</h2>
      </div>

      <div className="flex items-center gap-2 lg:gap-3">
        <Link
          to="/profile"
          className="rounded-xl p-2 text-secondary transition-colors hover:bg-surface-container-high hover:text-primary"
          aria-label="Abrir meu perfil"
          title="Meu perfil"
        >
          <span className="material-symbols-outlined">person</span>
        </Link>

        <button
          type="button"
          onClick={onLogout}
          className="rounded-xl border border-surface-container-highest px-2.5 py-1.5 text-xs font-semibold text-secondary transition-colors hover:border-primary hover:text-primary lg:hidden"
        >
          Sair
        </button>
      </div>
    </header>
  );
}
