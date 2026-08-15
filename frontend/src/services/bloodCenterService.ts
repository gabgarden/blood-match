import { api } from "../api/client";

export const BLOOD_TYPES = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"] as const;
export type BloodTypeCode = (typeof BLOOD_TYPES)[number];

export type BloodCenterSearchResult = {
  organizationId: string;
  name: string;
  city: string | null;
  state: string | null;
};

export type InventoryLevel = {
  bloodType: BloodTypeCode;
  percentage: number;
  label?: string;
};

export type BloodCenterInventory = {
  organizationId: string | null;
  name: string | null;
  city: string | null;
  state: string | null;
  updatedAt: string | null;
  levels: InventoryLevel[];
};

export const WEEKDAYS = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
] as const;

export type Weekday = (typeof WEEKDAYS)[number];

export const WEEKDAY_LABELS: Record<Weekday, string> = {
  MONDAY: "Segunda",
  TUESDAY: "Terça",
  WEDNESDAY: "Quarta",
  THURSDAY: "Quinta",
  FRIDAY: "Sexta",
  SATURDAY: "Sábado",
  SUNDAY: "Domingo",
};

export type ScheduleWindow = {
  dayOfWeek: Weekday;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  capacity: number;
};

export type BloodCenterSchedulePayload = {
  weeklyWindows: ScheduleWindow[];
  blockedDates: string[];
};

export type BloodCenterAppointment = {
  donationId: string;
  expectedDate: string | null;
  expectedTime: string | null;
  status: string;
  donorName: string;
  donorBloodType: string | null;
  donorPhone: string | null;
};

export type DonationSlot = {
  startTime: string;
  endTime: string;
  capacity: number;
  booked: number;
  available: number;
};

export type DonationSlotsResponse = {
  organizationId: string | null;
  date: string | null;
  hasSchedule: boolean;
  slots: DonationSlot[];
};

