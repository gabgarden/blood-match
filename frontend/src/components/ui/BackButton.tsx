import { useNavigate } from "react-router-dom";

type BackButtonProps = {
  fallbackPath?: string;
  label?: string;
  className?: string;
  variant?: "ghost" | "pill" | "outline";
};

export function BackButton({
  fallbackPath = "/dashboard",
  label = "Voltar",
  className = "",
  variant = "ghost",
}: BackButtonProps) {
  const navigate = useNavigate();

  const handleBack = () => {
    if (window.history.length > 2) {
      navigate(-1);
    } else {
      navigate(fallbackPath);
    }
  };

  const getVariantStyles = () => {
    switch (variant) {
      case "pill":
        return "px-3 py-1.5 rounded-full bg-surface-container-low hover:bg-red-50 text-secondary hover:text-primary border border-gray-200/60 shadow-xs";
      case "outline":
        return "px-3 py-1.5 rounded-xl border border-gray-200 hover:border-red-200 hover:bg-red-50 text-secondary hover:text-primary";
      case "ghost":
      default:
        return "p-1.5 rounded-xl text-secondary hover:text-primary hover:bg-surface-container-high";
    }
  };

  return (
    <button
      type="button"
      onClick={handleBack}
      className={`inline-flex items-center gap-1.5 text-xs font-bold transition-all duration-200 active:scale-95 ${getVariantStyles()} ${className}`}
      aria-label={label}
      title={label}
    >
      <span className="material-symbols-outlined text-lg">arrow_back</span>
      {label && <span className="hidden sm:inline">{label}</span>}
    </button>
  );
}
