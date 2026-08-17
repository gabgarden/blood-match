#!/usr/bin/env python3
"""
Simulador de uso recorrente do BloodMatch.

Provoca a arquitetura de fulfillment (pool por hemocentro + FIFO +
contador materializado na escrita + @Version otimista) com:

  - rajadas concorrentes de doações completadas no mesmo hemocentro
  - ondas recorrentes (uso contínuo) com datas/prazos flexíveis
  - leituras misturadas às escritas
  - pendente -> complete em paralelo
  - isolamento entre hemocentros
  - estabilização: espera o estado parar de oscilar e compara com um
    oráculo FIFO idêntico ao DonationRequestFulfillmentService

A API usa LocalDate.now() no servidor para dateRequested e para a
janela de elegibilidade. Doações com data anterior a dateRequested
NÃO entram no pool da request. Por isso as ondas recorrentes simulam
chegadas sucessivas no mesmo dia operacional, com dateLimit variável.

Requisitos: Python 3.10+, API no ar, geocoding (mesmo endereço do seed).

Exemplos:
  python scripts/simulate-usage.py --scenario all
  python scripts/simulate-usage.py --scenario concurrency --concurrency 16
  python scripts/simulate-usage.py --scenario recurring --waves 4 --wave-size 6 \\
      --today 2026-08-16 --date-limits 7,14,21,30 --report sim-report.json
"""

from __future__ import annotations

import argparse
import json
import os
import random
import statistics
import string
import sys
import threading
import time
import uuid
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta, timezone
from typing import Any

BLOOD_TYPES = ["O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"]
URGENCIES = ["CRITICAL", "MEDIUM", "LOW"]
COMPATIBLE_RECIPIENTS = {
    "O-": BLOOD_TYPES,
    "O+": ["O+", "A+", "B+", "AB+"],
    "A-": ["A-", "A+", "AB-", "AB+"],
    "A+": ["A+", "AB+"],
    "B-": ["B-", "B+", "AB-", "AB+"],
    "B+": ["B+", "AB+"],
    "AB-": ["AB-", "AB+"],
    "AB+": ["AB+"],
}
CAMPOS_ADDRESS = {
    "street": "Rua Rocha Leão, 2 - Caju",
    "city": "Campos dos Goytacazes",
    "state": "RJ",
    "zipCode": "28035-045",
}
LOCK_HINT = "changed by another operation"
DEFAULT_PASSWORD = "Senha12345!"


# ---------------------------------------------------------------------------
# HTTP
# ---------------------------------------------------------------------------


@dataclass
class HttpResult:
    status: int
    body: Any
    elapsed_ms: float
    error: str | None = None

    @property
    def ok(self) -> bool:
        return 200 <= self.status < 300

    @property
    def is_lock_conflict(self) -> bool:
        text = ""
        if isinstance(self.body, dict):
            text = str(self.body.get("error") or "")
        elif self.body is not None:
            text = str(self.body)
        return self.status in (400, 409, 500) and LOCK_HINT in text


class ApiClient:
    def __init__(self, base_url: str, timeout_s: float = 30.0) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout_s = timeout_s

    def request(
        self,
        method: str,
        path: str,
        token: str | None = None,
        payload: Any = None,
    ) -> HttpResult:
        url = f"{self.base_url}{path}"
        data = None
        headers = {"Accept": "application/json"}
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"
        if token:
            headers["Authorization"] = f"Bearer {token}"

        req = urllib.request.Request(url, data=data, headers=headers, method=method)
        started = time.perf_counter()
        try:
            with urllib.request.urlopen(req, timeout=self.timeout_s) as resp:
                raw = resp.read().decode("utf-8")
                body: Any = json.loads(raw) if raw else None
                return HttpResult(resp.status, body, (time.perf_counter() - started) * 1000)
        except urllib.error.HTTPError as exc:
            raw = exc.read().decode("utf-8", errors="replace")
            try:
                body = json.loads(raw) if raw else {"error": str(exc)}
            except json.JSONDecodeError:
                body = {"error": raw or str(exc)}
            return HttpResult(exc.code, body, (time.perf_counter() - started) * 1000, str(exc))
        except Exception as exc:  # noqa: BLE001 — o relatório precisa do erro cru
            return HttpResult(0, {"error": str(exc)}, (time.perf_counter() - started) * 1000, str(exc))


# ---------------------------------------------------------------------------
# Modelo local / oráculo FIFO
# ---------------------------------------------------------------------------


@dataclass
class Actor:
    name: str
    email: str
    party_id: str
    token: str
    blood_type: str | None = None
    kind: str = "donor"


@dataclass
class SimRequest:
    request_id: str
    organization_id: str
    blood_type_needed: str
    goal: int
    date_requested: str
    date_limit: str
    urgency: str
    active: bool = True


@dataclass
class SimDonation:
    donation_id: str
    organization_id: str
    donor_party_id: str
    donor_blood_type: str
    donation_date: str
    status: str = "COMPLETED"


@dataclass
class CallStats:
    label: str
    attempts: int = 0
    successes: int = 0
    lock_conflicts: int = 0
    lost_refresh: int = 0
    other_errors: int = 0
    latencies_ms: list[float] = field(default_factory=list)
    error_samples: list[str] = field(default_factory=list)
    _lock: threading.Lock = field(default_factory=threading.Lock, repr=False)

    def add(self, result: HttpResult, success: bool, lost_refresh: bool = False) -> None:
        sample = f"{result.status} {result.body}"
        with self._lock:
            self.attempts += 1
            self.latencies_ms.append(result.elapsed_ms)
            if result.is_lock_conflict:
                self.lock_conflicts += 1
            if lost_refresh:
                self.lost_refresh += 1
                self.successes += 1
            elif success:
                self.successes += 1
            elif not result.is_lock_conflict:
                self.other_errors += 1
            if not success or lost_refresh:
                if len(self.error_samples) < 8:
                    self.error_samples.append(sample)

    def summary(self) -> dict[str, Any]:
        lats = self.latencies_ms
        return {
            "label": self.label,
            "attempts": self.attempts,
            "successes": self.successes,
            "lock_conflicts": self.lock_conflicts,
            "lost_refresh": self.lost_refresh,
            "other_errors": self.other_errors,
            "error_samples": list(self.error_samples),
            "latency_ms": percentile_block(lats),
        }


