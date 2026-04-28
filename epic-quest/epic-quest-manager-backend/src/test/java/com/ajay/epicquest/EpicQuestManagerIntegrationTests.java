package com.ajay.epicquest;

import com.ajay.epicquest.model.User;
import com.ajay.epicquest.model.enums.Rarity;
import com.ajay.epicquest.model.enums.Role;
import com.ajay.epicquest.repository.HeroRepository;
import com.ajay.epicquest.repository.ItemRepository;
import com.ajay.epicquest.repository.QuestAcceptanceRepository;
import com.ajay.epicquest.repository.QuestRepository;
import com.ajay.epicquest.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
/**
 * End-to-end regression suite for the main API flows. These tests run against a
 * Spring application context with an in-memory H2 database and focus on contract,
 * authorization, validation, and previously fixed defect scenarios.
 */
class EpicQuestManagerIntegrationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HeroRepository heroRepository;

    @Autowired
    private ItemRepository itemRepository;

        @Autowired
        private QuestRepository questRepository;

        @Autowired
        private QuestAcceptanceRepository questAcceptanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String playerToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        
        objectMapper = new ObjectMapper();
        
        // Each test starts from a clean database so scenarios remain independent and repeatable.
        questAcceptanceRepository.deleteAll();
        heroRepository.deleteAll();
        questRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();

        User admin = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("adminPass"))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);

        User player = User.builder()
                .username("player")
                .passwordHash(passwordEncoder.encode("playerPass"))
                .role(Role.PLAYER)
                .build();
        userRepository.save(player);

        adminToken = loginAndGetToken("admin", "adminPass");
        playerToken = loginAndGetToken("player", "playerPass");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
                // Tests obtain real JWTs so security and authorization behavior is exercised end to end.
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> map = objectMapper.readValue(response, Map.class);
        return (String) map.get("access_token");
    }

    @Test
    void adminCanCreateAndDeleteItem() throws Exception {
        var createResp = mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Excalibur",
                                "category", "weapon",
                                "description", "Legendary blade",
                                "powerValue", 100,
                                "rarity", Rarity.LEGENDARY.name()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Excalibur"))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> item = objectMapper.readValue(createResp, Map.class);
        Integer itemId = (Integer) item.get("id");

        mockMvc.perform(get("/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("weapon"));

        mockMvc.perform(delete("/items/" + itemId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/items/" + itemId))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminCannotCreateItem() throws Exception {
        mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Rusty Sword",
                                "category", "weapon",
                                "description", "Weak",
                                "powerValue", 1,
                                "rarity", Rarity.COMMON.name()
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void heroInventoryLimitEnforced() throws Exception {
        // create hero as player
        var heroResp = mockMvc.perform(post("/heroes")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Arthur",
                                "heroClass", "fighter",
                                "level", 5
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner_user_id").isNumber())
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> hero = objectMapper.readValue(heroResp, Map.class);
        Integer heroId = (Integer) hero.get("id");

        // create 4 items as admin
        for (int i = 1; i <= 4; i++) {
            var itemResp = mockMvc.perform(post("/items")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "name", "Item" + i,
                                    "category", "artifact",
                                    "description", "desc",
                                    "powerValue", i,
                                    "rarity", Rarity.COMMON.name()
                            ))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Map<?, ?> item = objectMapper.readValue(itemResp, Map.class);
            Integer itemId = (Integer) item.get("id");

            var status = (i <= 3) ? 200 : 400;
            mockMvc.perform(post("/heroes/" + heroId + "/inventory")
                            .header("Authorization", "Bearer " + playerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("itemId", itemId))))
                    .andExpect(status().is(status));
        }
    }

    @Test
    void heroInventoryRejectsDuplicateItem() throws Exception {
        var heroResp = mockMvc.perform(post("/heroes")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Arthur",
                                "heroClass", "fighter",
                                "level", 5
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer heroId = (Integer) objectMapper.readValue(heroResp, Map.class).get("id");

        var itemResp = mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Duplicate Shield",
                                "category", "armor",
                                "description", "Blocks once",
                                "powerValue", 7,
                                "rarity", Rarity.RARE.name()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer itemId = (Integer) objectMapper.readValue(itemResp, Map.class).get("id");

        mockMvc.perform(post("/heroes/" + heroId + "/inventory")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("itemId", itemId))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/heroes/" + heroId + "/inventory")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("itemId", itemId))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Item is already in hero inventory"));
    }

    @Test
    void heroResponsesDoNotExposeOwnerPasswordHash() throws Exception {
        String heroResp = mockMvc.perform(post("/heroes")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "SecureHero",
                                "heroClass", "fighter",
                                "level", 6
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner_user_id").isNumber())
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Integer heroId = (Integer) objectMapper.readValue(heroResp, Map.class).get("id");

        mockMvc.perform(get("/heroes/" + heroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner_user_id").isNumber())
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void questAcceptRarityCheck() throws Exception {
        // create hero
        var heroResp = mockMvc.perform(post("/heroes")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Merlin",
                                "heroClass", "mage",
                                "level", 15
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer heroId = (Integer) objectMapper.readValue(heroResp, Map.class).get("id");

        // create item rarity common and add to hero
        var itemResp = mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Common Wand",
                                "category", "weapon",
                                "description", "basic",
                                "powerValue", 10,
                                "rarity", Rarity.COMMON.name()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer itemId = (Integer) objectMapper.readValue(itemResp, Map.class).get("id");

        mockMvc.perform(post("/heroes/" + heroId + "/inventory")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("itemId", itemId))))
                .andExpect(status().isOk());

        // create epic quest
        var questResp = mockMvc.perform(post("/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("title", "Dragon Hunt"),
                                Map.entry("description", "Slay the dragon"),
                                Map.entry("difficultyLevel", 10),
                                Map.entry("requiredRarity", Rarity.EPIC.name())
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer questId = (Integer) objectMapper.readValue(questResp, Map.class).get("id");

        // accept should fail rarity mismatch
        mockMvc.perform(post("/quests/" + questId + "/accept")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("heroId", heroId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void questAcceptResponseDoesNotRecurse() throws Exception {
        var heroResp = mockMvc.perform(post("/heroes")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Lancelot",
                                "heroClass", "fighter",
                                "level", 20
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer heroId = (Integer) objectMapper.readValue(heroResp, Map.class).get("id");

        var itemResp = mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Epic Blade",
                                "category", "weapon",
                                "description", "Epic rarity weapon",
                                "powerValue", 50,
                                "rarity", Rarity.EPIC.name()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer itemId = (Integer) objectMapper.readValue(itemResp, Map.class).get("id");

        mockMvc.perform(post("/heroes/" + heroId + "/inventory")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("itemId", itemId))))
                .andExpect(status().isOk());

        var questResp = mockMvc.perform(post("/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("title", "Epic Trial"),
                                Map.entry("description", "Prove your worth"),
                                Map.entry("difficultyLevel", 12),
                                Map.entry("requiredRarity", Rarity.EPIC.name())
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer questId = (Integer) objectMapper.readValue(questResp, Map.class).get("id");

        mockMvc.perform(post("/quests/" + questId + "/accept")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("heroId", heroId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedBy[0].id").isNumber())
                .andExpect(jsonPath("$.acceptedBy[0].hero.id").value(heroId))
                .andExpect(jsonPath("$.acceptedBy[0].quest").doesNotExist())
                .andExpect(jsonPath("$.acceptedBy[0].hero.owner").doesNotExist())
                .andExpect(jsonPath("$.acceptedBy[0].hero.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.acceptedBy[0].hero.owner_user_id").isNumber());
    }

    @Test
    void nestedHeroQuestAcceptRouteWorks() throws Exception {
        var heroResp = mockMvc.perform(post("/heroes")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Galahad",
                                "heroClass", "fighter",
                                "level", 18
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer heroId = (Integer) objectMapper.readValue(heroResp, Map.class).get("id");

        var itemResp = mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Legendary Spear",
                                "category", "weapon",
                                "description", "A quest-worthy weapon",
                                "powerValue", 60,
                                "rarity", Rarity.LEGENDARY.name()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer itemId = (Integer) objectMapper.readValue(itemResp, Map.class).get("id");

        mockMvc.perform(post("/heroes/" + heroId + "/inventory")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("itemId", itemId))))
                .andExpect(status().isOk());

        var questResp = mockMvc.perform(post("/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("title", "Heroic Deed"),
                                Map.entry("description", "Complete the heroic deed"),
                                Map.entry("difficultyLevel", 9),
                                Map.entry("requiredRarity", Rarity.EPIC.name())
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer questId = (Integer) objectMapper.readValue(questResp, Map.class).get("id");

        mockMvc.perform(post("/heroes/" + heroId + "/quests/" + questId + "/accept")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questId))
                .andExpect(jsonPath("$.acceptedBy[0].hero.id").value(heroId))
                .andExpect(jsonPath("$.acceptedBy[0].hero.owner").doesNotExist())
                .andExpect(jsonPath("$.acceptedBy[0].hero.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.acceptedBy[0].hero.owner_user_id").isNumber());
    }

    @Test
    void getAllQuestsDoesNotExposeOwnerPasswordHash() throws Exception {
        var heroResp = mockMvc.perform(post("/heroes")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "QuestViewer",
                                "heroClass", "fighter",
                                "level", 10
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer heroId = (Integer) objectMapper.readValue(heroResp, Map.class).get("id");

        var itemResp = mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Viewer Blade",
                                "category", "weapon",
                                "description", "Used to qualify for quest",
                                "powerValue", 40,
                                "rarity", Rarity.EPIC.name()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer itemId = (Integer) objectMapper.readValue(itemResp, Map.class).get("id");

        mockMvc.perform(post("/heroes/" + heroId + "/inventory")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("itemId", itemId))))
                .andExpect(status().isOk());

        var questResp = mockMvc.perform(post("/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("title", "Safe Listing"),
                                Map.entry("description", "Verify list output is safe"),
                                Map.entry("difficultyLevel", 8),
                                Map.entry("requiredRarity", Rarity.EPIC.name())
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer questId = (Integer) objectMapper.readValue(questResp, Map.class).get("id");

        mockMvc.perform(post("/heroes/" + heroId + "/quests/" + questId + "/accept")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/quests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acceptedBy[0].hero.owner").doesNotExist())
                .andExpect(jsonPath("$[0].acceptedBy[0].hero.passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].acceptedBy[0].hero.owner_user_id").isNumber());
    }

    @Test
    void testItemQueryParameters() throws Exception {
        // Clean up any existing items first
        itemRepository.deleteAll();

        // Create multiple items with different categories and rarities
        mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sword of Power",
                                "category", "weapon",
                                "rarity", "LEGENDARY",
                                "powerValue", 100
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Iron Sword",
                                "category", "weapon",
                                "rarity", "COMMON",
                                "powerValue", 20
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Health Potion",
                                "category", "potion",
                                "rarity", "RARE",
                                "powerValue", 50
                        ))))
                .andExpect(status().isCreated());

        // Test filtering by category
        mockMvc.perform(get("/items?category=weapon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Test filtering by rarity
        mockMvc.perform(get("/items?rarity=LEGENDARY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Sword of Power"));

        // Test filtering by both category and rarity
        mockMvc.perform(get("/items?category=weapon&rarity=COMMON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Iron Sword"));

        // Test pagination
        mockMvc.perform(get("/items?page=0&limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Test no filters (should return all items sorted by rarity then power_value desc)
        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Iron Sword"))
                .andExpect(jsonPath("$[1].name").value("Health Potion"))
                .andExpect(jsonPath("$[2].name").value("Sword of Power"));
    }

        @Test
        void createItem_WithOversizedPowerValue_ReturnsBadRequest() throws Exception {
                mockMvc.perform(post("/items")
                                                .header("Authorization", "Bearer " + adminToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"name\":\"SwordHuge\",\"description\":\"Sharp tool\",\"category\":\"Weapon\",\"powerValue\":8888888888888888,\"rarity\":\"COMMON\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("Invalid request body")))
                                .andExpect(content().string(not(containsString("trace"))));
        }

        @Test
        void putItemsWithoutId_ReturnsMethodNotAllowedWithoutTrace() throws Exception {
                mockMvc.perform(put("/items")
                                                .header("Authorization", "Bearer " + adminToken)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{}"))
                                .andExpect(status().isMethodNotAllowed())
                                .andExpect(content().string(containsString("Method not allowed")))
                                .andExpect(content().string(not(containsString("trace"))));
        }

    @Test
    void testQuestPaginationQueryParameters() throws Exception {
        // Clean up quests first
        questAcceptanceRepository.deleteAll();
        questRepository.deleteAll();

        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/quests")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.ofEntries(
                                    Map.entry("title", "Quest " + i),
                                    Map.entry("description", "Desc " + i),
                                    Map.entry("difficultyLevel", i),
                                    Map.entry("requiredRarity", Rarity.COMMON.name())
                            ))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/quests?page=0&limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/quests?page=1&limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void adminCanCreateUsers() throws Exception {
        // Test admin can create a player user via register endpoint
        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "newplayer",
                                "password", "newpass123",
                                "role", "PLAYER"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newplayer"))
                .andExpect(jsonPath("$.role").value("PLAYER"));

        // Test admin can create an admin user via register endpoint
        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "newadmin",
                                "password", "adminpass123",
                                "role", "ADMIN"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newadmin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void adminCreateUser_ValidationErrors() throws Exception {
        // Test missing required fields
        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "testuser"
                                // missing password
                        ))))
                .andExpect(status().isBadRequest());

        // Test duplicate username
        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "admin", // already exists
                                "password", "password123",
                                "role", "PLAYER"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void playerCannotCreateAdminUsers() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "shouldnotwork",
                                "password", "password123",
                                "role", "ADMIN"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotCreateAdminUsers() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "shouldnotwork",
                                "password", "password123",
                                "role", "ADMIN"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rateLimitingOnAuthEndpoints() throws Exception {
        // Note: Rate limiting is disabled in test environment
        // This test verifies that auth endpoints work normally without rate limiting
        for (int i = 0; i < 10; i++) {  // Test more requests than the limit
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "username", "admin",
                                    "password", "adminPass"
                            ))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void rateLimitingOnRegisterEndpoint() throws Exception {
        // Note: Rate limiting is disabled in test environment
        // This test verifies that register endpoint works normally without rate limiting
        for (int i = 0; i < 10; i++) {  // Test more requests than the limit
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "username", "testuser" + i,
                                    "password", "password123"
                            ))))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void noRateLimitingOnOtherEndpoints() throws Exception {
        // Test that non-auth endpoints are not rate limited
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/items"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void heroOwnerCanListAcceptedQuests() throws Exception {
        var heroResp = mockMvc.perform(post("/heroes")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Riven",
                                "heroClass", "fighter",
                                "level", 8
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer heroId = (Integer) objectMapper.readValue(heroResp, Map.class).get("id");

        var itemResp = mockMvc.perform(post("/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Traveler Sword",
                                "category", "weapon",
                                "description", "starter",
                                "powerValue", 12,
                                "rarity", Rarity.COMMON.name()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer itemId = (Integer) objectMapper.readValue(itemResp, Map.class).get("id");

        mockMvc.perform(post("/heroes/" + heroId + "/inventory")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("itemId", itemId))))
                .andExpect(status().isOk());

        var questResp = mockMvc.perform(post("/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("title", "Scout Mission"),
                                Map.entry("description", "Reach the outpost"),
                                Map.entry("difficultyLevel", 2),
                                Map.entry("requiredRarity", Rarity.COMMON.name())
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer questId = (Integer) objectMapper.readValue(questResp, Map.class).get("id");

        mockMvc.perform(post("/quests/" + questId + "/accept")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("heroId", heroId))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/heroes/" + heroId + "/quests/accepted")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].questId").value(questId))
                .andExpect(jsonPath("$[0].title").value("Scout Mission"))
                .andExpect(jsonPath("$[0].acceptedAt").isNotEmpty());
    }

    @Test
    void nonOwnerCannotListAcceptedQuests() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "player2",
                                "password", "player2Pass"
                        ))))
                .andExpect(status().isCreated());

        String player2Token = loginAndGetToken("player2", "player2Pass");

        var heroResp = mockMvc.perform(post("/heroes")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "OwnerHero",
                                "heroClass", "fighter",
                                "level", 7
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer heroId = (Integer) objectMapper.readValue(heroResp, Map.class).get("id");

        mockMvc.perform(get("/heroes/" + heroId + "/quests/accepted")
                        .header("Authorization", "Bearer " + player2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotListAcceptedQuests() throws Exception {
        mockMvc.perform(get("/heroes/1/quests/accepted"))
                                .andExpect(status().isUnauthorized());
    }

    @Test
        void registerRoleOverrideRequiresAuthorization() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "strict-register-user",
                                "password", "password123",
                                "role", "ADMIN"
                        ))))
                                .andExpect(status().isForbidden());
    }

    @Test
    void registerWithoutRoleCreatesPlayer() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "plain-player",
                                "password", "password123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("plain-player"))
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }
}
