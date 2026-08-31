import { useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { AppButton, AppCard, BackButton, InlineAlert } from "../components/ui";
import { authService } from "../services/authService";
import { getPendingRegisterEmail } from "../services/pendingProfiles";
import { extractApiErrorMessage } from "../utils/apiError";

type CheckEmailLocationState = {
  email?: string;
};

export default function CheckEmailPage() {
  const location = useLocation();
  const email = useMemo(() => {
    const fromState = (location.state as CheckEmailLocationState | null)?.email?.trim();
    return fromState || getPendingRegisterEmail() || "";
  }, [location.state]);

  const [isResending, setIsResending] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleResend() {
    if (!email) {
      setErrorMessage("Não encontramos o e-mail do cadastro. Volte e tente novamente.");
      return;
    }

    setIsResending(true);
    setFeedback(null);
    setErrorMessage(null);

    try {
      const result = await authService.resendConfirmation(email);
      setFeedback(result.message);
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível reenviar o e-mail agora."));
    } finally {
      setIsResending(false);
    }
  }

  return (
    <main className="min-h-screen bg-surface flex flex-col items-center justify-center px-4 py-8 relative">
      <div className="w-full max-w-md mb-4 flex items-center justify-start">
        <BackButton fallbackPath="/register" label="Voltar ao cadastro" variant="outline" />
      </div>

      <AppCard className="w-full max-w-md p-8 shadow-xl">
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-primary">
          <span className="material-symbols-outlined text-3xl">mark_email_unread</span>
        </div>

        <header className="text-center mb-6">
          <h1 className="text-2xl font-extrabold headline-font text-primary">Confirme seu e-mail</h1>
          <p className="mt-3 text-sm text-gray-600 leading-relaxed">
            {email
              ? `Enviamos um e-mail para ${email}. Confirme a conta para ativá-la.`
              : "Enviamos um e-mail. Confirme a conta para ativá-la."}
          </p>
        </header>

        {feedback && <InlineAlert className="mb-4" tone="success" message={feedback} />}
        {errorMessage && <InlineAlert className="mb-4" tone="error" message={errorMessage} />}

        <AppButton type="button" variant="secondary" fullWidth disabled={isResending} onClick={handleResend}>
          {isResending ? "Reenviando..." : "Reenviar e-mail de confirmação"}
        </AppButton>

        <footer className="mt-6 text-center text-sm text-gray-600">
          Já confirmou?{" "}
          <Link className="font-bold text-primary hover:underline" to="/login">
            Entrar
          </Link>
        </footer>
      </AppCard>
    </main>
  );
}
