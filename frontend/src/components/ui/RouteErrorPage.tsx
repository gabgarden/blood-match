import { isRouteErrorResponse, Link, useRouteError } from "react-router-dom";

export function RouteErrorPage() {
  const error = useRouteError();

  let title = "Algo deu errado";
  let description = "Não foi possível carregar esta página agora. Tente novamente em instantes.";

  if (isRouteErrorResponse(error)) {
    title = `${error.status} ${error.statusText}`;

    if (error.status === 404) {
      description = "A rota solicitada não foi encontrada.";
    }
  } else if (error instanceof Error && error.message.trim().length > 0) {
    description = error.message;
  }

  return (
    <main className="min-h-screen bg-surface px-6 py-10 grid place-items-center">
      <section className="w-full max-w-lg rounded-[2rem] border border-red-100 bg-white p-8 shadow-xl">
        <p className="text-xs font-bold uppercase tracking-[0.35em] text-[#ae131a]">Bloodmatch</p>
        <h1 className="mt-3 headline-font text-3xl font-extrabold tracking-tight text-[#1a1c1d]">
          {title}
        </h1>
        <p className="mt-3 text-sm leading-6 text-gray-600">{description}</p>

        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <Link
            to="/"
            className="inline-flex items-center justify-center rounded-xl bg-[#ae131a] px-5 py-3 text-sm font-bold text-white shadow-lg shadow-[#ae131a]/20 transition-transform hover:scale-[0.99]"
          >
            Ir para o início
          </Link>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="inline-flex items-center justify-center rounded-xl border border-gray-200 bg-white px-5 py-3 text-sm font-bold text-[#1a1c1d] transition-colors hover:bg-gray-50"
          >
            Recarregar página
          </button>
        </div>
      </section>
    </main>
  );
}