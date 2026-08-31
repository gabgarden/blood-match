import type { ReactNode } from "react";

type ProfileSectionProps = {
  icon: string;
  title: string;
  description: string;
  children: ReactNode;
};

export function ProfileSection({ icon, title, description, children }: ProfileSectionProps) {
  return (
    <section className="rounded-[2rem] border border-surface-container-high bg-white p-6 sm:p-8">
      <div className="flex items-start gap-3 mb-6">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#fff2f0] text-primary">
          <span className="material-symbols-outlined text-xl">{icon}</span>
        </div>
        <div>
          <h2 className="font-headline text-xl font-extrabold text-on-surface">{title}</h2>
          <p className="mt-1 text-sm text-text-secondary">{description}</p>
        </div>
      </div>
      {children}
    </section>
  );
}
