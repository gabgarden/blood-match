export function AccessDenied({ title = "Sem permissão" }: { title?: string }) {
  return (
    <div className="rounded-3xl border border-red-200 bg-red-50 p-6 text-red-900">
      <h3 className="text-xl font-extrabold headline-font">{title}</h3>
      <p className="mt-2 text-sm text-red-800">
        Você não possui as permissões necessárias para acessar esta área.
      </p>
    </div>
  );
}
