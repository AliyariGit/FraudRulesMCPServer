from __future__ import annotations

import anthropic

from database.repo import save_rule
from models.schemas import DEFAULT_SEVERITY_WEIGHT, Rule, RuleDraft

_SYSTEM_PROMPT = """You translate a natural-language fraud-prevention instruction into a \
structured fraud rule condition tree.

Allowed fields: amount (number), country (string, ISO-2), merchant (string), \
customerAge (number), deviceRisk (string: LOW/MEDIUM/HIGH), transactionId (string), \
customer.country (string, the customer's home country -- use {"ref": "customer.country"} \
as the value when comparing a field against it instead of a literal).

Allowed operators: > < >= <= == !=.

Combine conditions with all_of (AND) or any_of (OR); nest combinators if the \
instruction requires it. A ConditionNode is either a combinator (all_of/any_of, a list \
of child ConditionNodes) or a leaf (field, operator, value) -- never both.

Choose `action` as one of DECLINE, REVIEW, ALERT, HIGH_RISK based on how severe the \
instruction implies the rule should be: "block"/"reject" => DECLINE, "flag for review" \
=> REVIEW, "alert"/"notify" => ALERT, otherwise HIGH_RISK.

Give a one-sentence `reasoning` explaining how the instruction maps to the conditions \
and action you chose.
"""


def generate_rule_from_text(instruction: str, model: str = "claude-sonnet-5") -> dict:
    """Calls the Anthropic API to draft a structured rule from a natural-language
    instruction. The rule is stored as PENDING_APPROVAL (source=LLM_GENERATED) --
    it must go through approve_rule before it can affect real evaluations.
    """
    client = anthropic.Anthropic()
    parsed = client.messages.parse(
        model=model,
        max_tokens=1024,
        system=_SYSTEM_PROMPT,
        messages=[{"role": "user", "content": instruction}],
        output_format=RuleDraft,
    )
    draft = parsed.parsed_output
    if draft is None:
        raise RuntimeError("The model did not return a parsable rule draft.")

    rule = Rule(
        name=draft.name,
        condition_dsl=None,
        condition_json=draft.condition_json,
        action=draft.action,
        severity_weight=DEFAULT_SEVERITY_WEIGHT[draft.action],
        status="PENDING_APPROVAL",
        source="LLM_GENERATED",
    )
    saved = save_rule(rule)

    result = saved.model_dump(mode="json")
    result["llm_reasoning"] = draft.reasoning
    result["instruction"] = instruction
    return result