def percentile_block(values: list[float]) -> dict[str, float] | None:
    if not values:
        return None
    ordered = sorted(values)
    def pct(p: float) -> float:
        idx = min(len(ordered) - 1, max(0, int(round((p / 100) * (len(ordered) - 1)))))
        return round(ordered[idx], 2)
    return {
        "min": round(ordered[0], 2),
        "p50": pct(50),
        "p95": pct(95),
        "max": round(ordered[-1], 2),
        "mean": round(statistics.fmean(ordered), 2),
    }


def can_donate(donor_type: str, needed: str) -> bool:
    return needed in COMPATIBLE_RECIPIENTS.get(donor_type, [])


def java_uuid_sort_key(value: str) -> tuple[int, int]:
    """Java UUID.compareTo: most/least significant bits as signed longs, not string order."""
    parsed = uuid.UUID(value)

    def signed64(n: int) -> int:
        return n - (1 << 64) if n >= 1 << 63 else n

    return signed64(parsed.int >> 64), signed64(parsed.int & ((1 << 64) - 1))


def fifo_oracle(
    requests: list[SimRequest],
    donations: list[SimDonation],
    current_date: str,
    organization_id: str,
) -> dict[str, int]:
    """Réplica de DonationRequestFulfillmentService.calculate (um hemocentro)."""
    active = [
        r
        for r in requests
        if r.active
        and r.organization_id == organization_id
        and r.date_limit >= current_date
    ]
    active.sort(key=lambda r: (r.date_requested, java_uuid_sort_key(r.request_id)))
    pool = [
        d
        for d in donations
        if d.status == "COMPLETED" and d.organization_id == organization_id
    ]
    pool.sort(key=lambda d: (d.donation_date, java_uuid_sort_key(d.donation_id)))

    fulfilled = {r.request_id: 0 for r in active}
    for donation in pool:
        for request in active:
            if donation.donation_date < request.date_requested:
                continue
            if donation.donation_date > request.date_limit:
                continue
            if not can_donate(donation.donor_blood_type, request.blood_type_needed):
                continue
            if fulfilled[request.request_id] >= request.goal:
                continue
            fulfilled[request.request_id] += 1
            break
    return fulfilled


def self_test_oracle() -> None:
    """Garante que o oráculo replica o caso FIFO básico dos testes Java."""
    r1 = SimRequest("00000000-0000-4000-8000-000000000001", "org", "A+", 2, "2026-08-01", "2026-08-31", "CRITICAL")
    r2 = SimRequest("00000000-0000-4000-8000-000000000002", "org", "A+", 2, "2026-08-02", "2026-08-31", "MEDIUM")
    d1 = SimDonation("00000000-0000-4000-8000-000000000011", "org", "p1", "O-", "2026-08-16")
    d2 = SimDonation("00000000-0000-4000-8000-000000000012", "org", "p2", "O-", "2026-08-16")
    d3 = SimDonation("00000000-0000-4000-8000-000000000013", "org", "p3", "O-", "2026-08-16")
    got = fifo_oracle([r2, r1], [d3, d1, d2], "2026-08-16", "org")
    assert got == {r1.request_id: 2, r2.request_id: 1}, got
    incompatible = SimRequest("00000000-0000-4000-8000-00000000000a", "org", "O-", 1, "2026-08-01", "2026-08-31", "LOW")
    got2 = fifo_oracle(
        [incompatible, r1],
        [SimDonation("00000000-0000-4000-8000-0000000000aa", "org", "p", "A+", "2026-08-16")],
        "2026-08-16",
        "org",
    )
    assert got2 == {incompatible.request_id: 0, r1.request_id: 1}, got2
    other = SimDonation("00000000-0000-4000-8000-0000000000bb", "other", "p", "O-", "2026-08-16")
    got3 = fifo_oracle([r1], [other], "2026-08-16", "org")
    assert got3 == {r1.request_id: 0}, got3
    early_java = "f6b5af69-669b-40ad-b9d3-f643f69f70d3"
    late_java = "3f88f138-cff7-4f1e-b7dc-13b25c3d5a13"
    assert java_uuid_sort_key(early_java) < java_uuid_sort_key(late_java)
    assert late_java < early_java
    r_java_first = SimRequest(early_java, "org", "B+", 2, "2026-08-17", "2026-08-31", "LOW")
    r_java_second = SimRequest(late_java, "org", "B-", 1, "2026-08-17", "2026-08-31", "MEDIUM")
    o_minus = SimDonation("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", "org", "p", "O-", "2026-08-17")
    got4 = fifo_oracle([r_java_second, r_java_first], [o_minus], "2026-08-17", "org")
    assert got4 == {early_java: 1, late_java: 0}, got4
    print("self-test do oráculo FIFO: ok")


def snapshot_signature(rows: list[dict[str, Any]]) -> str:
    items = sorted(
        (r.get("requestId"), r.get("fulfilledBloodBags"), r.get("active"), r.get("goalReached"))
        for r in rows
    )
    return json.dumps(items, separators=(",", ":"))


# ---------------------------------------------------------------------------
# IDs determinísticos o bastante para não colidir com o seed
# ---------------------------------------------------------------------------


def random_suffix(n: int = 6) -> str:
    alphabet = string.ascii_lowercase + string.digits
    return "".join(random.choice(alphabet) for _ in range(n))


def padded(n: int, width: int) -> str:
    return str(n)[-width:].zfill(width)


# ---------------------------------------------------------------------------
# Simulador
# ---------------------------------------------------------------------------


