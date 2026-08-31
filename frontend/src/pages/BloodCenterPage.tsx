import { useState } from "react";
import { AccessDenied } from "../components/AccessDenied";
import { DonorDashboardSidebar } from "../components/dashboard/DashboardSidebar";
import { DonorDashboardTopbar } from "../components/dashboard/DashboardTopbar";
import { MobileBottomNav } from "../components/dashboard/MobileBottomNav";
import { AppButton, AppCard, FullPageLoading, InlineAlert } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import {
  DEFAULT_DRAFT_WINDOW,
  useBloodCenterWorkspace,
  type DraftWindow,
} from "../hooks/useBloodCenterWorkspace";
import { useRoleResolution } from "../hooks/useRoleResolution";
import { hasAdminRole, hasBloodCenterRole } from "../routes/roleRouting";
import {
  WEEKDAY_LABELS,
  WEEKDAYS,
  type Weekday,
} from "../services/bloodCenterService";

function getStatusStyle(percentage: number) {
  if (percentage < 30) {
    return { text: "Crítico", className: "bg-red-100 text-red-700" };
  }
  if (percentage <= 70) {
    return { text: "Alerta", className: "bg-amber-100 text-amber-700" };
  }
  return { text: "Adequado", className: "bg-emerald-100 text-emerald-700" };
}

function formatIsoDate(value: string | null): string {
  if (!value) {
    return "—";
  }
  const [year, month, day] = value.slice(0, 10).split("-");
  if (!year || !month || !day) {
    return value;
  }
  return `${day}/${month}/${year}`;
}

