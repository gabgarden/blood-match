type InlineAlertProps = {
  tone: "success" | "error";
  message: string;
  className?: string;
};

export function InlineAlert({ tone, message, className = "" }: InlineAlertProps) {
  const toneClasses =
    tone === "success"
      ? "border-green-300 bg-green-50 text-green-800"
      : "border-red-300 bg-red-50 text-red-800";

  return (
    <div className={`rounded-xl border px-4 py-3 text-sm ${toneClasses} ${className}`.trim()}>{message}</div>
  );
}
