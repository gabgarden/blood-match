export function CommunityImpactSection() {
  const stats = [
    {
      label: "Bolsas de Sangue",
      value: "1.480+",
      sub: "Doadas através do sistema",
      icon: "bloodtype",
      color: "bg-red-50 text-primary border-red-100",
    },
    {
      label: "Vidas Salvas (Est.)",
      value: "5.920+",
      sub: "Pacientes beneficiados",
      icon: "favorite",
      color: "bg-rose-50 text-rose-600 border-rose-100",
    },
    {
      label: "Hemocentros Ativos",
      value: "12",
      sub: "Unidades conectadas na região",
      icon: "local_hospital",
      color: "bg-blue-50 text-blue-600 border-blue-100",
    },
    {
      label: "Doadores Engajados",
      value: "840+",
      sub: "Usuários cadastrados ativos",
      icon: "group",
      color: "bg-emerald-50 text-emerald-600 border-emerald-100",
    },
  ];

  return (
    <section className="col-span-12 rounded-[2rem] border border-surface-container-high bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-1 mb-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-2xl">insights</span>
            <h2 className="font-headline text-xl font-extrabold text-on-surface">
              Nosso Impacto em Números
            </h2>
          </div>
          <p className="mt-1 text-sm text-text-secondary">
            Transparência do movimento de doações na comunidade.
          </p>
        </div>

        <span className="rounded-full bg-[#fff2f0] px-3 py-1 text-xs font-bold text-primary border border-[#f5c6c2]">
          Atualizado hoje ★
        </span>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((item) => (
          <div
            key={item.label}
            className={`flex items-start gap-4 rounded-2xl border p-4 transition-all hover:shadow-md ${item.color}`}
          >
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white shadow-xs">
              <span className="material-symbols-outlined text-2xl">{item.icon}</span>
            </div>
            <div>
              <p className="font-headline text-2xl font-black text-on-surface leading-none">
                {item.value}
              </p>
              <p className="mt-1 text-xs font-bold text-on-surface">{item.label}</p>
              <p className="mt-0.5 text-[11px] text-text-secondary">{item.sub}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
