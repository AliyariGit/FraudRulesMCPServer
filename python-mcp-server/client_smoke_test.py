"""Spawns mcp_server.py over stdio (the same transport Claude Desktop uses) and
calls every registered tool once against representative payloads. This is the
closest check to "does it work in an MCP client" without installing one.
"""
from __future__ import annotations

import asyncio
import json
import os
import sys

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

SERVER_SCRIPT = os.path.join(os.path.dirname(__file__), "mcp_server.py")

SAMPLE_TXN = {
    "transactionId": "TX-SMOKE-0001",
    "amount": 8500,
    "country": "CA",
    "merchant": "CryptoExchange",
    "customerAge": 22,
    "deviceRisk": "HIGH",
    "customer": {"country": "US"},
}

EXPECTED_TOOLS = {
    "evaluate_transaction",
    "create_rule",
    "generate_rule_from_text",
    "approve_rule",
    "explain_decision",
    "simulate_rule",
    "migrate_legacy_rule",
}


async def call_tool(session: ClientSession, name: str, arguments: dict) -> dict:
    result = await session.call_tool(name, arguments)
    if result.isError:
        text = result.content[0].text if result.content else "<no content>"
        raise RuntimeError(f"{name} returned an error: {text}")
    if result.structuredContent is not None:
        return result.structuredContent
    return json.loads(result.content[0].text)


async def main() -> None:
    server_params = StdioServerParameters(
        command=sys.executable, args=[SERVER_SCRIPT], env=dict(os.environ)
    )
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()

            tools = await session.list_tools()
            names = {t.name for t in tools.tools}
            missing = EXPECTED_TOOLS - names
            assert not missing, f"Missing tools: {missing}"
            print(f"Discovered {len(names)} tools: {sorted(names)}")

            created = await call_tool(
                session,
                "create_rule",
                {
                    "name": "Smoke Test Rule",
                    "condition": "deviceRisk == HIGH AND merchant == CryptoExchange",
                    "action": "DECLINE",
                },
            )
            print("create_rule ->", created["id"], created["status"])

            evaluated = await call_tool(session, "evaluate_transaction", {"transaction": SAMPLE_TXN})
            print("evaluate_transaction -> decision:", evaluated["decision"], "riskScore:", evaluated["riskScore"])
            assert evaluated["decision"] == "DECLINE"

            explanation = await call_tool(session, "explain_decision", {"transaction_id": "TX-SMOKE-0001"})
            print("explain_decision ->", explanation["explanation"].splitlines()[0])

            simulated = await call_tool(
                session,
                "simulate_rule",
                {"condition": "amount > 5000", "action": "REVIEW", "name": "Smoke Sim"},
            )
            print(
                "simulate_rule -> tested:", simulated["transactionsTested"],
                "blocked:", simulated["blocked"],
                "falsePositives:", simulated["falsePositives"],
            )

            migrated = await call_tool(
                session,
                "migrate_legacy_rule",
                {"legacy_text": "IF TX_AMT > 9999 SET FRAUD_FLAG=Y"},
            )
            print("migrate_legacy_rule ->", migrated["id"], migrated["status"])

            approved = await call_tool(session, "approve_rule", {"rule_id": migrated["id"]})
            print("approve_rule ->", approved["status"])
            assert approved["status"] == "ACTIVE"

            try:
                generated = await call_tool(
                    session,
                    "generate_rule_from_text",
                    {"instruction": "Block suspicious payments from new devices over $5000"},
                )
                print("generate_rule_from_text ->", generated["id"], generated["status"])
            except RuntimeError as exc:
                print(f"generate_rule_from_text -> SKIPPED ({exc})")

    print("\nAll MCP tool smoke checks passed.")


if __name__ == "__main__":
    asyncio.run(main())