function asRecord(value: unknown): Record<string, unknown> | null {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function readString(value: unknown): string | null {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

function readNumber(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim().length > 0) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return fallback;
}

function clampPercentage(value: unknown): number {
  return Math.min(100, Math.max(0, Math.round(readNumber(value, 0))));
}

function isBloodType(value: string): value is BloodTypeCode {
  return (BLOOD_TYPES as readonly string[]).includes(value);
}

function normalizeItem(item: unknown): InventoryLevel | null {
  const record = asRecord(item);
  if (!record) {
    return null;
  }

  const rawType = readString(record.bloodType) ?? readString(record.type);
  if (!rawType || !isBloodType(rawType)) {
    return null;
  }

  return {
    bloodType: rawType,
    percentage: clampPercentage(record.percentage),
    label: readString(record.label) ?? undefined,
  };
}

export function isWeekday(value: string): value is Weekday {
  return (WEEKDAYS as readonly string[]).includes(value);
}

function normalizeWindow(item: unknown): ScheduleWindow | null {
  const record = asRecord(item);
  if (!record) {
    return null;
  }

  const rawDay = readString(record.dayOfWeek)?.toUpperCase();
  if (!rawDay || !isWeekday(rawDay)) {
    return null;
  }

  const startTime = (readString(record.startTime) ?? "08:00").slice(0, 5);
  const endTime = (readString(record.endTime) ?? "12:00").slice(0, 5);
  const slotDurationMinutes = readNumber(record.slotDurationMinutes, 30);
  const capacity = readNumber(record.capacity, 4);

  return {
    dayOfWeek: rawDay,
    startTime,
    endTime,
    slotDurationMinutes: slotDurationMinutes > 0 ? slotDurationMinutes : 30,
    capacity: capacity > 0 ? capacity : 4,
  };
}

function completeInventoryItems(items: InventoryLevel[]): InventoryLevel[] {
  const byType = new Map(items.map((item) => [item.bloodType, item]));
  return BLOOD_TYPES.map((bloodType) => byType.get(bloodType) ?? { bloodType, percentage: 0 });
}

function normalizeInventory(data: unknown, fallbackOrganizationId?: string): BloodCenterInventory {
  if (Array.isArray(data)) {
    return {
      organizationId: fallbackOrganizationId ?? null,
      name: null,
      city: null,
      state: null,
      updatedAt: null,
      levels: completeInventoryItems(data.map(normalizeItem).filter((item): item is InventoryLevel => item !== null)),
    };
  }

  const record = asRecord(data);
  const nested = Array.isArray(record?.levels)
    ? record.levels
    : Array.isArray(record?.items)
      ? record.items
      : [];

  return {
    organizationId: readString(record?.organizationId) ?? fallbackOrganizationId ?? null,
    name: readString(record?.name),
    city: readString(record?.city),
    state: readString(record?.state),
    updatedAt: readString(record?.updatedAt),
    levels: completeInventoryItems(nested.map(normalizeItem).filter((item): item is InventoryLevel => item !== null)),
  };
}

function worstOfInventories(inventories: BloodCenterInventory[]): InventoryLevel[] {
  const mins = new Map<BloodTypeCode, number>();

  for (const inventory of inventories) {
    for (const item of inventory.levels) {
      const current = mins.get(item.bloodType);
      mins.set(item.bloodType, current == null ? item.percentage : Math.min(current, item.percentage));
    }
  }

  return BLOOD_TYPES.map((bloodType) => ({
    bloodType,
    percentage: mins.get(bloodType) ?? 0,
  }));
}

function normalizeAppointment(item: unknown, index: number): BloodCenterAppointment {
  const record = asRecord(item);
  return {
    donationId: readString(record?.donationId) ?? `appointment-${index}`,
    expectedDate: readString(record?.expectedDate) ?? readString(record?.date),
    expectedTime: readString(record?.expectedTime) ?? readString(record?.time),
    status: readString(record?.status) ?? "PENDING",
    donorName: readString(record?.donorName) ?? "Doador",
    donorBloodType: readString(record?.donorBloodType) ?? readString(record?.bloodType),
    donorPhone: readString(record?.donorPhone) ?? readString(record?.phone),
  };
}

function normalizeSlot(item: unknown): DonationSlot | null {
  const record = asRecord(item);
  if (!record) {
    return null;
  }

  const startTime = (readString(record.startTime) ?? readString(record.time) ?? "").slice(0, 5);
  if (!startTime) {
    return null;
  }

  const capacity = readNumber(record.capacity, 0);
  const booked = readNumber(record.booked, 0);
  const available = record.available == null ? Math.max(capacity - booked, 0) : readNumber(record.available, 0);

  return {
    startTime,
    endTime: (readString(record.endTime) ?? startTime).slice(0, 5),
    capacity,
    booked,
    available,
  };
}

export async function searchBloodCenters(
  query: string,
  limit = 10,
): Promise<BloodCenterSearchResult[]> {
  const trimmed = query.trim();
  if (trimmed.length < 2) {
    return [];
  }

  const response = await api.get<BloodCenterSearchResult[]>("/blood-centers/search", {
    params: { q: trimmed, limit },
  });

  return response.data.map((item) => ({
    organizationId: item.organizationId,
    name: item.name,
    city: item.city ?? null,
    state: item.state ?? null,
  }));
}

export async function fetchRegionalInventory(): Promise<InventoryLevel[]> {
  const { data } = await api.get<unknown>("/blood-centers/inventory");

  if (!Array.isArray(data) || data.length === 0) {
    return [];
  }

  const inventories = data.map((item) => normalizeInventory(item));
  return worstOfInventories(inventories);
}

export async function fetchOwnInventory(organizationId: string): Promise<BloodCenterInventory> {
  const { data } = await api.get<unknown>(`/blood-centers/${organizationId}/inventory`);
  return normalizeInventory(data, organizationId);
}

export async function saveInventory(items: InventoryLevel[]): Promise<void> {
  await api.put("/blood-centers/inventory", {
    items: items.map((item) => ({
      bloodType: item.bloodType,
      percentage: item.percentage,
    })),
  });
}

export async function saveSchedule(payload: BloodCenterSchedulePayload): Promise<void> {
  await api.put("/blood-centers/schedule", {
    weeklyWindows: payload.weeklyWindows.map((window) => ({
      dayOfWeek: window.dayOfWeek,
      startTime: window.startTime,
      endTime: window.endTime,
      slotDurationMinutes: window.slotDurationMinutes,
      capacity: window.capacity,
    })),
    blockedDates: payload.blockedDates,
  });
}

export async function fetchOwnSchedule(): Promise<BloodCenterSchedulePayload> {
  const { data } = await api.get<unknown>("/blood-centers/schedule");
  const record = asRecord(data);
  const windowsSource = Array.isArray(record?.weeklyWindows) ? record.weeklyWindows : [];
  const blockedSource = Array.isArray(record?.blockedDates) ? record.blockedDates : [];

  return {
    weeklyWindows: windowsSource
      .map(normalizeWindow)
      .filter((item): item is ScheduleWindow => item !== null),
    blockedDates: blockedSource
      .map((item) => readString(item))
      .filter((item): item is string => Boolean(item)),
  };
}

function toLocalIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function addDaysLocalIso(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return toLocalIsoDate(date);
}

export async function fetchAppointments(from?: string, to?: string): Promise<BloodCenterAppointment[]> {
  const { data } = await api.get<unknown>("/blood-centers/appointments", {
    params: {
      from: from ?? addDaysLocalIso(-14),
      to: to ?? addDaysLocalIso(60),
    },
  });

  const list = Array.isArray(data) ? data : [];
  return list.map(normalizeAppointment);
}

export async function fetchDonationSlots(
  organizationId: string,
  date: string,
): Promise<DonationSlotsResponse> {
  const { data } = await api.get<unknown>(`/blood-centers/${organizationId}/slots`, {
    params: { date },
  });

  const record = asRecord(data);
  const slotsSource = Array.isArray(data) ? data : Array.isArray(record?.slots) ? record.slots : [];
  const slots = slotsSource.map(normalizeSlot).filter((item): item is DonationSlot => item !== null);

  return {
    organizationId: readString(record?.organizationId) ?? organizationId,
    date: readString(record?.date) ?? date,
    hasSchedule: typeof record?.hasSchedule === "boolean" ? record.hasSchedule : slots.length > 0,
    slots,
  };
}
