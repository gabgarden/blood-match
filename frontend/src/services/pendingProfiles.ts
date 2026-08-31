import { isAxiosError } from "axios";
import {
  createBloodCenterProfile,
  createDonorProfile,
  createRequesterProfile,
} from "./profileService";

export const PENDING_ROLES_KEY = "bloodmatch.pendingRoles";

export type PendingDonorProfile = {
  bloodType: string;
  weight: number;
  lastDonationDate?: string;
};

export type PendingProfiles = {
  email: string;
  partyType: "person" | "organization";
  donor?: PendingDonorProfile;
  registerAsBloodCenter?: boolean;
};

function isConflictError(error: unknown): boolean {
  return isAxiosError(error) && error.response?.status === 409;
}

export function savePendingProfiles(payload: PendingProfiles): void {
  sessionStorage.setItem(PENDING_ROLES_KEY, JSON.stringify(payload));
}

export function loadPendingProfiles(): PendingProfiles | null {
  const raw = sessionStorage.getItem(PENDING_ROLES_KEY);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as PendingProfiles;
    if (!parsed?.email || (parsed.partyType !== "person" && parsed.partyType !== "organization")) {
      sessionStorage.removeItem(PENDING_ROLES_KEY);
      return null;
    }
    return parsed;
  } catch {
    sessionStorage.removeItem(PENDING_ROLES_KEY);
    return null;
  }
}

export function clearPendingProfiles(): void {
  sessionStorage.removeItem(PENDING_ROLES_KEY);
}

export function getPendingRegisterEmail(): string | null {
  const email = loadPendingProfiles()?.email?.trim();
  return email ? email : null;
}

/** Creates roles saved at register time. Returns true when JWT should be refreshed. */
export async function completePendingProfiles(partyId: string): Promise<boolean> {
  const pending = loadPendingProfiles();
  if (!pending) {
    return false;
  }

  if (pending.donor) {
    try {
      await createDonorProfile({
        personId: partyId,
        bloodType: pending.donor.bloodType,
        weight: pending.donor.weight,
        lastDonationDate: pending.donor.lastDonationDate,
      });
    } catch (error) {
      if (!isConflictError(error)) {
        throw error;
      }
    }
  }

  if (pending.registerAsBloodCenter) {
    try {
      await createBloodCenterProfile(partyId);
    } catch (error) {
      if (!isConflictError(error)) {
        throw error;
      }
    }
  }

  try {
    await createRequesterProfile(partyId);
  } catch (error) {
    if (!isConflictError(error)) {
      throw error;
    }
  }

  return true;
}
