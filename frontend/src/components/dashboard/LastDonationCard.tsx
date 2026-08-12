import { AppButton } from "../ui";

type LastDonationCardProps = {
  lastDonationDate: string | null;
  lastDonationHospitalName: string | null;
  hasDonation: boolean;
  onCreateExternalDonation: () => void;
};

function formatDate(input: string | null): string {
  if (!input || input === "null") {
    return "Sem registro";
  }

  const parsed = new Date(input);
  if (Number.isNaN(parsed.getTime())) {
    return input;
  }

  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "long",
    year: "numeric",
  }).format(parsed);
}

export function LastDonationCard({
  lastDonationDate,
  lastDonationHospitalName,
  hasDonation,
  onCreateExternalDonation,
}: LastDonationCardProps) {
  const formattedDate = formatDate(lastDonationDate);
  const locationText = lastDonationHospitalName || "Local não informado";

  return (
    <section className="relative col-span-12 lg:col-span-4 overflow-hidden rounded-[2rem] bg-pulse-gradient p-6 lg:p-8 text-white shadow-lg shadow-primary/15">
      <div className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-white/10" />
      <div className="absolute -bottom-10 -left-6 h-28 w-28 rounded-full bg-white/5" />

      <div className="relative z-10 flex h-full flex-col">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/15">
          <span className="material-symbols-outlined">history</span>
        </div>

        <h2 className="mt-5 font-headline text-xl font-extrabold">Última doação</h2>

        {hasDonation ? (
          <div className="mt-3 space-y-3 text-sm text-white/90">
            <p className="flex items-start gap-2">
              <span className="material-symbols-outlined mt-0.5 text-base text-white/70">event</span>
              <span>{formattedDate}</span>
            </p>
            <p className="flex items-start gap-2">
              <span className="material-symbols-outlined mt-0.5 text-base text-white/70">local_hospital</span>
              <span>{locationText}</span>
            </p>
          </div>
        ) : (
          <p className="mt-3 text-sm text-white/85">
            Ainda não há doações registradas nesta conta.
          </p>
        )}

        <AppButton
          variant="light"
          fullWidth
          className="mt-8"
          onClick={onCreateExternalDonation}
        >
          {hasDonation ? "Registrar outra doação" : "Registrar doação externa"}
        </AppButton>
      </div>
    </section>
  );
}
