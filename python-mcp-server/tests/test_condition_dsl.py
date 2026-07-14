import pytest

from models.schemas import FieldRef
from rules.condition_dsl import ConditionParseError, parse_condition


def test_single_leaf():
    node = parse_condition("amount > 10000")
    assert node.field == "amount"
    assert node.operator == ">"
    assert node.value == 10000


def test_and_chain():
    node = parse_condition("amount > 10000 AND country != CA")
    assert node.all_of is not None
    assert len(node.all_of) == 2
    assert node.all_of[0].field == "amount"
    assert node.all_of[1].field == "country"
    assert node.all_of[1].value == "CA"


def test_or_chain():
    node = parse_condition("deviceRisk == HIGH OR customerAge < 18")
    assert node.any_of is not None
    assert len(node.any_of) == 2


def test_and_or_precedence():
    node = parse_condition("amount > 5000 AND country != CA OR deviceRisk == HIGH")
    assert node.any_of is not None
    assert len(node.any_of) == 2
    first_group = node.any_of[0]
    assert first_group.all_of is not None
    assert len(first_group.all_of) == 2


def test_field_reference_value():
    node = parse_condition("country != customer.country")
    assert node.value == FieldRef(ref="customer.country")


def test_unknown_field_rejected():
    with pytest.raises(ConditionParseError):
        parse_condition("totallyUnknownField > 5")


def test_quoted_string_value():
    node = parse_condition('merchant == "CryptoExchange"')
    assert node.value == "CryptoExchange"


def test_alias_operators():
    node = parse_condition("country <> CA")
    assert node.operator == "!="
    node2 = parse_condition("amount = 100")
    assert node2.operator == "=="
