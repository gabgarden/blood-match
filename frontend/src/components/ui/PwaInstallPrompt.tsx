import { useEffect, useState } from "react";

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed" }>;
}

function checkIsIos(): boolean {
  if (typeof window === "undefined") return false;
  const userAgent = window.navigator.userAgent.toLowerCase();
  return /iphone|ipad|ipod/.test(userAgent);
}

function checkIsStandalone(): boolean {
  if (typeof window === "undefined") return false;
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    ("standalone" in window.navigator && (window.navigator as unknown as { standalone: boolean }).standalone)
  );
}

export function PwaInstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [showPrompt, setShowPrompt] = useState(false);
  const [isIos] = useState(() => checkIsIos());
  const [showIosTip, setShowIosTip] = useState(() => {
    if (checkIsStandalone()) return false;
    const isDismissed = localStorage.getItem("bloodmatch_pwa_dismissed");
    return checkIsIos() && !isDismissed;
  });

  useEffect(() => {
    if (checkIsStandalone()) {
      return;
    }

    const isDismissed = localStorage.getItem("bloodmatch_pwa_dismissed");
    if (isDismissed) {
      return;
    }

    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      setShowPrompt(true);
    };

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);

    return () => {
      window.removeEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    };
  }, []);

  const handleInstallClick = async () => {
    if (!deferredPrompt) return;
    await deferredPrompt.prompt();
    const choiceResult = await deferredPrompt.userChoice;
    if (choiceResult.outcome === "accepted") {
      setShowPrompt(false);
    }
    setDeferredPrompt(null);
  };

  const handleDismiss = () => {
    setShowPrompt(false);
    setShowIosTip(false);
    localStorage.setItem("bloodmatch_pwa_dismissed", "true");
  };

  if (!showPrompt && !showIosTip) {
    return null;
  }

  return (
    <div className="fixed bottom-16 sm:bottom-6 left-4 right-4 sm:left-auto sm:right-6 z-50 max-w-sm bg-white/95 backdrop-blur-xl border border-red-200/80 rounded-2xl p-4 shadow-2xl space-y-3 animate-bounce-subtle">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-pulse-gradient flex items-center justify-center shrink-0 shadow-md">
            <span className="material-symbols-outlined text-white text-xl">phone_iphone</span>
          </div>
          <div>
            <h4 className="font-extrabold text-sm text-gray-900 headline-font">Instalar o App BloodMatch</h4>
            <p className="text-xs text-gray-600">Acesso rápido e atalho direto na tela inicial do seu celular.</p>
          </div>
        </div>
        <button
          type="button"
          onClick={handleDismiss}
          className="text-gray-400 hover:text-gray-600 p-1 text-xs font-bold"
          title="Fechar"
        >
          ✕
        </button>
      </div>

      {showPrompt && (
        <div className="flex items-center gap-2 pt-1">
          <button
            type="button"
            onClick={handleInstallClick}
            className="flex-1 py-2 px-3 rounded-xl bg-primary hover:bg-red-700 text-white font-bold text-xs shadow-md transition-all flex items-center justify-center gap-1.5"
          >
            <span className="material-symbols-outlined text-base">download</span>
            <span>Instalar App</span>
          </button>
          <button
            type="button"
            onClick={handleDismiss}
            className="py-2 px-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs transition-colors"
          >
            Agora não
          </button>
        </div>
      )}

      {isIos && showIosTip && !showPrompt && (
        <div className="bg-red-50 p-2.5 rounded-xl border border-red-100 text-[11px] text-gray-700 leading-snug space-y-1">
          <p className="font-bold text-primary flex items-center gap-1">
            <span className="material-symbols-outlined text-sm">ios_share</span>
            Como instalar no iPhone:
          </p>
          <p>
            Toque no botão <span className="font-bold">Compartilhar</span> na barra do Safari e selecione <span className="font-bold">"Adicionar à Tela de Início"</span>.
          </p>
        </div>
      )}
    </div>
  );
}
