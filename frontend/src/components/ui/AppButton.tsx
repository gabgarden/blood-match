import type { ButtonHTMLAttributes, ReactNode } from "react";

type AppButtonVariant = "primary" | "secondary" | "danger" | "light";

type AppButtonProps = {
  children: ReactNode;
  variant?: AppButtonVariant;
  fullWidth?: boolean;
} & ButtonHTMLAttributes<HTMLButtonElement>;

const variantClasses: Record<AppButtonVariant, string> = {
  primary: "bg-red-600 text-white hover:bg-red-700 disabled:opacity-60",
  secondary: "bg-white text-gray-800 border border-gray-300 hover:bg-gray-50 disabled:opacity-60",
  danger: "bg-[#ae131a] text-white hover:bg-[#920f16] disabled:opacity-60",
  light: "bg-white text-[#ae131a] hover:bg-red-50 disabled:opacity-60",
};

export function AppButton({
  children,
  variant = "primary",
  fullWidth = false,
  className = "",
  ...props
}: AppButtonProps) {
  return (
    <button
      {...props}
      className={`${fullWidth ? "w-full" : ""} rounded-xl py-3 font-bold transition-colors ${variantClasses[variant]} ${className}`.trim()}
    >
      {children}
    </button>
  );
}
