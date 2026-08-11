type FullPageLoadingProps = {
  message?: string;
};

export function FullPageLoading({ message = "Carregando..." }: FullPageLoadingProps) {
  return (
    <main className="min-h-screen grid place-items-center bg-surface px-6">
      <div className="text-center">
        <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-primary/20 border-t-primary" />
        <p className="mt-3 text-sm text-gray-600">{message}</p>
      </div>
    </main>
  );
}