"""One-off generator for data/synthetic_transactions.json used by simulate_rule.
Produces transactions with a fraud-correlated structure (not uniformly random)
so simulation metrics (blocked/false-positive/fraud-prevented) are meaningful.
Re-run only if the fixture needs regenerating; the JSON output is committed.
"""
from __future__ import annotations

import json
import pathlib
import random

random.seed(42)

COUNTRIES = ["CA", "US", "GB", "FR", "DE", "BR", "NG", "IN"]
MERCHANTS = ["GroceryStore", "Electronics", "CryptoExchange", "Airline", "OnlineRetail", "Restaurant"]
DEVICE_RISKS = ["LOW", "MEDIUM", "HIGH"]

N = 300


def make_transaction(i: int) -> dict:
    is_fraud = random.random() < 0.15

    if is_fraud:
        amount = round(random.uniform(4000, 25000), 2)
        merchant = random.choices(MERCHANTS, weights=[1, 1, 6, 1, 2, 1])[0]
        device_risk = random.choices(DEVICE_RISKS, weights=[1, 2, 7])[0]
        customer_age = random.randint(18, 60)
        country = random.choice(COUNTRIES)
        home_country = random.choice([c for c in COUNTRIES if c != country]) if random.random() < 0.6 else country
    else:
        amount = round(random.uniform(5, 3000), 2)
        merchant = random.choices(MERCHANTS, weights=[5, 4, 1, 3, 5, 4])[0]
        device_risk = random.choices(DEVICE_RISKS, weights=[6, 3, 1])[0]
        customer_age = random.randint(18, 75)
        home_country = random.choice(COUNTRIES)
        country = home_country if random.random() < 0.9 else random.choice(COUNTRIES)

    return {
        "transactionId": f"TX{100000 + i}",
        "amount": amount,
        "country": country,
        "merchant": merchant,
        "customerAge": customer_age,
        "deviceRisk": device_risk,
        "customer": {"country": home_country},
        "is_fraud": is_fraud,
    }


def main() -> None:
    transactions = [make_transaction(i) for i in range(N)]
    out_path = pathlib.Path(__file__).resolve().parent / "synthetic_transactions.json"
    out_path.write_text(json.dumps(transactions, indent=2))
    fraud_count = sum(t["is_fraud"] for t in transactions)
    print(f"Wrote {len(transactions)} transactions ({fraud_count} fraudulent) to {out_path}")


if __name__ == "__main__":
    main()
