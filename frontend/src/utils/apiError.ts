import { isAxiosError } from "axios";

type ApiErrorBody = {
  error?: unknown;
  message?: unknown;
  errors?: unknown;
};

function readString(value: unknown): string | null {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

export function extractApiErrorMessage(error: unknown, fallback: string): string {
  if (!isAxiosError(error)) {
    return error instanceof Error && error.message.trim().length > 0 ? error.message : fallback;
  }

  if (!error.response) {
    return "Não foi possível conectar ao servidor. Confirme se a API está no ar e se este endereço tem permissão de acesso.";
  }

  const data = error.response.data as ApiErrorBody | string | undefined;

  if (typeof data === "string") {
    const message = readString(data);
    if (message) {
      return translateKnownApiMessage(message);
    }
  } else if (data && typeof data === "object") {
    const fromError = readString(data.error);
    if (fromError) {
      return translateKnownApiMessage(fromError);
    }

    const fromMessage = readString(data.message);
    if (fromMessage) {
      return translateKnownApiMessage(fromMessage);
    }

    if (Array.isArray(data.errors) && data.errors.length > 0) {
      const firstError = data.errors[0];
      if (typeof firstError === "string") {
        const message = readString(firstError);
        if (message) {
          return translateKnownApiMessage(message);
        }
      } else if (firstError && typeof firstError === "object") {
        const message = readString((firstError as { message?: unknown }).message);
        if (message) {
          return translateKnownApiMessage(message);
        }
      }
    }
  }

  switch (error.response.status) {
    case 400:
      return "Dados inválidos. Verifique os campos e tente novamente.";
    case 401:
      return "Não autorizado. Faça login novamente.";
    case 403:
      return "Sem permissão para esta ação. Verifique se seu perfil (role) está completo e faça login novamente.";
    case 404:
      return "Recurso não encontrado.";
    case 409:
      return "Conflito: este recurso já existe ou não pode ser criado novamente.";
    default:
      return fallback;
  }
}

export function translateKnownApiMessage(message: string): string {
  const normalized = message.toLowerCase();
  if (normalized.includes("email not confirmed")) {
    return "Confirme seu e-mail antes de entrar.";
  }
  if (normalized.includes("invalid confirmation token")) {
    return "Link de confirmação inválido.";
  }
  if (normalized.includes("confirmation token expired")) {
    return "Este link de confirmação expirou. Solicite um novo e-mail.";
  }
  if (normalized.includes("if the email is registered")) {
    return "Se este e-mail estiver cadastrado e pendente, enviaremos um novo link.";
  }
  if (normalized === "email confirmed") {
    return "E-mail confirmado com sucesso.";
  }
  if (normalized.includes("expectedtime is required")) {
    return "Este hemocentro exige um horário. Escolha um dos horários disponíveis.";
  }
  if (normalized.includes("time slot is fully booked")) {
    return "Este horário já está lotado. Escolha outro.";
  }
  if (normalized.includes("does not match a valid slot")) {
    return "O horário escolhido não está disponível neste hemocentro.";
  }
  return message;
}

export function isUnconfirmedAccountError(error: unknown): boolean {
  if (!isAxiosError(error) || error.response?.status !== 401) {
    return false;
  }

  const data = error.response.data as ApiErrorBody | string | undefined;
  const raw =
    (typeof data === "string" ? readString(data) : readString(data?.error) ?? readString(data?.message)) ?? "";
  const normalized = raw.toLowerCase();
  return normalized.includes("email not confirmed") || normalized.includes("user account is disabled");
}
