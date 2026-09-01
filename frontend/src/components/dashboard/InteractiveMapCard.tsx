import { useEffect, useMemo, useRef, useState } from "react";
import L from "leaflet";
import type { Recommendation } from "../../hooks/useDonorDashboard";

type InteractiveMapCardProps = {
  recommendations: Recommendation[];
  onSchedule?: (requestId: string) => void;
  isEligibleToDonate?: boolean;
  daysRemaining?: number;
};

// Coordenadas base (Campos dos Goytacazes - RJ, centro dos hemocentros do sistema)
const BASE_LAT = -21.7545;
const BASE_LNG = -41.3244;

// Mapeamento de coordenadas geográficas reais de hospitais e hemocentros em Campos dos Goytacazes - RJ
const KNOWN_COORDINATES: Record<string, [number, number]> = {
  "Hospital Ferreira Machado": [-21.75122, -41.31905],
  "Hemocentro Regional de Campos": [-21.75250, -41.31930],
  "Hospital Geral Benê (Beneficência Portuguesa)": [-21.75698, -41.32623],
  "Núcleo Medicina Transfusional (Banco de Sangue)": [-21.75412, -41.32785],
  "Santa Casa de Misericórdia de Campos": [-21.76185, -41.32352],
  "Hospital Unimed Campos": [-21.76542, -41.33235],
  "Hospital dos Plantadores de Cana": [-21.75841, -41.32142],
  "Hospital Geral de Guarus": [-21.73885, -41.33925],
  "Hospital Geral Dr. Beda": [-21.75382, -41.32765],
  "Hospital Escola Álvaro Alvim": [-21.76425, -41.33512],
};

function getCoordinatesForItem(item: Recommendation, index: number): [number, number] {
  if (item.latitude != null && item.longitude != null) {
    return [item.latitude, item.longitude];
  }
  const known = KNOWN_COORDINATES[item.bloodCenterName.trim()];
  if (known) {
    return known;
  }
  // Offset leve se for um novo hemocentro sem coordenadas no banco
  const angle = (index * 2 * Math.PI) / 8;
  const radius = 0.015;
  return [BASE_LAT + Math.sin(angle) * radius, BASE_LNG + Math.cos(angle) * radius];
}

function getUrgencyConfig(urgency: "LOW" | "MEDIUM" | "CRITICAL") {
  switch (urgency) {
    case "CRITICAL":
      return {
        color: "#dc2626", // Vermelho
        label: "Crítica",
        badgeBg: "bg-red-100 text-red-700",
        pulse: true,
      };
    case "MEDIUM":
      return {
        color: "#d97706", // Amarelo/Laranja
        label: "Média",
        badgeBg: "bg-amber-100 text-amber-700",
        pulse: false,
      };
    default:
      return {
        color: "#16a34a", // Verde
        label: "Normal",
        badgeBg: "bg-emerald-100 text-emerald-700",
        pulse: false,
      };
  }
}

function createCustomIcon(color: string, pulse: boolean) {
  const pulseHtml = pulse
    ? `<span class="absolute -top-1 -right-1 h-3.5 w-3.5 animate-ping rounded-full bg-red-500 opacity-75"></span>`
    : "";

  const html = `
    <div class="relative flex items-center justify-center">
      ${pulseHtml}
      <div style="background-color: ${color};" class="flex h-9 w-9 items-center justify-center rounded-full text-white shadow-lg ring-2 ring-white">
        <span class="material-symbols-outlined text-xl">bloodtype</span>
      </div>
    </div>
  `;

  return L.divIcon({
    html,
    className: "custom-leaflet-marker",
    iconSize: [36, 36],
    iconAnchor: [18, 18],
    popupAnchor: [0, -20],
  });
}