export default function BloodCenterPage() {
  const { roles, partyId, logout } = useAuth();
  const isResolvingRoles = useRoleResolution(roles);
  const canAccess = hasBloodCenterRole(roles) || hasAdminRole(roles);
  const workspace = useBloodCenterWorkspace(partyId, canAccess);
  const [draft, setDraft] = useState<DraftWindow>(DEFAULT_DRAFT_WINDOW);
  const [blockedDraft, setBlockedDraft] = useState("");

  if (isResolvingRoles) {
    return <FullPageLoading message="Carregando permissões..." />;
  }

  if (!canAccess) {
    return (
      <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
        <DonorDashboardSidebar onLogout={logout} activeItem="blood-center" />
        <DonorDashboardTopbar title="Hemocentro" onLogout={logout} />
        <main className="pt-20 px-4 pb-24 lg:ml-64 lg:px-8 lg:pb-10">
          <div className="mx-auto max-w-3xl">
            <AccessDenied title="Área de hemocentro indisponível" />
          </div>
        </main>
        <MobileBottomNav activeItem="blood-center" />
      </div>
    );
  }

  function handleAddWindow() {
    if (draft.startTime >= draft.endTime) {
      return;
    }
    workspace.addWindow({ ...draft });
    setDraft(DEFAULT_DRAFT_WINDOW);
  }

  return (
    <div className="min-h-screen bg-[#f9f9fb] text-[#1a1c1d]">
      <DonorDashboardSidebar onLogout={logout} activeItem="blood-center" />
      <DonorDashboardTopbar title="Hemocentro" onLogout={logout} />

      <main className="pt-20 px-4 pb-24 lg:ml-64 lg:px-8 lg:pb-10">
        <div className="mx-auto max-w-5xl space-y-6">
          <header className="rounded-[2rem] border border-surface-container-high bg-white p-6 sm:p-8">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-red-50 text-primary">
                <span className="material-symbols-outlined text-2xl">local_hospital</span>
              </div>
              <div>
                <h1 className="font-headline text-2xl font-extrabold text-on-surface">Painel do hemocentro</h1>
                <p className="text-sm text-text-secondary">
                  Publique estoque, horários de doação e acompanhe as marcações.
                </p>
              </div>
            </div>
          </header>

          {workspace.feedback && <InlineAlert tone="success" message={workspace.feedback} />}
          {workspace.errorMessage && <InlineAlert tone="error" message={workspace.errorMessage} />}

          <AppCard className="p-6 sm:p-8 space-y-5">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary">water_drop</span>
                  <h2 className="font-headline text-xl font-extrabold">Estoque</h2>
                </div>
                <p className="mt-1 text-sm text-text-secondary">Nível estimado por tipo sanguíneo (0 a 100%).</p>
              </div>
              <AppButton
                type="button"
                variant="danger"
                disabled={workspace.isSavingInventory}
                onClick={() => void workspace.handleSaveInventory()}
                className="px-5 py-2.5 text-sm"
              >
                {workspace.isSavingInventory ? "Salvando..." : "Salvar estoque"}
              </AppButton>
            </div>

            {workspace.isLoadingInventory ? (
              <p className="text-sm text-text-secondary inline-flex items-center gap-2">
                <span className="material-symbols-outlined animate-spin text-base">progress_activity</span>
                Carregando estoque...
              </p>
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {workspace.items.map((item) => {
                  const status = getStatusStyle(item.percentage);
                  return (
                    <div
                      key={item.bloodType}
                      className="rounded-2xl border border-surface-container-high bg-surface-container-low p-4 space-y-3"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-headline text-xl font-black">{item.bloodType}</span>
                        <span className={`rounded-md px-2 py-0.5 text-[10px] font-extrabold uppercase ${status.className}`}>
                          {status.text}
                        </span>
                      </div>
                      <div className="flex items-center gap-3">
                        <input
                          type="range"
                          min={0}
                          max={100}
                          step={1}
                          value={item.percentage}
                          onChange={(event) =>
                            workspace.updateItemPercentage(item.bloodType, Number(event.target.value))
                          }
                          className="w-full accent-[#ae131a]"
                        />
                        <input
                          type="number"
                          min={0}
                          max={100}
                          value={item.percentage}
                          onChange={(event) =>
                            workspace.updateItemPercentage(
                              item.bloodType,
                              Math.min(100, Math.max(0, Number(event.target.value) || 0)),
                            )
                          }
                          className="w-16 rounded-xl border border-surface-container-high bg-white px-2 py-1.5 text-sm font-bold text-center"
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </AppCard>

          <AppCard className="p-6 sm:p-8 space-y-5">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-primary">schedule</span>
                  <h2 className="font-headline text-xl font-extrabold">Horários</h2>
                </div>
                <p className="mt-1 text-sm text-text-secondary">
                  Não há consulta da agenda salva. Defina as janelas e publique. A prévia usa os slots do dia.
                </p>
              </div>
              <AppButton
                type="button"
                variant="danger"
                disabled={workspace.isSavingSchedule}
                onClick={() => void workspace.handleSaveSchedule()}
                className="px-5 py-2.5 text-sm"
              >
                {workspace.isSavingSchedule ? "Salvando..." : "Salvar horários"}
              </AppButton>
            </div>

            <div className="rounded-2xl border border-surface-container-high bg-surface-container-low p-4 space-y-3">
              <p className="text-xs font-bold uppercase tracking-wider text-secondary">Nova janela</p>
              <div className="flex flex-wrap gap-2">
                {WEEKDAYS.map((day) => (
                  <button
                    key={day}
                    type="button"
                    onClick={() => setDraft((current) => ({ ...current, dayOfWeek: day }))}
                    className={`rounded-xl px-3 py-1.5 text-xs font-bold ${
                      draft.dayOfWeek === day
                        ? "bg-primary text-white"
                        : "bg-white border border-surface-container-high text-secondary"
                    }`}
                  >
                    {WEEKDAY_LABELS[day]}
                  </button>
                ))}
              </div>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <label className="text-xs font-bold text-secondary">
                  Início
                  <input
                    type="time"
                    value={draft.startTime}
                    onChange={(event) => setDraft((current) => ({ ...current, startTime: event.target.value }))}
                    className="mt-1 w-full rounded-xl border border-surface-container-high bg-white px-3 py-2 text-sm"
                  />
                </label>
                <label className="text-xs font-bold text-secondary">
                  Fim
                  <input
                    type="time"
                    value={draft.endTime}
                    onChange={(event) => setDraft((current) => ({ ...current, endTime: event.target.value }))}
                    className="mt-1 w-full rounded-xl border border-surface-container-high bg-white px-3 py-2 text-sm"
                  />
                </label>
                <label className="text-xs font-bold text-secondary">
                  Duração
                  <select
                    value={draft.slotDurationMinutes}
                    onChange={(event) =>
                      setDraft((current) => ({ ...current, slotDurationMinutes: Number(event.target.value) }))
                    }
                    className="mt-1 w-full rounded-xl border border-surface-container-high bg-white px-3 py-2 text-sm"
                  >
                    {[15, 30, 45, 60].map((minutes) => (
                      <option key={minutes} value={minutes}>
                        {minutes} min
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-xs font-bold text-secondary">
                  Capacidade
                  <input
                    type="number"
                    min={1}
                    value={draft.capacity}
                    onChange={(event) =>
                      setDraft((current) => ({ ...current, capacity: Math.max(1, Number(event.target.value) || 1) }))
                    }
                    className="mt-1 w-full rounded-xl border border-surface-container-high bg-white px-3 py-2 text-sm"
                  />
                </label>
              </div>
              <AppButton type="button" variant="secondary" className="px-4 py-2 text-sm" onClick={handleAddWindow}>
                Adicionar janela
              </AppButton>
            </div>

            {workspace.windows.length === 0 ? (
              <p className="text-sm text-text-secondary">Nenhuma janela cadastrada nesta sessão.</p>
            ) : (
              <ul className="space-y-2">
                {workspace.windows.map((window, index) => (
                  <li
                    key={`${window.dayOfWeek}-${window.startTime}-${index}`}
                    className="flex items-center justify-between rounded-2xl border border-surface-container-high bg-white px-4 py-3"
                  >
                    <p className="text-sm font-semibold">
                      {WEEKDAY_LABELS[window.dayOfWeek as Weekday]} · {window.startTime}–{window.endTime} ·{" "}
                      {window.slotDurationMinutes} min · {window.capacity} vagas
                    </p>
                    <button
                      type="button"
                      onClick={() => workspace.removeWindow(index)}
                      className="text-primary text-sm font-bold hover:underline"
                    >
                      Remover
                    </button>
                  </li>
                ))}
              </ul>
            )}

            <div className="space-y-2">
              <p className="text-xs font-bold uppercase tracking-wider text-secondary">Datas bloqueadas</p>
              <div className="flex flex-wrap items-center gap-2">
                <input
                  type="date"
                  value={blockedDraft}
                  onChange={(event) => setBlockedDraft(event.target.value)}
                  className="rounded-xl border border-surface-container-high bg-white px-3 py-2 text-sm"
                />
                <AppButton
                  type="button"
                  variant="secondary"
                  className="px-4 py-2 text-sm"
                  onClick={() => {
                    workspace.addBlockedDate(blockedDraft);
                    setBlockedDraft("");
                  }}
                >
                  Bloquear data
                </AppButton>
              </div>
              <div className="flex flex-wrap gap-2">
                {workspace.blockedDates.map((date) => (
                  <button
                    key={date}
                    type="button"
                    onClick={() => workspace.removeBlockedDate(date)}
                    className="rounded-full bg-red-50 px-3 py-1 text-xs font-bold text-primary"
                  >
                    {formatIsoDate(date)} ×
                  </button>
                ))}
              </div>
            </div>

            <div className="rounded-2xl border border-dashed border-surface-container-highest p-4 space-y-3">
              <p className="text-sm font-bold">Prévia de slots</p>
              <div className="flex flex-wrap items-center gap-2">
                <input
                  type="date"
                  value={workspace.previewDate}
                  onChange={(event) => void workspace.loadSlotPreview(event.target.value)}
                  className="rounded-xl border border-surface-container-high bg-white px-3 py-2 text-sm"
                />
                <AppButton
                  type="button"
                  variant="secondary"
                  className="px-4 py-2 text-sm"
                  onClick={() => void workspace.loadSlotPreview(workspace.previewDate)}
                >
                  Consultar
                </AppButton>
              </div>
              {workspace.isLoadingPreview && <p className="text-xs text-text-secondary">Carregando slots...</p>}
              {workspace.previewSlots && (
                <p className="text-xs text-text-secondary">
                  {workspace.previewSlots.hasSchedule
                    ? `${workspace.previewSlots.slots.length} horário(s) neste dia.`
                    : "Sem agenda publicada para esta data."}
                </p>
              )}
              {workspace.previewSlots?.slots.length ? (
                <div className="flex flex-wrap gap-2">
                  {workspace.previewSlots.slots.map((slot) => (
                    <span
                      key={`${slot.startTime}-${slot.endTime}`}
                      className="rounded-xl bg-surface-container-low px-3 py-1.5 text-xs font-bold"
                    >
                      {slot.startTime} · {slot.available} livres
                    </span>
                  ))}
                </div>
              ) : null}
            </div>
          </AppCard>

          <AppCard className="p-6 sm:p-8 space-y-5">
            <div>
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">event_note</span>
                <h2 className="font-headline text-xl font-extrabold">Marcações</h2>
              </div>
              <p className="mt-1 text-sm text-text-secondary">
                Doações previstas de {formatIsoDate(workspace.rangeFrom)} a {formatIsoDate(workspace.rangeTo)}.
              </p>
            </div>

            {workspace.isLoadingAppointments ? (
              <p className="text-sm text-text-secondary">Carregando marcações...</p>
            ) : workspace.appointments.length === 0 ? (
              <p className="text-sm text-text-secondary">Nenhuma marcação neste período.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[640px] text-left text-sm">
                  <thead>
                    <tr className="text-[11px] uppercase tracking-wider text-secondary">
                      <th className="pb-2 font-bold">Doador</th>
                      <th className="pb-2 font-bold">Tipo</th>
                      <th className="pb-2 font-bold">Data</th>
                      <th className="pb-2 font-bold">Horário</th>
                      <th className="pb-2 font-bold">Telefone</th>
                      <th className="pb-2 font-bold">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-surface-container-high">
                    {workspace.appointments.map((appointment) => (
                      <tr key={appointment.donationId}>
                        <td className="py-3 font-semibold">{appointment.donorName}</td>
                        <td className="py-3">{appointment.donorBloodType || "—"}</td>
                        <td className="py-3">{formatIsoDate(appointment.expectedDate)}</td>
                        <td className="py-3">{appointment.expectedTime ?? "—"}</td>
                        <td className="py-3">{appointment.donorPhone || "—"}</td>
                        <td className="py-3">{appointment.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </AppCard>
        </div>
      </main>

      <MobileBottomNav activeItem="blood-center" />
    </div>
  );
}
