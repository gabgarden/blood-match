import { useEffect, useState } from "react";
import { isAxiosError } from "axios";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { authService } from "../services/authService";
import { AppButton, AppCard, BackButton, InlineAlert } from "../components/ui";
import { resolvePostLoginPath } from "../routes/roleRouting";
import { extractApiErrorMessage, isUnconfirmedAccountError } from "../utils/apiError";

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [form, setForm] = useState({ email: "", password: "" });
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [needsEmailConfirmation, setNeedsEmailConfirmation] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);

  useEffect(() => {
    const notice = authService.consumePostLoginNotice();
    if (notice) {
      if (notice.toLowerCase().includes("expirad") || notice.toLowerCase().includes("erro") || notice.toLowerCase().includes("inválid")) {
        setErrorMessage(notice);
      } else {
        setSuccessMessage(notice);
      }
    }
  }, []);

  function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleResendConfirmation() {
    if (!form.email.trim()) {
      setErrorMessage("Informe o e-mail para reenviar a confirmação.");
      return;
    }

    setIsResending(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const result = await authService.resendConfirmation(form.email.trim());
      setSuccessMessage(result.message);
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível reenviar o e-mail agora."));
    } finally {
      setIsResending(false);
    }
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);
    setNeedsEmailConfirmation(false);
    setIsSubmitting(true);

    try {
      const session = await login(form);
      const redirectPath =
        (location.state as { from?: string } | null)?.from ?? resolvePostLoginPath(session.roles);
      navigate(redirectPath, { replace: true });
    } catch (error) {
      if (isUnconfirmedAccountError(error)) {
        setNeedsEmailConfirmation(true);
        setErrorMessage("Confirme seu e-mail antes de entrar.");
      } else if (isAxiosError(error) && error.response?.status === 401) {
        setErrorMessage("E-mail ou senha inválidos.");
      } else {
        setErrorMessage(extractApiErrorMessage(error, "Não foi possível entrar. Tente novamente em instantes."));
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen bg-surface flex flex-col items-center justify-center px-4 py-8 relative">
      <div className="w-full max-w-md mb-4 flex items-center justify-start">
        <BackButton fallbackPath="/" label="Voltar para a página inicial" variant="outline" />
      </div>
      <AppCard className="w-full max-w-md p-8 shadow-xl">
        <header className="text-center mb-8">
          <h1 className="text-3xl font-extrabold headline-font text-primary">BloodMatch</h1>
          <p className="mt-2 text-sm text-gray-600">Faça login para ajudar a salvar vidas.</p>
        </header>

        {successMessage && <InlineAlert className="mb-4" tone="success" message={successMessage} />}
        {errorMessage && <InlineAlert className="mb-4" tone="error" message={errorMessage} />}

        {needsEmailConfirmation && (
          <AppButton
            type="button"
            variant="secondary"
            fullWidth
            className="mb-4"
            disabled={isResending}
            onClick={handleResendConfirmation}
          >
            {isResending ? "Reenviando..." : "Reenviar e-mail de confirmação"}
          </AppButton>
        )}

        <form className="space-y-4" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="email" className="block text-sm font-semibold text-gray-700 mb-1">
              E-mail
            </label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={handleChange}
              className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500"
              placeholder="voce@exemplo.com"
              required
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-semibold text-gray-700 mb-1">
              Senha
            </label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={handleChange}
              className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500"
              placeholder="Digite sua senha"
              required
            />
          </div>

          <AppButton type="submit" fullWidth disabled={isSubmitting}>
            {isSubmitting ? "Entrando..." : "Entrar"}
          </AppButton>
        </form>

        <footer className="mt-6 text-center text-sm text-gray-600">
          Não possui conta?{" "}
          <Link className="font-bold text-primary hover:underline" to="/register">
            Cadastre-se
          </Link>
        </footer>
      </AppCard>
    </main>
  );
}
