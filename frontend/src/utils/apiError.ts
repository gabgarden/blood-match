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
    return "Não foi possível conectar ao servidor. Verifique se a API permite acesso deste domínio (CORS).";
  }

  const data = error.response.data as ApiErrorBody | string | undefined;

  if (typeof data === "string") {
    const message = readString(data);
    if (message) {
      return message;
    }
  } else if (data && typeof data === "object") {
    const fromError = readString(data.error);
    if (fromError) {
      return fromError;
    }

    const fromMessage = readString(data.message);
    if (fromMessage) {
      return fromMessage;
    }

    if (Array.isArray(data.errors) && data.errors.length > 0) {
      const firstError = data.errors[0];
      if (typeof firstError === "string") {
        const message = readString(firstError);
        if (message) {
          return message;
        }
      } else if (firstError && typeof firstError === "object") {
        const message = readString((firstError as { message?: unknown }).message);
        if (message) {
          return message;
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
