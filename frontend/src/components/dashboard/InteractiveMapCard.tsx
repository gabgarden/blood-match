import { useEffect, useRef, useState } from "react";
import L from "leaflet";
import type { Recommendation } from "../../hooks/useDonorDashboard";

type InteractiveMapCardProps = {
  recommendations: Recommendation[];
};

// Coordenadas base (Campos dos Goytacazes - RJ, centro dos hemocentros do sistema)
const BASE_LAT = -21.7545;
const BASE_LNG = -41.3244;

// Mapeamento determinístico de posições para hemocentros conhecidos
const KNOWN_COORDINATES: Record<string, [number, number]> = {
  "Hospital Ferreira Machado": [-21.7512, -41.3190],
  "Hemocentro Regional de Campos": [-21.7525, -41.3195],
  "Hospital Geral Benê (Beneficência Portuguesa)": [-21.7570, -41.3260],
  "Núcleo Medicina Transfusional (Banco de Sangue)": [-21.7620, -41.3230],
  "Santa Casa de Misericórdia de Campos": [-21.7650, -41.3320],
  "Hospital Unimed Campos": [-21.7610, -41.3290],
  "Hospital dos Plantadores de Cana": [-21.7580, -41.3210],
  "Hospital Geral de Guarus": [-21.7390, -41.3390],
  "Hospital Geral Dr. Beda": [-21.7530, -41.3270],
  "Hospital Escola Álvaro Alvim": [-21.7640, -41.3350],
};

function getCoordinates(name: string, index: number): [number, number] {
  const known = KNOWN_COORDINATES[name.trim()];
  if (known) {
    return known;
  }
  // Offset leve se for um novo hemocentro
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

export function InteractiveMapCard({ recommendations }: InteractiveMapCardProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const [selectedCenter, setSelectedCenter] = useState<{
    name: string;
    bloodType: string;
    urgency: "LOW" | "MEDIUM" | "CRITICAL";
    lat: number;
    lng: number;
    fulfilled: number;
    goal: number;
  } | null>(null);

  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: [BASE_LAT, BASE_LNG],
        zoom: 13,
        zoomControl: true,
      });

      L.tileLayer("https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png", {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
        subdomains: "abcd",
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

    // Adicionar pinos para cada recomendação/hemocentro
    recommendations.forEach((item, idx) => {
      const [lat, lng] = getCoordinates(item.bloodCenterName, idx);
      const config = getUrgencyConfig(item.urgency);
      const icon = createCustomIcon(config.color, config.pulse);

      const fulfilled = item.fulfilledBloodBags ?? 0;
      const goal = item.goalBloodBags ?? 0;

      const marker = L.marker([lat, lng], { icon }).addTo(map);

      marker.on("click", () => {
        setSelectedCenter({
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
    if (recommendations.length > 0) {
      const bounds = recommendations.map((item, idx) => getCoordinates(item.bloodCenterName, idx));
      map.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 });
    }
  }, [recommendations]);

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

            {/* Botões de Navegação "Como Chegar" */}
            <div className="mt-3 flex gap-2">
              <a
                href={googleMapsUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl bg-primary py-2 px-3 text-xs font-bold text-white shadow-sm hover:bg-[#920f16] transition-colors"
              >
                <span className="material-symbols-outlined text-sm">navigation</span>
                Google Maps
              </a>
              <a
                href={wazeUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl bg-[#33ccff] py-2 px-3 text-xs font-bold text-slate-900 shadow-sm hover:bg-[#28b8e6] transition-colors"
              >
                <span className="material-symbols-outlined text-sm">near_me</span>
                Waze
              </a>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
