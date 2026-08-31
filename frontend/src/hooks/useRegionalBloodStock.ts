import { useEffect, useState } from "react";
import { fetchRegionalInventory, type InventoryLevel } from "../services/bloodCenterService";
import { extractApiErrorMessage } from "../utils/apiError";

export function useRegionalBloodStock(enabled: boolean) {
  const [levels, setLevels] = useState<InventoryLevel[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!enabled) {
      return;
    }

    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const nextLevels = await fetchRegionalInventory();
        if (!cancelled) {
          setLevels(nextLevels);
        }
      } catch (error) {
        if (!cancelled) {
          setLevels([]);
          setErrorMessage(
            extractApiErrorMessage(error, "Não foi possível carregar o estoque dos hemocentros agora."),
          );
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, [enabled]);

  return { levels, isLoading, errorMessage };
}
