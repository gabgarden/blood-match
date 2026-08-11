import type { HTMLAttributes, ReactNode } from "react";

type AppCardProps = {
  children: ReactNode;
} & HTMLAttributes<HTMLElement>;

export function AppCard({ children, className = "", ...props }: AppCardProps) {
  return (
    <section
      {...props}
      className={`rounded-[2rem] border border-gray-200 bg-white shadow-sm ${className}`.trim()}
    >
      {children}
    </section>
  );
}
