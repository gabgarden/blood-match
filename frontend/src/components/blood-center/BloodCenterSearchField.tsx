import { useEffect, useId, useRef, useState, type KeyboardEvent } from "react";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import {
  searchBloodCenters,
  type BloodCenterSearchResult,
} from "../../services/bloodCenterService";
import { extractApiErrorMessage } from "../../utils/apiError";

export type BloodCenterSelection = {
  organizationId: string;
  name: string;
  city: string | null;
  state: string | null;
};

type BloodCenterSearchFieldProps = {
  value: BloodCenterSelection | null;
  onChange: (selection: BloodCenterSelection | null) => void;
  label?: string;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  id?: string;
};

function formatLocation(item: Pick<BloodCenterSearchResult, "city" | "state">): string | null {
  const parts = [item.city, item.state].filter((part) => part && part.trim().length > 0);
  return parts.length > 0 ? parts.join(" · ") : null;
}

export function BloodCenterSearchField({
  value,
  onChange,
  label = "Hemocentro",
  placeholder = "Buscar pelo nome do hemocentro...",
  required = false,
  disabled = false,
  id,
}: BloodCenterSearchFieldProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const listboxId = `${inputId}-listbox`;

  const [query, setQuery] = useState("");
  const [results, setResults] = useState<BloodCenterSearchResult[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);

  const rootRef = useRef<HTMLDivElement>(null);
  const debouncedQuery = useDebouncedValue(query, 300);

  useEffect(() => {
    if (value) {
      setQuery(value.name);
    }
  }, [value?.organizationId, value?.name]);

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
        setHighlightedIndex(-1);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  useEffect(() => {
    const trimmed = debouncedQuery.trim();

    if (value && trimmed === value.name.trim()) {
      setResults([]);
      setIsLoading(false);
      setErrorMessage(null);
      return;
    }

    if (trimmed.length < 2) {
      setResults([]);
      setIsLoading(false);
      setErrorMessage(null);
      return;
    }

    let cancelled = false;

    async function runSearch() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const items = await searchBloodCenters(trimmed, 10);
        if (cancelled) {
          return;
        }
        setResults(items);
        setHighlightedIndex(items.length > 0 ? 0 : -1);
        setIsOpen(true);
      } catch (error) {
        if (cancelled) {
          return;
        }
        setResults([]);
        setErrorMessage(extractApiErrorMessage(error, "Não foi possível buscar hemocentros."));
        setIsOpen(true);
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    runSearch();

    return () => {
      cancelled = true;
    };
  }, [debouncedQuery, value]);

  function selectItem(item: BloodCenterSearchResult) {
    onChange({
      organizationId: item.organizationId,
      name: item.name,
      city: item.city,
      state: item.state,
    });
    setQuery(item.name);
    setResults([]);
    setIsOpen(false);
    setHighlightedIndex(-1);
    setErrorMessage(null);
  }

  function clearSelection() {
    onChange(null);
    setQuery("");
    setResults([]);
    setIsOpen(false);
    setHighlightedIndex(-1);
    setErrorMessage(null);
  }

  function handleInputChange(next: string) {
    setQuery(next);
    if (value) {
      onChange(null);
    }
    setIsOpen(true);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (!isOpen && (event.key === "ArrowDown" || event.key === "ArrowUp")) {
      setIsOpen(true);
      return;
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      if (results.length === 0) {
        return;
      }
      setHighlightedIndex((current) => (current + 1) % results.length);
      return;
    }

    if (event.key === "ArrowUp") {
      event.preventDefault();
      if (results.length === 0) {
        return;
      }
      setHighlightedIndex((current) => (current <= 0 ? results.length - 1 : current - 1));
      return;
    }

    if (event.key === "Enter") {
      if (isOpen && highlightedIndex >= 0 && results[highlightedIndex]) {
        event.preventDefault();
        selectItem(results[highlightedIndex]);
      }
      return;
    }

    if (event.key === "Escape") {
      setIsOpen(false);
      setHighlightedIndex(-1);
    }
  }

  const showDropdown =
    isOpen &&
    !value &&
    (isLoading || errorMessage !== null || debouncedQuery.trim().length >= 2);

  return (
    <div ref={rootRef} className="space-y-2">
      <label
        htmlFor={inputId}
        className="block font-label text-sm font-semibold text-secondary uppercase tracking-wider"
      >
        {label}
        {required ? <span className="ml-1 text-primary">*</span> : null}
      </label>

      {value ? (
        <div className="flex items-start gap-3 rounded-xl bg-surface-container-lowest border border-outline-variant/40 p-4">
          <span className="material-symbols-outlined text-primary mt-0.5">local_hospital</span>
          <div className="min-w-0 flex-1">
            <p className="font-headline text-base font-bold text-on-surface truncate">{value.name}</p>
            {formatLocation(value) && (
              <p className="mt-0.5 text-sm text-text-secondary">{formatLocation(value)}</p>
            )}
          </div>
          <button
            type="button"
            onClick={clearSelection}
            disabled={disabled}
            className="rounded-lg px-2 py-1 text-xs font-bold uppercase tracking-wide text-secondary hover:bg-surface-container-high transition-colors disabled:opacity-50"
            aria-label="Trocar hemocentro"
          >
            Trocar
          </button>
        </div>
      ) : (
        <div className="relative">
          <input
            id={inputId}
            type="text"
            role="combobox"
            aria-expanded={showDropdown}
            aria-controls={listboxId}
            aria-autocomplete="list"
            aria-activedescendant={
              highlightedIndex >= 0 ? `${listboxId}-option-${highlightedIndex}` : undefined
            }
            value={query}
            onChange={(event) => handleInputChange(event.target.value)}
            onFocus={() => setIsOpen(true)}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            disabled={disabled}
            autoComplete="off"
            className="w-full bg-surface-container-highest border-none rounded-xl p-4 pr-12 focus:ring-0 focus:bg-surface-container-lowest focus:border-l-4 focus:border-primary transition-all placeholder:text-gray-400 disabled:opacity-60"
          />
          <span className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 material-symbols-outlined text-sm pointer-events-none">
            {isLoading ? "progress_activity" : "search"}
          </span>

          {showDropdown && (
            <div
              id={listboxId}
              role="listbox"
              className="absolute z-30 mt-2 w-full overflow-hidden rounded-xl border border-surface-container-high bg-white shadow-lg"
            >
              {isLoading && (
                <p className="px-4 py-3 text-sm text-text-secondary">Buscando hemocentros...</p>
              )}

              {!isLoading && errorMessage && (
                <p className="px-4 py-3 text-sm text-primary">{errorMessage}</p>
              )}

              {!isLoading && !errorMessage && results.length === 0 && (
                <p className="px-4 py-3 text-sm text-text-secondary">
                  Nenhum hemocentro encontrado para “{debouncedQuery.trim()}”.
                </p>
              )}

              {!isLoading &&
                !errorMessage &&
                results.map((item, index) => {
                  const location = formatLocation(item);
                  const active = index === highlightedIndex;

                  return (
                    <button
                      key={item.organizationId}
                      id={`${listboxId}-option-${index}`}
                      type="button"
                      role="option"
                      aria-selected={active}
                      onMouseEnter={() => setHighlightedIndex(index)}
                      onClick={() => selectItem(item)}
                      className={`flex w-full flex-col items-start gap-0.5 px-4 py-3 text-left transition-colors ${
                        active ? "bg-[#fff2f0]" : "bg-white hover:bg-surface-container-low"
                      }`}
                    >
                      <span className="font-semibold text-on-surface">{item.name}</span>
                      {location && <span className="text-xs text-text-secondary">{location}</span>}
                    </button>
                  );
                })}
            </div>
          )}
        </div>
      )}

      {!value && query.trim().length > 0 && query.trim().length < 2 && (
        <p className="text-xs text-text-secondary">Digite ao menos 2 caracteres para buscar.</p>
      )}
    </div>
  );
}
