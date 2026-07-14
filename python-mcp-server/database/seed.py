from __future__ import annotations

import json
import pathlib

from database.session import get_session, init_db
from models.db_models import RuleORM
from models.schemas import DEFAULT_SEVERITY_WEIGHT
from rules.condition_dsl import parse_condition

SEED_FILE = pathlib.Path(__file__).resolve().parent.parent / "data" / "seed_rules.json"


def seed() -> None:
    init_db()
    session = get_session()
    try:
        if session.query(RuleORM).count() > 0:
            print("Rules already seeded, skipping.")
            return
        rules = json.loads(SEED_FILE.read_text())
        for r in rules:
            condition_json = parse_condition(r["condition_dsl"]).model_dump(exclude_none=True)
            session.add(
                RuleORM(
                    name=r["name"],
                    condition_dsl=r["condition_dsl"],
                    condition_json=condition_json,
                    action=r["action"],
                    severity_weight=r.get("severity_weight", DEFAULT_SEVERITY_WEIGHT[r["action"]]),
                    priority=r.get("priority", 100),
                    status="ACTIVE",
                    source="MANUAL",
                )
            )
        session.commit()
        print(f"Seeded {len(rules)} rules.")
    finally:
        session.close()


if __name__ == "__main__":
    seed()
