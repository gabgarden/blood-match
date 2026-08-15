import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { AppButton, AppCard, BackButton, InlineAlert } from "../components/ui";
import { authService } from "../services/authService";
import { getPendingRegisterEmail } from "../services/pendingProfiles";
import { extractApiErrorMessage } from "../utils/apiError";

type ConfirmStatus = "loading" | "success" | "error";

export default function ConfirmEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token")?.trim() ?? "";

  const [status, setStatus] = useState<ConfirmStatus>(token ? "loading" : "error");
  const [errorMessage, setErrorMessage] = useState<string | null>(
    token ? null : "Link inválido. O token de confirmação não foi informado.",
  );
  const [email, setEmail] = useState(getPendingRegisterEmail() ?? "");
  const [isResending, setIsResending] = useState(false);
  const [resendMessage, setResendMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }

    let cancelled = false;

    async function confirm() {
      try {
        const result = await authService.confirmEmail(token);
        if (cancelled) {
          return;
        }
        if (result.email) {
          setEmail(result.email);
        }
        authService.setPostLoginNotice("E-mail confirmado. Entre para continuar.");
        setStatus("success");
      } catch (error) {
        if (cancelled) {
          return;
        }
        setStatus("error");
        setErrorMessage(extractApiErrorMessage(error, "Não foi possível confirmar o e-mail. O link pode ter expirado."));
      }
    }

    confirm();

    return () => {
      cancelled = true;
    };
  }, [token]);

  async function handleResend() {
    if (!email) {
      setErrorMessage("Informe o e-mail para reenviar a confirmação.");
      return;
    }

    setIsResending(true);
    setResendMessage(null);

    try {
      const result = await authService.resendConfirmation(email);
      setResendMessage(result.message);
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível reenviar o e-mail agora."));
    } finally {
      setIsResending(false);
    }
  }

  return (
    <main className="min-h-screen bg-surface flex flex-col items-center justify-center px-4 py-8 relative">
      <div className="w-full max-w-md mb-4 flex items-center justify-start">
        <BackButton fallbackPath="/login" label="Ir para o login" variant="outline" />
      </div>

      <AppCard className="w-full max-w-md p-8 shadow-xl text-center">
        {status === "loading" && (
          <>
            <span className="material-symbols-outlined animate-spin text-4xl text-primary">progress_activity</span>
            <h1 className="mt-4 text-2xl font-extrabold headline-font text-on-surface">Confirmando e-mail...</h1>
            <p className="mt-2 text-sm text-gray-600">Aguarde um instante.</p>
          </>
        )}

        {status === "success" && (
          <>
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
              <span className="material-symbols-outlined text-3xl">mark_email_read</span>
            </div>
            <h1 className="text-2xl font-extrabold headline-font text-on-surface">E-mail confirmado</h1>
            <p className="mt-2 text-sm text-gray-600">Sua conta está ativa. Entre para continuar.</p>
            <Link
              to="/login"
              className="mt-6 inline-flex w-full items-center justify-center rounded-xl bg-red-600 py-3 font-bold text-white hover:bg-red-700"
            >
              Ir para o login
            </Link>
          </>
        )}

        {status === "error" && (
          <>
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-primary">
              <span className="material-symbols-outlined text-3xl">error</span>
            </div>
            <h1 className="text-2xl font-extrabold headline-font text-on-surface">Não foi possível confirmar</h1>
            {errorMessage && <InlineAlert className="mt-4 text-left" tone="error" message={errorMessage} />}
            {resendMessage && <InlineAlert className="mt-3 text-left" tone="success" message={resendMessage} />}
            {email ? (
              <AppButton
                type="button"
                variant="secondary"
                fullWidth
                className="mt-4"
                disabled={isResending}
                onClick={() => void handleResend()}
              >
                {isResending ? "Reenviando..." : "Reenviar e-mail de confirmação"}
              </AppButton>
            ) : null}
            <Link to="/login" className="mt-6 inline-block text-sm font-bold text-primary hover:underline">
              Voltar para o login
            </Link>
          </>
        )}
      </AppCard>
    </main>
  );
}
