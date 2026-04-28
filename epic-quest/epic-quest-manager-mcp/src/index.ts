import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

import { BackendApiError, EpicQuestBackendClient } from "./backend-client.js";
import { loadBackendConfig } from "./config.js";

const backendConfig = loadBackendConfig();
const backendClient = new EpicQuestBackendClient(backendConfig);

const server = new McpServer({
  name: "epic-quest-manager-mcp",
  version: "0.1.0",
});

server.tool(
  "accept_quest_for_hero",
  "Have a hero accept a quest through the existing Epic Quest Manager backend. Use this when you already know the hero and quest IDs and want the protected gameplay flow executed via MCP.",
  {
    heroId: z.number().int().positive().describe("Hero identifier"),
    questId: z.number().int().positive().describe("Quest identifier"),
  },
  async ({ heroId, questId }) => {
    try {
      const quest = await backendClient.acceptQuestForHero(heroId, questId);

      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(
              {
                success: true,
                heroId,
                questId: quest.id,
                title: quest.title,
                difficultyLevel: quest.difficultyLevel,
                requiredRarity: quest.requiredRarity,
                description: quest.description ?? null,
              },
              null,
              2,
            ),
          },
        ],
      };
    } catch (error) {
      const message =
        error instanceof BackendApiError
          ? `Backend error ${error.status}: ${error.message}`
          : error instanceof Error
            ? error.message
            : "Unexpected MCP server error";

      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(
              {
                success: false,
                heroId,
                questId,
                error: message,
              },
              null,
              2,
            ),
          },
        ],
        isError: true,
      };
    }
  },
);

async function main(): Promise<void> {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((error) => {
  console.error(
    error instanceof Error ? error.message : "Failed to start Epic Quest Manager MCP server",
  );
  process.exit(1);
});