export function InteractiveMapCard({
  recommendations,
  onSchedule,
  isEligibleToDonate = true,
  daysRemaining = 0,
}: InteractiveMapCardProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const [filterMode, setFilterMode] = useState<"ALL" | "CRITICAL" | "PENDING">("ALL");
  const [selectedCenter, setSelectedCenter] = useState<{
    requestId: string;
    name: string;
    bloodType: string;
    urgency: "LOW" | "MEDIUM" | "CRITICAL";
    lat: number;
    lng: number;
    fulfilled: number;
    goal: number;
  } | null>(null);

  const filteredRecommendations = useMemo(() => {
    if (filterMode === "CRITICAL") {
      return recommendations.filter((r) => r.urgency === "CRITICAL");
    }
    if (filterMode === "PENDING") {
      return recommendations.filter((r) => !r.goalReached);
    }
    return recommendations;
  }, [recommendations, filterMode]);

  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [BASE_LAT, BASE_LNG],
        zoom: 13,
        zoomControl: true,
      });

      L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 19,
      }).addTo(map);

      mapInstanceRef.current = map;
    }

    const map = mapInstanceRef.current;

    // Limpar marcadores anteriores
    map.eachLayer((layer) => {
      if (layer instanceof L.Marker) {
        map.removeLayer(layer);
      }
    });

    // Adicionar pinos para cada recomendação/hemocentro filtrado
    filteredRecommendations.forEach((item: Recommendation, idx: number) => {
      const [lat, lng] = getCoordinatesForItem(item, idx);
      const config = getUrgencyConfig(item.urgency);
      const icon = createCustomIcon(config.color, config.pulse);

      const fulfilled = item.fulfilledBloodBags ?? 0;
      const goal = item.goalBloodBags ?? 0;

      const marker = L.marker([lat, lng], { icon }).addTo(map);

      marker.on("click", () => {
        setSelectedCenter({
          requestId: item.id,
          name: item.bloodCenterName,
          bloodType: item.bloodTypeNeeded,
          urgency: item.urgency,
          lat,
          lng,
          fulfilled,
          goal,
        });
      });
    });

    // Ajustar zoom para conter todos os pinos se houver mais de 1
    if (filteredRecommendations.length > 0) {
      const bounds = filteredRecommendations.map((item: Recommendation, idx: number) => getCoordinatesForItem(item, idx));
      map.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 });
    }
  }, [filteredRecommendations]);

  const selectedConfig = selectedCenter ? getUrgencyConfig(selectedCenter.urgency) : null;
  const googleMapsUrl = selectedCenter
    ? `https://www.google.com/maps/dir/?api=1&destination=${selectedCenter.lat},${selectedCenter.lng}`
    : "#";
  const wazeUrl = selectedCenter
    ? `https://waze.com/ul?ll=${selectedCenter.lat},${selectedCenter.lng}&navigate=yes`
    : "#";

  return (
    <section className="col-span-12 overflow-hidden rounded-[2rem] border border-surface-container-high bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between mb-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-2xl">map</span>
            <h2 className="font-headline text-xl font-extrabold text-on-surface">
              Mapa de Hemocentros e Urgências
            </h2>
          </div>
          <p className="mt-1 text-sm text-text-secondary">
            Localize hemocentros próximos e veja onde sua doação é mais urgente.
          </p>
        </div>

        {/* Legenda dos Pinos */}
        <div className="flex items-center gap-3 text-xs font-bold">
          <span className="inline-flex items-center gap-1.5 rounded-lg bg-red-50 px-2.5 py-1 text-red-700">
            <span className="h-2 w-2 rounded-full bg-red-600 animate-pulse"></span>
            Crítica
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-lg bg-amber-50 px-2.5 py-1 text-amber-700">
            <span className="h-2 w-2 rounded-full bg-amber-500"></span>
            Média
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-50 px-2.5 py-1 text-emerald-700">
            <span className="h-2 w-2 rounded-full bg-emerald-500"></span>
            Normal
          </span>
        </div>
      </div>

      {!isEligibleToDonate && daysRemaining > 0 && (
        <div className="mb-4 flex items-center gap-3 rounded-2xl bg-amber-50/90 border border-amber-200/80 p-4 text-amber-900 shadow-xs">
          <span className="material-symbols-outlined text-amber-600 text-2xl shrink-0">hourglass_top</span>
          <div className="text-xs leading-relaxed">
            <p className="font-extrabold text-sm text-amber-950">Aviso de Elegibilidade (Período de Descanso)</p>
            <p className="text-amber-800 mt-0.5">
              Faltam <strong>{daysRemaining} {daysRemaining === 1 ? "dia" : "dias"}</strong> para você poder doar novamente. Todas as solicitações e hemocentros ativos continuam visíveis no mapa para consulta!
            </p>
          </div>
        </div>
      )}

      {/* Filtros Rápidos Inteligentes */}
      <div className="flex flex-wrap items-center gap-2 mb-3">
        <button
          type="button"
          onClick={() => setFilterMode("ALL")}
          className={`rounded-full px-3 py-1 text-xs font-bold transition-all ${
            filterMode === "ALL"
              ? "bg-primary text-white shadow-xs"
              : "bg-surface-container-low text-secondary hover:bg-surface-container-high"
          }`}
        >
          Todos os locais ({recommendations.length})
        </button>

        <button
          type="button"
          onClick={() => setFilterMode("CRITICAL")}
          className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold transition-all ${
            filterMode === "CRITICAL"
              ? "bg-red-600 text-white shadow-xs"
              : "bg-red-50 text-red-700 hover:bg-red-100"
          }`}
        >
          <span className="h-2 w-2 rounded-full bg-red-400 animate-pulse"></span>
          Apenas Urgência Crítica
        </button>

        <button
          type="button"
          onClick={() => setFilterMode("PENDING")}
          className={`rounded-full px-3 py-1 text-xs font-bold transition-all ${
            filterMode === "PENDING"
              ? "bg-emerald-600 text-white shadow-xs"
              : "bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
          }`}
        >
          Com meta pendente
        </button>
      </div>

      {/* Container do Mapa Leaflet */}
      <div className="relative">
        <div
          ref={mapContainerRef}
          className="h-80 w-full rounded-2xl border border-surface-container-high z-0"
        />

        {/* Card flutuante de detalhes ao clicar no pino */}
        {selectedCenter && selectedConfig && (
          <div className="absolute bottom-4 left-4 right-4 z-10 mx-auto max-w-md rounded-2xl bg-white p-4 shadow-xl border border-surface-container-high animate-in fade-in slide-in-from-bottom-2">
            <div className="flex items-start justify-between gap-3">
              <div>
                <span className={`inline-block rounded-md px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider ${selectedConfig.badgeBg}`}>
                  Urgência {selectedConfig.label}
                </span>
                <h3 className="mt-1 font-headline font-bold text-on-surface text-base">
                  {selectedCenter.name}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setSelectedCenter(null)}
                className="rounded-lg p-1 text-text-secondary hover:bg-surface-container-low"
              >
                <span className="material-symbols-outlined text-lg">close</span>
              </button>
            </div>

            <div className="mt-3 flex items-center justify-between text-xs text-text-secondary border-t border-surface-container-low pt-3">
              <div>
                <span>Tipo necessário: </span>
                <strong className="font-extrabold text-primary">{selectedCenter.bloodType}</strong>
              </div>
              {selectedCenter.goal > 0 && (
                <div>
                  <span>Bolsas: </span>
                  <strong className="font-bold text-on-surface">
                    {selectedCenter.fulfilled} / {selectedCenter.goal}
                  </strong>
                </div>
              )}
            </div>

            {/* Botões de Ação: Agendar Doação & Navegação */}
            <div className="mt-3 flex flex-col gap-2 sm:flex-row">
              {onSchedule && (
                <button
                  type="button"
                  onClick={() => onSchedule(selectedCenter.requestId)}
                  className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl bg-primary py-2 px-3 text-xs font-bold text-white shadow-sm hover:bg-[#920f16] transition-colors"
                >
                  <span className="material-symbols-outlined text-sm">event_available</span>
                  Agendar doação
                </button>
              )}
              <div className="flex flex-1 gap-2">
                <a
                  href={googleMapsUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex-1 inline-flex items-center justify-center gap-1 rounded-xl bg-surface-container-low border border-surface-container-high py-2 px-2 text-xs font-bold text-on-surface hover:bg-surface-container-high transition-colors text-center"
                >
                  <span className="material-symbols-outlined text-sm text-primary">navigation</span>
                  Google Maps
                </a>
                <a
                  href={wazeUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex-1 inline-flex items-center justify-center gap-1 rounded-xl bg-[#33ccff]/10 border border-[#33ccff]/30 py-2 px-2 text-xs font-bold text-slate-800 hover:bg-[#33ccff]/20 transition-colors text-center"
                >
                  <span className="material-symbols-outlined text-sm text-[#00a3cc]">near_me</span>
                  Waze
                </a>
              </div>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
