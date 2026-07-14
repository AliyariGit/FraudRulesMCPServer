CREATE TABLE rules (
    id              BIGSERIAL PRIMARY KEY,
    name            TEXT NOT NULL,
    condition_dsl   TEXT,
    condition_json  JSONB NOT NULL,
    action          TEXT NOT NULL CHECK (action IN ('DECLINE', 'REVIEW', 'ALERT', 'HIGH_RISK')),
    severity_weight INT NOT NULL,
    priority        INT NOT NULL DEFAULT 100,
    status          TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL', 'DISABLED')),
    source          TEXT NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'LLM_GENERATED', 'MIGRATED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE evaluations (
    id             BIGSERIAL PRIMARY KEY,
    transaction_id TEXT NOT NULL,
    payload        JSONB NOT NULL,
    decision       TEXT NOT NULL,
    risk_score     INT NOT NULL,
    matched_rules  JSONB NOT NULL,
    reason         TEXT NOT NULL,
    rule_trace     JSONB NOT NULL,
    evaluated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_evaluations_transaction_id ON evaluations (transaction_id);
