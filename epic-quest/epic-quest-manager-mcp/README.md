# Epic Quest Manager MCP Server

Separate MCP server that integrates with the Epic Quest backend without changing backend code.

## What It Shows

- MCP server setup using stdio transport
- backend JWT login integration
- one impactful protected tool: `accept_quest_for_hero`

## Tool

`accept_quest_for_hero`

- Input: `heroId`, `questId`
- Calls: `POST /heroes/{heroId}/quests/{questId}/accept`
- Returns: structured success/error output suitable for MCP clients

## Required Environment

- `BACKEND_BASE_URL` (example: `http://localhost:8080`)
- `BACKEND_USERNAME`
- `BACKEND_PASSWORD`
- `BACKEND_TIMEOUT_MS` (optional, default `10000`)

Use `.env.example` as a template.

## Quick Start

```powershell
npm install
npm run build
npm run dev
```

Backend must already be running and reachable at `BACKEND_BASE_URL`.

## Demo Suggestion

1. Run a successful quest acceptance call.
2. Run a controlled failure (duplicate acceptance or rarity mismatch).

This demonstrates both happy-path integration and business-rule error handling.
