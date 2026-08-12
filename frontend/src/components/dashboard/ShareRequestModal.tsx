import { useState } from "react";

type ShareRequestModalProps = {
  isOpen: boolean;
  onClose: () => void;
  hospitalName?: string;
  bloodType?: string;
  requestId?: string;
};

export function ShareRequestModal({
  isOpen,
  onClose,
  hospitalName = "Hemocentro de Campos",
  bloodType = "O-",
  requestId = "",
}: ShareRequestModalProps) {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const shareUrl = `${window.location.origin}/requests#${requestId}`;
  const whatsappMsg = `🚨 URGENTE: Precisamos de doadores de sangue ${bloodType} no ${hospitalName}. Saiba como doar pelo BloodMatch: ${shareUrl}`;
  const whatsappUrl = `https://api.whatsapp.com/send?text=${encodeURIComponent(whatsappMsg)}`;

  function handleCopyLink() {
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in">
      <div className="w-full max-w-md overflow-hidden rounded-[2.5rem] bg-white p-6 shadow-2xl border border-surface-container-high animate-in zoom-in-95">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-xl">share</span>
            <h3 className="font-headline text-lg font-extrabold text-on-surface">
              Compartilhar Campanha
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl p-1.5 text-text-secondary hover:bg-surface-container-low transition-colors"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Card em formato 9:16 Simulado para Stories / WhatsApp */}
        <div className="relative overflow-hidden rounded-3xl bg-pulse-gradient p-6 text-white shadow-xl">
          <div className="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-white/10 blur-xl" />
          <div className="absolute -left-10 bottom-0 h-32 w-32 rounded-full bg-white/10 blur-xl" />

          <div className="relative z-10 space-y-4">
            <div className="flex items-center justify-between">
              <span className="rounded-full bg-white/20 px-3 py-1 text-[10px] font-black uppercase tracking-wider text-white backdrop-blur-md">
                Campanha Urgente 🩸
              </span>
              <span className="font-headline text-xs font-extrabold opacity-90">BloodMatch</span>
            </div>

            <div className="py-2 text-center">
              <p className="text-xs font-semibold uppercase tracking-wider text-white/80">
                Precisamos do tipo sanguíneo
              </p>
              <div className="my-2 inline-flex h-20 w-20 items-center justify-center rounded-2xl bg-white text-primary shadow-2xl">
                <span className="font-headline text-4xl font-black">{bloodType}</span>
              </div>
              <h4 className="font-headline text-xl font-extrabold leading-snug">{hospitalName}</h4>
            </div>

            <div className="rounded-xl bg-black/20 p-3 text-center text-xs font-semibold backdrop-blur-md">
              Acesse o aplicativo para verificar compatibilidade e agendar sua doação.
            </div>
          </div>
        </div>

        {/* Ações de Compartilhamento */}
        <div className="mt-5 space-y-2.5">
          <a
            href={whatsappUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-600 px-5 py-3 text-sm font-bold text-white shadow-md hover:bg-emerald-700 transition-colors"
          >
            <span className="material-symbols-outlined text-lg">chat</span>
            Enviar no WhatsApp
          </a>

          <button
            type="button"
            onClick={handleCopyLink}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-surface-container-low border border-surface-container-high px-5 py-3 text-sm font-bold text-on-surface hover:bg-surface-container-high transition-colors"
          >
            <span className="material-symbols-outlined text-lg">content_copy</span>
            {copied ? "Link Copiado! ✓" : "Copiar Link Direto"}
          </button>
        </div>
      </div>
    </div>
  );
}
