export type UserRole = "DONOR" | "REQUESTER" | "BLOOD_CENTER" | "SYSTEM_ADMIN";

export type LoginResponse = {
  tokenType: string;
  accessToken: string;
  expiresIn?: number;
  partyId: string;
  roles: string[];
};

export type AuthSession = {
  tokenType: string;
  accessToken: string;
  expiresAt: number;
  partyId: string;
  roles: string[];
};

export type LoginCredentials = {
  email: string;
  password: string;
};
