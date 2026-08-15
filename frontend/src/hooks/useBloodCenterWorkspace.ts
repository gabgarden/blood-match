import { useCallback, useEffect, useState } from "react";
import {
  BLOOD_TYPES,
  fetchAppointments,
  fetchDonationSlots,
  fetchOwnInventory,
  fetchOwnSchedule,
  saveInventory,
  saveSchedule,
  type BloodCenterAppointment,
  type DonationSlotsResponse,
  type InventoryLevel,
  type ScheduleWindow,
  type Weekday,
} from "../services/bloodCenterService";
import { extractApiErrorMessage } from "../utils/apiError";

function toLocalIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function addDaysIso(base: Date, days: number): string {
  const next = new Date(base);
  next.setDate(next.getDate() + days);
  return toLocalIsoDate(next);
}

const emptyItems = (): InventoryLevel[] => BLOOD_TYPES.map((bloodType) => ({ bloodType, percentage: 0 }));

export function useBloodCenterWorkspace(organizationId: string | null, enabled: boolean) {
  const [items, setItems] = useState<InventoryLevel[]>(emptyItems);
  const [windows, setWindows] = useState<ScheduleWindow[]>([]);
  const [blockedDates, setBlockedDates] = useState<string[]>([]);
  const [appointments, setAppointments] = useState<BloodCenterAppointment[]>([]);
  const [previewDate, setPreviewDate] = useState(toLocalIsoDate(new Date()));
  const [previewSlots, setPreviewSlots] = useState<DonationSlotsResponse | null>(null);

  const [isLoadingInventory, setIsLoadingInventory] = useState(false);
  const [isLoadingAppointments, setIsLoadingAppointments] = useState(false);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [isSavingInventory, setIsSavingInventory] = useState(false);
  const [isSavingSchedule, setIsSavingSchedule] = useState(false);

  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const rangeFrom = toLocalIsoDate(new Date());
  const rangeTo = addDaysIso(new Date(), 14);

  const loadInventory = useCallback(async () => {
    if (!organizationId) {
      return;
    }

    setIsLoadingInventory(true);
    try {
      const inventory = await fetchOwnInventory(organizationId);
      setItems(inventory.levels);
    } catch {
      setItems(emptyItems());
    } finally {
      setIsLoadingInventory(false);
    }
  }, [organizationId]);

  const loadAppointments = useCallback(async () => {
    setIsLoadingAppointments(true);
    try {
      const next = await fetchAppointments(rangeFrom, rangeTo);
      setAppointments(next);
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível carregar as marcações."));
    } finally {
      setIsLoadingAppointments(false);
    }
  }, [rangeFrom, rangeTo]);

  const loadSchedule = useCallback(async () => {
    try {
      const schedule = await fetchOwnSchedule();
      setWindows(schedule.weeklyWindows);
      setBlockedDates(schedule.blockedDates);
    } catch {
      setWindows([]);
      setBlockedDates([]);
    }
  }, []);

  useEffect(() => {
    if (!enabled || !organizationId) {
      return;
    }

    void loadInventory();
    void loadAppointments();
    void loadSchedule();
  }, [enabled, organizationId, loadInventory, loadAppointments, loadSchedule]);

  async function handleSaveInventory() {
    setIsSavingInventory(true);
    setFeedback(null);
    setErrorMessage(null);

    try {
      await saveInventory(items);
      setFeedback("Estoque atualizado com sucesso.");
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível salvar o estoque."));
    } finally {
      setIsSavingInventory(false);
    }
  }

  async function handleSaveSchedule() {
    setIsSavingSchedule(true);
    setFeedback(null);
    setErrorMessage(null);

    try {
      await saveSchedule({ weeklyWindows: windows, blockedDates });
      setFeedback("Horários publicados com sucesso.");
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível salvar a agenda."));
    } finally {
      setIsSavingSchedule(false);
    }
  }

  async function loadSlotPreview(date: string) {
    if (!organizationId) {
      return;
    }

    setIsLoadingPreview(true);
    setPreviewDate(date);

    try {
      const response = await fetchDonationSlots(organizationId, date);
      setPreviewSlots(response);
    } catch (error) {
      setPreviewSlots(null);
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível pré-visualizar os horários."));
    } finally {
      setIsLoadingPreview(false);
    }
  }

  function updateItemPercentage(bloodType: InventoryLevel["bloodType"], percentage: number) {
    setItems((current) =>
      current.map((item) => (item.bloodType === bloodType ? { ...item, percentage } : item)),
    );
  }

  function addWindow(window: ScheduleWindow) {
    setWindows((current) => [...current, window]);
  }

  function removeWindow(index: number) {
    setWindows((current) => current.filter((_, itemIndex) => itemIndex !== index));
  }

  function addBlockedDate(date: string) {
    if (!date) {
      return;
    }
    setBlockedDates((current) => (current.includes(date) ? current : [...current, date].sort()));
  }

  function removeBlockedDate(date: string) {
    setBlockedDates((current) => current.filter((item) => item !== date));
  }

  return {
    items,
    windows,
    blockedDates,
    appointments,
    previewDate,
    previewSlots,
    rangeFrom,
    rangeTo,
    isLoadingInventory,
    isLoadingAppointments,
    isLoadingPreview,
    isSavingInventory,
    isSavingSchedule,
    feedback,
    errorMessage,
    updateItemPercentage,
    addWindow,
    removeWindow,
    addBlockedDate,
    removeBlockedDate,
    handleSaveInventory,
    handleSaveSchedule,
    loadSlotPreview,
  };
}

export type DraftWindow = {
  dayOfWeek: Weekday;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  capacity: number;
};

export const DEFAULT_DRAFT_WINDOW: DraftWindow = {
  dayOfWeek: "MONDAY",
  startTime: "08:00",
  endTime: "12:00",
  slotDurationMinutes: 30,
  capacity: 4,
};