class Simulator:
    def __init__(self, args: argparse.Namespace) -> None:
        self.args = args
        self.api = ApiClient(args.api_url, timeout_s=args.timeout)
        self.run_id = args.run_id or datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S") + random_suffix(4)
        self.today = date.fromisoformat(args.today) if args.today else date.today()
        self.donation_date = (
            date.fromisoformat(args.donation_date) if args.donation_date else self.today
        )
        self.date_limits = [int(x) for x in args.date_limits.split(",") if x.strip()]
        self.password = args.password
        self.findings: list[dict[str, Any]] = []
        self.reports: list[dict[str, Any]] = []
        self._seq = 0
        self._seq_lock = threading.Lock()
        self._id_base = random.randint(50_000_000, 90_000_000)

    def next_seq(self) -> int:
        with self._seq_lock:
            self._seq += 1
            return self._seq

    def note(self, kind: str, message: str, **extra: Any) -> None:
        item = {"kind": kind, "message": message, **extra}
        self.findings.append(item)
        prefix = {"ok": "  [ok]", "warn": "  [warn]", "fail": "  [fail]", "info": "  [info]"}.get(kind, "  [-]")
        print(f"{prefix} {message}")

    # ----- HTTP helpers -----

    def login(self, email: str) -> str:
        result = self.api.request("POST", "/auth/login", payload={"email": email, "password": self.password})
        if not result.ok or not isinstance(result.body, dict) or not result.body.get("accessToken"):
            raise RuntimeError(f"login failed for {email}: {result.status} {result.body}")
        return result.body["accessToken"]

    def register_person(self, name: str, email: str, cpf: str, phone: str) -> str:
        payload = {
            "name": name,
            "cpf": cpf,
            "birthDate": "1998-05-10",
            "email": email,
            "password": self.password,
            "passwordConfirmation": self.password,
            "phoneNumber": phone,
            **CAMPOS_ADDRESS,
        }
        result = self.api.request("POST", "/parties/persons", payload=payload)
        if not result.ok:
            raise RuntimeError(f"register person failed ({email}): {result.status} {result.body}")
        return result.body["id"]

    def register_org(self, name: str, email: str, cnpj: str, phone: str) -> str:
        payload = {
            "name": name,
            "cnpj": cnpj,
            "email": email,
            "password": self.password,
            "passwordConfirmation": self.password,
            "phoneNumber": phone,
            **CAMPOS_ADDRESS,
        }
        result = self.api.request("POST", "/parties/organizations", payload=payload)
        if not result.ok:
            raise RuntimeError(f"register org failed ({email}): {result.status} {result.body}")
        return result.body["id"]

    def provision_center(self, label: str) -> Actor:
        seq = self.next_seq()
        email = f"sim.{self.run_id}.{label}.{seq}@blood.local"
        cnpj = padded(self._id_base + seq, 14)
        phone = "2298" + padded(self._id_base + seq, 7)
        org_id = self.register_org(f"Sim {label} {self.run_id}", email, cnpj, phone)
        token = self.login(email)
        r1 = self.api.request("POST", "/requesters", token, {"partyId": org_id})
        r2 = self.api.request("POST", "/blood-centers", token, {"organizationId": org_id})
        if not r1.ok:
            raise RuntimeError(f"register requester failed: {r1.status} {r1.body}")
        if not r2.ok:
            raise RuntimeError(f"register blood center failed: {r2.status} {r2.body}")
        token = self.login(email)
        return Actor(name=label, email=email, party_id=org_id, token=token, kind="center")

    def provision_donor(self, label: str, blood_type: str) -> Actor:
        seq = self.next_seq()
        email = f"sim.{self.run_id}.d.{label}.{seq}@blood.local"
        cpf = padded(self._id_base + seq, 11)
        phone = "2299" + padded(self._id_base + seq, 7)
        person_id = self.register_person(f"Sim Donor {label} {seq}", email, cpf, phone)
        token = self.login(email)
        result = self.api.request(
            "POST",
            "/donors",
            token,
            {"personId": person_id, "bloodType": blood_type, "weight": 72.0},
        )
        if not result.ok:
            raise RuntimeError(f"register donor failed ({email}): {result.status} {result.body}")
        token = self.login(email)
        return Actor(
            name=f"{label}-{seq}",
            email=email,
            party_id=person_id,
            token=token,
            blood_type=blood_type,
            kind="donor",
        )

    def create_requests(self, center: Actor, count: int) -> list[SimRequest]:
        created: list[SimRequest] = []
        for i in range(count):
            needed = BLOOD_TYPES[i % len(BLOOD_TYPES)]
            goal = 1 + (i % 4)
            limit_days = self.date_limits[i % len(self.date_limits)]
            date_limit = (self.today + timedelta(days=limit_days)).isoformat()
            urgency = URGENCIES[i % len(URGENCIES)]
            result = self.api.request(
                "POST",
                "/donation-requests",
                center.token,
                {
                    "partyId": center.party_id,
                    "organizationId": center.party_id,
                    "bloodTypeNeeded": needed,
                    "goalBloodBags": goal,
                    "dateLimit": date_limit,
                    "urgency": urgency,
                    "directedTo": f"sim-wave-{i}",
                },
            )
            if not result.ok:
                raise RuntimeError(f"create request failed: {result.status} {result.body}")
            created.append(
                SimRequest(
                    request_id=result.body["id"],
                    organization_id=center.party_id,
                    blood_type_needed=needed,
                    goal=goal,
                    date_requested=self.today.isoformat(),
                    date_limit=date_limit,
                    urgency=urgency,
                )
            )
            time.sleep(0.02)
        return created

    def list_requests(self, center: Actor) -> list[dict[str, Any]]:
        result = self.api.request("GET", f"/donation-requests/{center.party_id}", center.token)
        if not result.ok:
            raise RuntimeError(f"list requests failed: {result.status} {result.body}")
        return result.body or []

    def donor_history(self, donor: Actor) -> list[dict[str, Any]]:
        result = self.api.request("GET", f"/donors/{donor.party_id}/donations", donor.token)
        if not result.ok:
            return []
        return result.body or []

    def recommendations(self, donor: Actor, include_non_eligible: bool = True) -> HttpResult:
        flag = "true" if include_non_eligible else "false"
        return self.api.request(
            "GET",
            f"/donation-requests/recommendations?personId={donor.party_id}&includeNonEligible={flag}",
            donor.token,
        )

    # ----- doações com retry / detecção de refresh perdido -----

    def complete_donation(
        self,
        donor: Actor,
        organization_id: str,
        donation_date: date,
        stats: CallStats,
        known_ids: set[str] | None = None,
    ) -> SimDonation | None:
        known = known_ids if known_ids is not None else {
            row["donationId"] for row in self.donor_history(donor)
        }
        last: HttpResult | None = None
        for attempt in range(self.args.retries + 1):
            last = self.api.request(
                "POST",
                "/donations/completed",
                donor.token,
                {
                    "personId": donor.party_id,
                    "organizationId": organization_id,
                    "donationDate": donation_date.isoformat(),
                },
            )
            if last.ok:
                stats.add(last, success=True)
                return SimDonation(
                    donation_id=last.body["id"],
                    organization_id=organization_id,
                    donor_party_id=donor.party_id,
                    donor_blood_type=donor.blood_type or "O+",
                    donation_date=donation_date.isoformat(),
                    status=last.body.get("status", "COMPLETED"),
                )
            history = self.donor_history(donor)
            created = [row for row in history if row["donationId"] not in known]
            if created:
                stats.add(last, success=True, lost_refresh=True)
                row = created[-1]
                return SimDonation(
                    donation_id=row["donationId"],
                    organization_id=organization_id,
                    donor_party_id=donor.party_id,
                    donor_blood_type=donor.blood_type or "O+",
                    donation_date=row.get("date") or donation_date.isoformat(),
                    status="COMPLETED",
                )
            if last.is_lock_conflict and attempt < self.args.retries:
                time.sleep(self.args.backoff * (2**attempt))
                continue
            stats.add(last, success=False)
            return None
        if last:
            stats.add(last, success=False)
        return None

    def create_pending(self, donor: Actor, organization_id: str, expected: date) -> str | None:
        result = self.api.request(
            "POST",
            "/donations/create-pending",
            donor.token,
            {
                "personId": donor.party_id,
                "organizationId": organization_id,
                "expectedDate": expected.isoformat(),
            },
        )
        if not result.ok:
            self.note("warn", f"pending donation failed for {donor.email}: {result.status} {result.body}")
            return None
        return result.body["id"]

    def complete_pending(
        self,
        donor: Actor,
        donation_id: str,
        completion: date,
        stats: CallStats,
    ) -> bool:
        last: HttpResult | None = None
        for attempt in range(self.args.retries + 1):
            last = self.api.request(
                "PATCH",
                "/donations/complete",
                donor.token,
                {"donationId": donation_id, "completionDate": completion.isoformat()},
            )
            if last.ok:
                stats.add(last, success=True)
                return True
            # A doação já foi persistida como COMPLETED antes do refresh.
            # Retry aqui vira "Only pending donations can be completed".
            already_completed = last.is_lock_conflict or (
                isinstance(last.body, dict)
                and "Only pending" in str(last.body.get("error") or "")
            )
            if already_completed:
                stats.add(last, success=True, lost_refresh=True)
                return True
            if attempt < self.args.retries:
                time.sleep(self.args.backoff * (2**attempt))
                continue
            stats.add(last, success=False)
            return False
        if last:
            stats.add(last, success=False)
        return False

    def cancel_request(self, center: Actor, request_id: str) -> bool:
        result = self.api.request("DELETE", f"/donation-requests/{request_id}", center.token)
        return result.ok or result.status == 204

    # ----- estabilização -----

    def stabilize(
        self,
        center: Actor,
        requests: list[SimRequest],
        donations: list[SimDonation],
        spare: Actor | None,
    ) -> dict[str, Any]:
        polls = []
        last_sig = None
        stable_hits = 0
        rows: list[dict[str, Any]] = []
        for i in range(self.args.stabilize_polls):
            rows = self.list_requests(center)
            sig = snapshot_signature(rows)
            polls.append({"poll": i, "signature": sig, "count": len(rows)})
            if sig == last_sig:
                stable_hits += 1
            else:
                stable_hits = 0
            last_sig = sig
            if stable_hits >= 1 and i > 0:
                break
            time.sleep(self.args.stabilize_wait)

        by_id = {row["requestId"]: row for row in rows}
        for req in requests:
            row = by_id.get(req.request_id)
            if row is not None:
                req.active = bool(row.get("active", True))
                req.date_requested = row.get("dateRequested") or req.date_requested
                req.date_limit = row.get("dateLimit") or req.date_limit
                req.goal = int(row.get("goalBloodBags") or req.goal)

        observed = {
            row["requestId"]: int(row.get("fulfilledBloodBags") or 0)
            for row in rows
            if row.get("active")
        }
        expected = fifo_oracle(
            requests, donations, self.today.isoformat(), center.party_id
        )
        mismatches = []
        for req_id, exp in expected.items():
            got = observed.get(req_id, 0)
            if got != exp:
                mismatches.append({"requestId": req_id, "expected": exp, "observed": got})

        overfill = [
            {
                "requestId": row["requestId"],
                "fulfilled": row.get("fulfilledBloodBags"),
                "goal": row.get("goalBloodBags"),
            }
            for row in rows
            if int(row.get("fulfilledBloodBags") or 0) > int(row.get("goalBloodBags") or 0)
        ]
        flag_mismatch = [
            {
                "requestId": row["requestId"],
                "fulfilled": row.get("fulfilledBloodBags"),
                "goal": row.get("goalBloodBags"),
                "goalReached": row.get("goalReached"),
            }
            for row in rows
            if bool(row.get("goalReached"))
            != (int(row.get("fulfilledBloodBags") or 0) >= int(row.get("goalBloodBags") or 0))
        ]

        nudged = False
        nudge_donation: SimDonation | None = None
        if mismatches and spare is not None:
            self.note("warn", "oráculo diverge após a rajada; disparando doação-nudge para forçar refresh")
            stats = CallStats("nudge")
            nudge_donation = self.complete_donation(
                spare, center.party_id, self.donation_date, stats
            )
            nudged = nudge_donation is not None
            if nudge_donation:
                donations.append(nudge_donation)
                time.sleep(self.args.stabilize_wait)
                rows = self.list_requests(center)
                observed = {
                    row["requestId"]: int(row.get("fulfilledBloodBags") or 0)
                    for row in rows
                    if row.get("active")
                }
                expected = fifo_oracle(
                    requests, donations, self.today.isoformat(), center.party_id
                )
                mismatches = [
                    {"requestId": rid, "expected": exp, "observed": observed.get(rid, 0)}
                    for rid, exp in expected.items()
                    if observed.get(rid, 0) != exp
                ]

        conserved = sum(observed.values())
        allocatable = sum(expected.values())
        verdict = "pass" if not mismatches and not overfill and not flag_mismatch else "fail"
        if verdict == "pass":
            self.note(
                "ok",
                f"estado estável: fulfilled={conserved} bate com o oráculo FIFO "
                f"(nudge={'sim' if nudged else 'não'})",
            )
        else:
            self.note(
                "fail",
                f"estado instável/incorreto: {len(mismatches)} divergências FIFO, "
                f"{len(overfill)} overfill, {len(flag_mismatch)} flags",
            )

        return {
            "verdict": verdict,
            "polls": len(polls),
            "nudged": nudged,
            "nudgeDonationId": nudge_donation.donation_id if nudge_donation else None,
            "observedFulfilledSum": conserved,
            "oracleFulfilledSum": allocatable,
            "mismatches": mismatches,
            "overfill": overfill,
            "goalReachedFlagMismatch": flag_mismatch,
            "snapshot": rows,
        }

    def burst(
        self,
        donors: list[Actor],
        center: Actor,
        donation_date: date,
        label: str,
        sequential: bool = False,
    ) -> tuple[list[SimDonation], CallStats]:
        stats = CallStats(label)
        created: list[SimDonation] = []
        lock = threading.Lock()

        def one(donor: Actor) -> SimDonation | None:
            return self.complete_donation(donor, center.party_id, donation_date, stats)

        if sequential:
            for donor in donors:
                item = one(donor)
                if item:
                    created.append(item)
        else:
            with ThreadPoolExecutor(max_workers=max(1, self.args.workers)) as pool:
                futs = [pool.submit(one, d) for d in donors]
                for fut in as_completed(futs):
                    item = fut.result()
                    if item:
                        with lock:
                            created.append(item)
        return created, stats

    # ----- cenários -----

    def scenario_concurrency(self) -> dict[str, Any]:
        print("\n== cenário: concurrency ==")
        n = self.args.concurrency
        center = self.provision_center("conc")
        donors = [self.provision_donor("conc", BLOOD_TYPES[i % 8]) for i in range(n)]
        spare = self.provision_donor("spare", "O-")
        requests = self.create_requests(center, self.args.requests)
        self.note("info", f"hemocentro {center.party_id[:8]}… | {n} doadores | {len(requests)} requests")

        donations, stats = self.burst(donors, center, self.donation_date, "concurrency-burst")
        self.note(
            "info",
            f"burst: {stats.successes}/{stats.attempts} ok, "
            f"{stats.lock_conflicts} lock, {stats.lost_refresh} refresh perdido",
        )
        if stats.lock_conflicts:
            self.note(
                "warn",
                "conflito otimista observado — @Version nas requests está atuando no write path",
            )
        if stats.lost_refresh:
            self.note(
                "warn",
                "doação persistida com refresh falho: o contador pode ficar stale até o próximo write",
            )

        stab = self.stabilize(center, requests, donations, spare)
        return {
            "scenario": "concurrency",
            "centerId": center.party_id,
            "donors": n,
            "requests": len(requests),
            "donationDate": self.donation_date.isoformat(),
            "write": stats.summary(),
            "stabilization": stab,
        }

    def scenario_recurring(self) -> dict[str, Any]:
        print("\n== cenário: recurring ==")
        waves = self.args.waves
        wave_size = self.args.wave_size
        center = self.provision_center("rec")
        donors = [
            self.provision_donor("rec", BLOOD_TYPES[i % 8])
            for i in range(waves * wave_size + 1)
        ]
        spare = donors[-1]
        wave_donors = donors[:-1]
        requests = self.create_requests(center, self.args.requests)
        all_donations: list[SimDonation] = []
        wave_reports: list[dict[str, Any]] = []

        extra_request: SimRequest | None = None
        cancelled: SimRequest | None = None
        stale_after_create: dict[str, Any] | None = None
        stale_after_cancel: dict[str, Any] | None = None

        for w in range(waves):
            chunk = wave_donors[w * wave_size : (w + 1) * wave_size]
            print(f"  onda {w + 1}/{waves} ({len(chunk)} doações, date={self.donation_date.isoformat()})")
            donations, stats = self.burst(
                chunk, center, self.donation_date, f"wave-{w + 1}"
            )
            all_donations.extend(donations)
            stab = self.stabilize(center, requests, all_donations, spare if w == waves - 1 else None)
            wave_reports.append(
                {
                    "wave": w + 1,
                    "write": stats.summary(),
                    "stabilization": {
                        k: stab[k]
                        for k in (
                            "verdict",
                            "nudged",
                            "observedFulfilledSum",
                            "oracleFulfilledSum",
                            "mismatches",
                        )
                    },
                    "writeLatencyMs": stats.summary()["latency_ms"],
                }
            )
            self.note(
                "info",
                f"onda {w + 1}: p95={stats.summary()['latency_ms']['p95'] if stats.latencies_ms else '?'}ms "
                f"fulfilled={stab['observedFulfilledSum']}",
            )

            if w == 0:
                extra = self.create_requests(center, 1)[0]
                extra_request = extra
                requests.append(extra)
                rows = self.list_requests(center)
                extra_row = next((r for r in rows if r["requestId"] == extra.request_id), None)
                fulfilled = int((extra_row or {}).get("fulfilledBloodBags") or 0)
                surplus = max(
                    0,
                    len(all_donations) - sum(r.goal for r in requests if r.request_id != extra.request_id),
                )
                stale_after_create = {
                    "requestId": extra.request_id,
                    "fulfilledBloodBags": fulfilled,
                    "expectedUntilNextWrite": 0,
                    "possibleSurplusInPool": surplus > 0 or any(
                        can_donate(d.donor_blood_type, extra.blood_type_needed) for d in all_donations
                    ),
                }
                if fulfilled == 0:
                    self.note(
                        "ok",
                        "nova request nasceu em fulfilled=0 (refresh só no write de doação) — "
                        "stale-on-create confirmado",
                    )
                else:
                    self.note(
                        "warn",
                        f"nova request já veio com fulfilled={fulfilled} — comportamento inesperado",
                    )

            if w == 1 and requests:
                target = next((r for r in requests if r.active and r.request_id != (extra_request.request_id if extra_request else "")), requests[0])
                before = self.list_requests(center)
                before_map = {r["requestId"]: r.get("fulfilledBloodBags") for r in before}
                if self.cancel_request(center, target.request_id):
                    target.active = False
                    cancelled = target
                    after = self.list_requests(center)
                    after_map = {r["requestId"]: r.get("fulfilledBloodBags") for r in after}
                    siblings_changed = [
                        rid
                        for rid, val in after_map.items()
                        if rid != target.request_id and before_map.get(rid) != val
                    ]
                    stale_after_cancel = {
                        "cancelledRequestId": target.request_id,
                        "siblingsChangedImmediately": siblings_changed,
                    }
                    if not siblings_changed:
                        self.note(
                            "ok",
                            "cancelar request NÃO redistribuiu bolsas na hora — stale-on-cancel confirmado",
                        )
                    else:
                        self.note(
                            "warn",
                            f"cancelamento redistribuiu imediatamente {len(siblings_changed)} irmãs",
                        )

        latencies = [w["writeLatencyMs"]["mean"] for w in wave_reports if w.get("writeLatencyMs")]
        growth = None
        if len(latencies) >= 2:
            growth = round(latencies[-1] - latencies[0], 2)
            self.note(
                "info",
                f"custo do write path: mean onda1={latencies[0]}ms -> onda{waves}={latencies[-1]}ms "
                f"(Δ={growth}ms). O refresh é O(doações × requests) do hemocentro.",
            )

        return {
            "scenario": "recurring",
            "centerId": center.party_id,
            "waves": waves,
            "waveSize": wave_size,
            "donationDate": self.donation_date.isoformat(),
            "dateLimitsDays": self.date_limits,
            "staleAfterCreate": stale_after_create,
            "staleAfterCancel": stale_after_cancel,
            "cancelledRequestId": cancelled.request_id if cancelled else None,
            "writePathMeanGrowthMs": growth,
            "wavesDetail": wave_reports,
        }

    def scenario_mixed_reads(self) -> dict[str, Any]:
        print("\n== cenário: mixed-reads ==")
        writers_n = max(4, self.args.concurrency // 2)
        readers_n = max(4, self.args.concurrency // 2)
        center = self.provision_center("mix")
        writers = [self.provision_donor("mixw", BLOOD_TYPES[i % 8]) for i in range(writers_n)]
        readers = [self.provision_donor("mixr", BLOOD_TYPES[i % 8]) for i in range(readers_n)]
        spare = self.provision_donor("mixs", "O-")
        requests = self.create_requests(center, self.args.requests)
        write_stats = CallStats("mixed-write")
        read_stats = CallStats("mixed-read")
        donations: list[SimDonation] = []
        stale_reads = 0
        lock = threading.Lock()
        stop = threading.Event()

        def write_one(donor: Actor) -> None:
            item = self.complete_donation(donor, center.party_id, self.donation_date, write_stats)
            if item:
                with lock:
                    donations.append(item)

        def read_loop(donor: Actor) -> None:
            nonlocal stale_reads
            while not stop.is_set():
                rec = self.recommendations(donor, include_non_eligible=True)
                listed = self.api.request(
                    "GET", f"/donation-requests/{center.party_id}", center.token
                )
                read_stats.add(rec, success=rec.ok)
                if listed.ok and isinstance(listed.body, list):
                    for row in listed.body:
                        goal = int(row.get("goalBloodBags") or 0)
                        fulfilled = int(row.get("fulfilledBloodBags") or 0)
                        if fulfilled > goal:
                            stale_reads += 1
                time.sleep(0.05)

        reader_threads = [threading.Thread(target=read_loop, args=(d,), daemon=True) for d in readers]
        for t in reader_threads:
            t.start()
        with ThreadPoolExecutor(max_workers=max(1, self.args.workers)) as pool:
            list(pool.map(write_one, writers))
        time.sleep(0.2)
        stop.set()
        for t in reader_threads:
            t.join(timeout=2)

        stab = self.stabilize(center, requests, donations, spare)
        self.note(
            "info",
            f"leituras durante escrita: {read_stats.successes}/{read_stats.attempts} ok, "
            f"overfill visível em {stale_reads} snapshots",
        )
        return {
            "scenario": "mixed-reads",
            "write": write_stats.summary(),
            "read": read_stats.summary(),
            "visibleOverfillSnapshots": stale_reads,
            "stabilization": stab,
        }

    def scenario_pending(self) -> dict[str, Any]:
        print("\n== cenário: pending-complete ==")
        n = max(4, min(self.args.concurrency, 12))
        center = self.provision_center("pend")
        donors = [self.provision_donor("pend", BLOOD_TYPES[i % 8]) for i in range(n)]
        spare = self.provision_donor("pends", "O-")
        requests = self.create_requests(center, self.args.requests)
        pending_ids: list[tuple[Actor, str]] = []
        for donor in donors:
            pid = self.create_pending(donor, center.party_id, self.today)
            if pid:
                pending_ids.append((donor, pid))

        mid = self.list_requests(center)
        mid_sum = sum(int(r.get("fulfilledBloodBags") or 0) for r in mid)
        if mid_sum == 0:
            self.note("ok", "pending NÃO alterou fulfilledBloodBags — refresh só no complete")
        else:
            self.note("warn", f"pending alterou o pool (sum={mid_sum}) — inesperado")

        stats = CallStats("pending-complete")
        donations: list[SimDonation] = []
        lock = threading.Lock()

        def one(pair: tuple[Actor, str]) -> None:
            donor, donation_id = pair
            ok = self.complete_pending(donor, donation_id, self.donation_date, stats)
            if ok:
                with lock:
                    donations.append(
                        SimDonation(
                            donation_id=donation_id,
                            organization_id=center.party_id,
                            donor_party_id=donor.party_id,
                            donor_blood_type=donor.blood_type or "O+",
                            donation_date=self.donation_date.isoformat(),
                        )
                    )

        with ThreadPoolExecutor(max_workers=max(1, self.args.workers)) as pool:
            list(pool.map(one, pending_ids))

        stab = self.stabilize(center, requests, donations, spare)
        return {
            "scenario": "pending-complete",
            "pendingCreated": len(pending_ids),
            "fulfilledAfterPendingOnly": mid_sum,
            "write": stats.summary(),
            "stabilization": stab,
        }

    def scenario_isolation(self) -> dict[str, Any]:
        print("\n== cenário: isolation ==")
        a = self.provision_center("isoA")
        b = self.provision_center("isoB")
        donors_a = [self.provision_donor("isoA", "O-") for _ in range(3)]
        donors_b = [self.provision_donor("isoB", "O-") for _ in range(2)]
        reqs_a = self.create_requests(a, 2)
        reqs_b = self.create_requests(b, 2)
        stats = CallStats("isolation")
        dons_a = []
        dons_b = []
        for d in donors_a:
            item = self.complete_donation(d, a.party_id, self.donation_date, stats)
            if item:
                dons_a.append(item)
        for d in donors_b:
            item = self.complete_donation(d, b.party_id, self.donation_date, stats)
            if item:
                dons_b.append(item)

        rows_a = self.list_requests(a)
        rows_b = self.list_requests(b)
        sum_a = sum(int(r.get("fulfilledBloodBags") or 0) for r in rows_a)
        sum_b = sum(int(r.get("fulfilledBloodBags") or 0) for r in rows_b)
        leaked = False
        if sum_a > len(dons_a) or sum_b > len(dons_b):
            leaked = True
            self.note("fail", f"possível vazamento de pool A={sum_a}/{len(dons_a)} B={sum_b}/{len(dons_b)}")
        else:
            self.note("ok", f"isolamento ok: A fulfilled={sum_a} de {len(dons_a)} doações; B={sum_b} de {len(dons_b)}")

        exp_a = fifo_oracle(reqs_a, dons_a, self.today.isoformat(), a.party_id)
        exp_b = fifo_oracle(reqs_b, dons_b, self.today.isoformat(), b.party_id)
        return {
            "scenario": "isolation",
            "centerA": {"id": a.party_id, "fulfilledSum": sum_a, "donations": len(dons_a), "oracle": exp_a},
            "centerB": {"id": b.party_id, "fulfilledSum": sum_b, "donations": len(dons_b), "oracle": exp_b},
            "leaked": leaked,
            "write": stats.summary(),
        }

    def run(self) -> dict[str, Any]:
        if self.donation_date > date.today():
            raise SystemExit("--donation-date / --today não pode ser no futuro (a API rejeita).")
        if self.donation_date < date.today() or self.today < date.today():
            self.note(
                "warn",
                "dateRequested das requests é sempre o hoje do servidor. "
                "Doação com data anterior a isso não preenche request criada hoje.",
            )

        ping = self.api.request("POST", "/auth/login", payload={"email": "invalid@x", "password": "x"})
        if ping.status == 0:
            raise SystemExit(f"API inacessível em {self.args.api_url}: {ping.error}")

        selected = self.args.scenario
        runners = {
            "concurrency": self.scenario_concurrency,
            "recurring": self.scenario_recurring,
            "mixed-reads": self.scenario_mixed_reads,
            "pending": self.scenario_pending,
            "isolation": self.scenario_isolation,
        }
        order = list(runners) if selected == "all" else [selected]
        started = time.perf_counter()
        print(f"BloodMatch simulator  run={self.run_id}  api={self.args.api_url}")
        print(f"today={self.today.isoformat()}  donationDate={self.donation_date.isoformat()}  "
              f"dateLimits={self.date_limits}  scenario={selected}")

        for name in order:
            try:
                self.reports.append(runners[name]())
            except Exception as exc:  # noqa: BLE001
                self.note("fail", f"cenário {name} abortou: {exc}")
                self.reports.append({"scenario": name, "error": str(exc)})

        elapsed = round(time.perf_counter() - started, 2)
        fails = [f for f in self.findings if f["kind"] == "fail"]
        payload = {
            "runId": self.run_id,
            "api": self.args.api_url,
            "today": self.today.isoformat(),
            "donationDate": self.donation_date.isoformat(),
            "dateLimitsDays": self.date_limits,
            "elapsedSeconds": elapsed,
            "scenarios": self.reports,
            "findings": self.findings,
            "failed": len(fails),
            "whatThisEvaluates": EVALUATION_NOTES,
        }
        print(f"\nconcluído em {elapsed}s  findings={len(self.findings)}  fails={len(fails)}")
        return payload

    def compact(self, payload: dict[str, Any]) -> dict[str, Any]:
        compact_scenarios = []
        for item in payload["scenarios"]:
            row = {"scenario": item.get("scenario"), "error": item.get("error")}
            if "write" in item:
                row["write"] = item["write"]
            if "stabilization" in item:
                stab = item["stabilization"]
                row["stabilization"] = {
                    "verdict": stab.get("verdict"),
                    "nudged": stab.get("nudged"),
                    "observedFulfilledSum": stab.get("observedFulfilledSum"),
                    "oracleFulfilledSum": stab.get("oracleFulfilledSum"),
                    "mismatchCount": len(stab.get("mismatches") or []),
                    "overfillCount": len(stab.get("overfill") or []),
                }
            for key in (
                "waves",
                "waveSize",
                "staleAfterCreate",
                "staleAfterCancel",
                "writePathMeanGrowthMs",
                "read",
                "visibleOverfillSnapshots",
                "pendingCreated",
                "fulfilledAfterPendingOnly",
                "leaked",
                "centerA",
                "centerB",
                "wavesDetail",
            ):
                if key in item:
                    row[key] = item[key]
            compact_scenarios.append({k: v for k, v in row.items() if v is not None})
        return {
            "runId": payload["runId"],
            "failed": payload["failed"],
            "elapsedSeconds": payload["elapsedSeconds"],
            "findings": payload["findings"],
            "scenarios": compact_scenarios,
        }


EVALUATION_NOTES = {
    "concurrency": [
        "Taxa de OptimisticLockingFailureException no refresh de fulfilledBloodBags.",
        "Se a doação persiste e o refresh falha (lost_refresh): write path não é atômico.",
        "Retries do cliente mascaram ou revelam a ausência de retry no servidor.",
    ],
    "stabilization": [
        "Após a rajada, o snapshot GET deve convergir e bater com o oráculo FIFO.",
        "Nudge necessário = contador materializado ficou stale até o próximo write.",
        "Overfill (fulfilled > goal) quebra a invariante do domínio.",
        "goalReached inconsistente com fulfilled/goal é bug de DTO/materialização.",
    ],
    "recurring": [
        "Ondas sucessivas no mesmo hemocentro: o pool cresce e o FIFO redistribui.",
        "Latência média por onda: custo O(doações históricas × requests) do refresher.",
        "Nova request nasce em 0 mesmo com sobra no pool (stale-on-create).",
        "Cancelar request não redistribui até a próxima doação completa (stale-on-cancel).",
        "dateLimit diferente por request: janela temporal do acceptsDonation.",
    ],
    "mixed-reads": [
        "Leituras (recomendações / lista) durante escritas não devem 5xx.",
        "Read path não recalcula: pode devolver contador mid-refresh (atraso aceito?).",
        "Overfill visível em snapshot de leitura indica persistência parcial do loop de save.",
    ],
    "pending": [
        "create-pending não dispara refresh; complete dispara — contrato da estratégia.",
        "Complete concorrente pressiona o mesmo @Version das requests.",
    ],
    "isolation": [
        "Pool é por hemocentro: doações em A não podem preencher requests de B.",
    ],
    "architectureRisksTheScriptCannotTimeTravel": [
        "dateRequested é sempre LocalDate.now() — não dá para criar request 'ontem' via API.",
        "Expiração real (dateLimit < hoje) não é simulável sem esperar o calendário.",
        "Intervalo de 3 meses entre doações do mesmo doador: ondas usam doadores distintos.",
    ],
}


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="Simula uso concorrente e recorrente da API BloodMatch para stressar o fulfillment.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    p.add_argument("--api-url", default=os.environ.get("API_URL", "http://localhost:8080"))
    p.add_argument(
        "--scenario",
        default="all",
        choices=["all", "concurrency", "recurring", "mixed-reads", "pending", "isolation"],
    )
    p.add_argument("--today", help="Data lógica ISO (YYYY-MM-DD). Default: hoje local.")
    p.add_argument("--donation-date", help="Data das doações. Default: --today. Não pode ser futura.")
    p.add_argument(
        "--date-limits",
        default="7,14,21,30",
        help="Offsets em dias para dateLimit das requests, separados por vírgula.",
    )
    p.add_argument("--concurrency", type=int, default=12, help="Doadores na rajada concorrente.")
    p.add_argument("--requests", type=int, default=6, help="Requests ativas no hemocentro principal.")
    p.add_argument("--waves", type=int, default=3, help="Ondas do cenário recurring.")
    p.add_argument("--wave-size", type=int, default=5, help="Doações por onda.")
    p.add_argument("--workers", type=int, default=12, help="Threads da rajada.")
    p.add_argument("--retries", type=int, default=6, help="Retries em conflito otimista.")
    p.add_argument("--backoff", type=float, default=0.05, help="Backoff inicial (s) dos retries.")
    p.add_argument("--stabilize-polls", type=int, default=6)
    p.add_argument("--stabilize-wait", type=float, default=0.25)
    p.add_argument("--timeout", type=float, default=30.0)
    p.add_argument("--password", default=os.environ.get("SEED_PASSWORD", DEFAULT_PASSWORD))
    p.add_argument("--run-id", help="Prefixo estável para emails/CPF desta execução.")
    p.add_argument("--report", help="Caminho JSON do relatório.")
    p.add_argument("--quiet-json", action="store_true", help="Imprime só o JSON final.")
    p.add_argument("--self-test", action="store_true", help="Valida o oráculo FIFO sem chamar a API.")
    return p


def main() -> int:
    args = build_parser().parse_args()
    if args.self_test:
        self_test_oracle()
        return 0
    if args.requests < 1 or args.concurrency < 1 or args.waves < 1 or args.wave_size < 1:
        print("parâmetros numéricos devem ser >= 1", file=sys.stderr)
        return 2
    sim = Simulator(args)
    if args.quiet_json:
        import contextlib
        import io
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            payload = sim.run()
        print(json.dumps(payload, indent=2, ensure_ascii=False))
    else:
        payload = sim.run()
        print("\n--- relatório resumido ---")
        print(json.dumps(sim.compact(payload), indent=2, ensure_ascii=False, default=str))
    if args.report:
        with open(args.report, "w", encoding="utf-8") as fh:
            json.dump(payload, fh, indent=2, ensure_ascii=False)
        print(f"relatório gravado em {args.report}")
    return 1 if payload["failed"] else 0


if __name__ == "__main__":
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8")
        except (AttributeError, OSError):
            pass
    sys.exit(main())
