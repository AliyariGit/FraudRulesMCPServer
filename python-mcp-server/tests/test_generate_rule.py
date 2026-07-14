"""Tests the generate_rule_from_text plumbing (storage, status, source,
reasoning passthrough) with the Anthropic client mocked, so this doesn't
require live API credits to verify.
"""
from unittest.mock import MagicMock, patch

from models.schemas import ConditionNode, RuleDraft
from tools.generate_rule import generate_rule_from_text


def test_generate_rule_from_text_stores_pending_with_llm_output():
    draft = RuleDraft(
        name="New Device High Value",
        condition_json=ConditionNode(
            all_of=[
                ConditionNode(field="deviceRisk", operator="==", value="HIGH"),
                ConditionNode(field="amount", operator=">", value=5000),
            ]
        ),
        action="REVIEW",
        reasoning="Instruction asked to flag high-value payments from risky new devices.",
    )
    mock_response = MagicMock()
    mock_response.parsed_output = draft

    with patch("tools.generate_rule.anthropic.Anthropic") as MockClient:
        MockClient.return_value.messages.parse.return_value = mock_response
        result = generate_rule_from_text(
            "Block suspicious payments from new devices over $5000"
        )

    assert result["status"] == "PENDING_APPROVAL"
    assert result["source"] == "LLM_GENERATED"
    assert result["action"] == "REVIEW"
    assert result["name"] == "New Device High Value"
    assert result["llm_reasoning"] == draft.reasoning
    assert result["id"] is not None
