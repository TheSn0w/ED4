package net.botwithus;

import net.botwithus.api.game.hud.Dialog;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Equipment;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.movement.Movement;
import net.botwithus.rs3.game.movement.NavPath;
import net.botwithus.rs3.game.movement.TraverseEvent;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.characters.player.Player;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.util.RandomGenerator;
import net.botwithus.rs3.util.Regex;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

import static net.botwithus.rs3.game.Client.getLocalPlayer;

public class ED4 extends LoopingScript {

    public boolean runScript;
    Player player = Client.getLocalPlayer();


    public int CoordX;
    public int CoordY;
    private BotState botState;
    boolean usePontifexRing = false;
    boolean useMaxGuild = false;
    boolean usePrayerPots;
    boolean useEssenceOfFinality;
    private boolean scriptRunning = false;
    long runStartTime;
    private Instant scriptStartTime;
    boolean useQuickPrayers;
    boolean killMages;
    boolean useWarsRetreat = false;
    boolean useDarkness;
    boolean useOverloads;
    boolean useInvokeDeath;
    boolean useBank;
    boolean vulnBombUsed = false;
    boolean smokeCloudUsed = false;
    boolean useSmokeCloud;
    boolean useFamiliar;
    boolean useWen;
    boolean useJas;
    boolean quickPrayersActive = false;
    boolean useShadowReefTeleport;
    private Random random = new Random();

    public int getCurrentDungTokens() {
        return VarManager.getVarValue(VarDomainType.PLAYER, 1097);
    }


    enum BotState {
        WALK_TO_ZAMMY_PORTAL, PRAYING, BANKING, INTERACT_WITH_ZAMMY, WALKTOZAMMY, MOVE_TO_CEBERUS, ATTACK_CEBERUS,  ACCEPT_DIALOG, FirstWalk, IDLE
    }

    public ED4(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new SkeletonScriptGraphicsContext(getConsole(), this);
        botState = BotState.IDLE;
        loadConfiguration();
        this.runStartTime = System.currentTimeMillis();
    }

    public void startScript() {
        println("Attempting to start script...");
        if (!scriptRunning) {
            scriptRunning = true;
            scriptStartTime = Instant.now();
            println("Script started at: " + scriptStartTime);
        } else {
            println("Attempted to start script, but it is already running.");
        }
    }

    public void stopScript() {
        if (scriptRunning) {
            scriptRunning = false;

            unsubscribeAll();

            Instant stopTime = Instant.now();
            println("Script stopped at: " + stopTime);
            long duration = Duration.between(scriptStartTime, stopTime).toMillis();
            println("Script ran for: " + duration + " milliseconds.");
        } else {
            println("Attempted to stop script, but it is not running.");
        }
    }

    @Override
    public void onLoop() {
        if (!scriptRunning) {
            return;
        }
        switch (botState) {
            case IDLE -> {
                if (useJas) {
                    deactivateScriptureOfJas();
                } else if (useWen) {
                    deactivateScriptureOfWen();
                }
                if (useWarsRetreat) {
                    useWarsTeleport();
                }
                if (useMaxGuild) {
                    useMaxGuildTeleport();
                }
                if (useShadowReefTeleport) {
                    useTablet();
                }
                if (!useWarsRetreat && !useMaxGuild && !useShadowReefTeleport) {
                    faladorBank();
                }

                vulnBombUsed = false;
                smokeCloudUsed = false;

            }

            case BANKING -> {
                if (useQuickPrayers) {
                    disableQuickPrayers();
                }
                if (useShadowReefTeleport) {
                    if (shouldBank(getLocalPlayer())) {
                        ringOfKinship();
                    } else {
                        useTreasureChest();
                    }
                }
                if (useMaxGuild) {
                    LoadMaxGuildPresetLogic();
                }
                if (useBank && useWarsRetreat) {
                    LoadPresetLogic();
                }
                if (!usePrayerPots && useWarsRetreat) {
                    botState = BotState.PRAYING;
                }
                else {
                    if (useWarsRetreat) {
                        ResultSet<Item> restore = InventoryItemQuery.newQuery(93).results();

                        Item prayerOrRestorePot = restore.stream()
                                .filter(item -> item.getName() != null &&
                                        (item.getName().toLowerCase().contains("prayer") ||
                                                item.getName().toLowerCase().contains("restore")))
                                .findFirst()
                                .orElse(null);

                        if (prayerOrRestorePot == null) {
                            println("No Prayer or Restore pots found, interacting with bank chest.");
                            LoadPresetLogic();
                        } else {
                            botState = BotState.WALK_TO_ZAMMY_PORTAL;
                        }
                    }
                }
                if (useFamiliar) {
                    ResultSet<Item> pouch = InventoryItemQuery.newQuery(93).results();

                    Item itemToSummon = pouch.stream()
                            .filter(item -> item.getName() != null &&
                                    (item.getName().toLowerCase().contains("pouch") ||
                                            item.getName().toLowerCase().contains("contract")) &&
                                    !item.getName().toLowerCase().contains("rune"))
                            .findFirst()
                            .orElse(null);

                    if (itemToSummon == null) {
                        ;
                        LoadPresetLogic();
                    }
                }
            }

            case PRAYING -> {
                if (useWarsRetreat) {
                    useAltarofWar();
                }
                if (useMaxGuild) {
                    Execution.delay(admireThroneofFame());
                }
            }
            case WALK_TO_ZAMMY_PORTAL -> {
                if (useMaxGuild) {
                    Execution.delay(walkToPortalMaxGuild());
                }
                if (useWarsRetreat) {
                    walkToPortal();
                }
                if (!useWarsRetreat && !useMaxGuild && !useShadowReefTeleport) {
                    walkTozammy();

                }
            }
            case WALKTOZAMMY -> {
                moveToSpecificLocation();
            }
            case INTERACT_WITH_ZAMMY -> {
                enterZammy();
            }
            case ACCEPT_DIALOG -> {
                acceptDialog();
            }
            case FirstWalk -> {
                FirstWalk();
            }
            case MOVE_TO_CEBERUS -> {
                Movetominiboss();
            }
            case ATTACK_CEBERUS -> {
                attackMiniBoss();
            }


        }
    }

    private void walkTozammy() {
        Coordinate zammyLocation = new Coordinate(1761, 1343, 0);

        if (Movement.traverse(NavPath.resolve(zammyLocation)) == TraverseEvent.State.FINISHED) {
            botState = BotState.INTERACT_WITH_ZAMMY;

        }
    }
    public void faladorBank() {
        Coordinate taverlyBank = new Coordinate(2946, 3368, 0);
        if (Movement.traverse(NavPath.resolve(taverlyBank)) == TraverseEvent.State.FINISHED) {
            interactWithBank();
        }
    }

    public void interactWithBank() {
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Bank booth").results();
        SceneObject bankChest = results.nearest();
        bankChest.interact("Load Last Preset from");
        Execution.delay(RandomGenerator.nextInt(1500, 2250));
        if (useFamiliar) {
            manageFamiliarSummoning();
        } else {
            botState = BotState.WALK_TO_ZAMMY_PORTAL;
        }
    }

    private void walkToPortal() {
        if (getLocalPlayer() == null) {
            return;
        }
        EntityResultSet<SceneObject> ZammyPortal = SceneObjectQuery.newQuery().name("Portal (The Zamorakian Undercity)").results();
        if (ZammyPortal.isEmpty()) {
            return;
        }
        SceneObject Portal = ZammyPortal.nearest();
        if (Portal != null) {
            boolean success = Portal.interact("Enter");
            println("Interacted with portal: " + success);

            boolean surgedAtCorrectLocation = Execution.delayUntil(15000, () -> {
                Coordinate playerCoord = getLocalPlayer().getCoordinate();
                if (playerCoord.getX() >= 3292 && playerCoord.getX() <= 3296 &&
                        playerCoord.getY() >= 10135 && playerCoord.getY() <= 10137) {
                    if (Math.random() <= 0.70) {
                        Execution.delay(RandomGenerator.nextInt(302, 589));
                        ScriptConsole.println("Used Surge: " + ActionBar.useAbility("Surge"));
                        return true;
                    }
                    return false;
                }
                return false;
            });

            if (!surgedAtCorrectLocation) {
                println("Failed to surge to correct location or opted not to surge.");
                return;
            }

            Execution.delay(RandomGenerator.nextInt(400, 600));
            success = Portal.interact("Enter");
            println("Interacted with portal: " + success);
            if (!success) {
                println("Failed to interact with portal.");
                return;
            }

            boolean movedToNewRegion = Execution.delayUntil(15000, () -> getLocalPlayer().getCoordinate().getRegionId() != 13214);
            if (movedToNewRegion) {
                botState = BotState.INTERACT_WITH_ZAMMY;
            } else {
                println("Failed to move to the new region after entering portal.");
                botState = BotState.IDLE;
            }
        }
    }


    private long walkToPortalMaxGuild() {
        if (getLocalPlayer().isMoving()) {
            return random.nextLong(1200, 1500);
        }
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("The Zamorakian Undercity portal").option("Enter").results();
        if (!results.isEmpty()) {
            SceneObject portal = results.nearest();
            portal.interact("Enter");
            ScriptConsole.println("Interacted with portal.");
            Execution.delayUntil(random.nextLong(15000, 20000), () -> getLocalPlayer().getCoordinate().equals(new Coordinate(1759, 1341, 0)));
            botState = BotState.INTERACT_WITH_ZAMMY;
        }
        return random.nextInt(1200, 1500);
    }

    public void enterZammy() {
        while (getLocalPlayer() == null || getLocalPlayer().isMoving() || getLocalPlayer().getAnimationId() != -1) {
            Execution.delay(RandomGenerator.nextInt(800, 1000));
            println("Delaying until player is not moving or animating.");
        }

        EntityResultSet<SceneObject> sceneObjectQuery = SceneObjectQuery.newQuery().name("The Zamorakian Undercity").results();
        if (!sceneObjectQuery.isEmpty()) {
            SceneObject portal = sceneObjectQuery.nearest();
            long startTime = System.currentTimeMillis();
            long waitTime = RandomGenerator.nextInt(7500, 10000);
            while (System.currentTimeMillis() - startTime < waitTime) {
                boolean interacted = portal.interact("Enter");
                println("Interacted with Wooden thing: " + interacted);
                if (interacted && Execution.delayUntil(RandomGenerator.nextInt(800, 1000), Dialog::isOpen)) {
                    botState = BotState.ACCEPT_DIALOG;
                    break;
                }
            }
        }
    }

    private void useAltarofWar() {
        if (getLocalPlayer() == null) {
            println("Local player not found.");
            return;
        }

        if (getLocalPlayer().getPrayerPoints() < Skills.PRAYER.getActualLevel() * 100) {
            EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Altar of War").results();

            if (!query.isEmpty()) {
                SceneObject altar = query.nearest();
                if (altar != null && altar.validate()) {
                    boolean interacted = altar.interact("Pray");
                    if (interacted) {
                        println("Interacting with Altar of War: " + interacted);
                        Execution.delay(RandomGenerator.nextInt(3500, 4900));
                    } else {
                        println("Failed to interact with Altar of War.");
                    }
                } else {
                    println("Altar of War not found or not valid.");
                }
            } else {
                println("No Altar of War found nearby.");
            }
        }

        if (getLocalPlayer().getPrayerPoints() >= Skills.PRAYER.getActualLevel()) {
            println("Prayer points are at or above maximum, moving to next state.");
            if (useFamiliar) {
                manageFamiliarSummoning();
            } else {
                botState = BotState.WALK_TO_ZAMMY_PORTAL;
            }
        } else {
            println("Failed to replenish prayer points to maximum.");
        }
    }


    private void LoadPresetLogic() {
        EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Bank chest").results();
        if (!query.isEmpty()) {
            println("Loading preset!");
            SceneObject bankChest = query.nearest();
            bankChest.interact("Load Last Preset from");
            Execution.delay(RandomGenerator.nextInt(4500, 5000));
            if (!usePrayerPots)
                botState = BotState.PRAYING;
            else
                if (useFamiliar)
                    manageFamiliarSummoning();
                else
                    botState = BotState.WALK_TO_ZAMMY_PORTAL;
        }
    }
    public void manageFamiliarSummoning() {
        if (!scriptRunning) {
            return;
        }
        boolean isFamiliarSummoned = isFamiliarSummoned();
        int familiarTimeRemaining = VarManager.getVarbitValue(6055);

        if (isFamiliarSummoned) {
            familiarTimeRemaining = VarManager.getVarbitValue(6055);
            println("Familiar time remaining: " + familiarTimeRemaining + " Minutes");
        }


        if (!isFamiliarSummoned || familiarTimeRemaining <= 5) {
            summonFamiliar();
        } else {
            int scrollsStored = getScrollsStored();
            boolean hasScrolls = hasScrollsInInventory();
            println("Backpack contains Scrolls: " + hasScrolls + ", Scrolls stored in Familiar: " + scrollsStored);

            if (scrollsStored <= 50 && hasScrolls) {
                println("Handling inventory scrolls...");
                handleInventoryScrolls();
            } else {
                botState = BotState.WALK_TO_ZAMMY_PORTAL;
            }
        }
    }


    private void handleInventoryScrolls() {
        if (hasScrollsInInventory()) {
            storeMaxScrolls();
        } else {
            println("No scrolls found in inventory.");
            botState = BotState.WALK_TO_ZAMMY_PORTAL;
        }
    }

    private boolean hasScrollsInInventory() {
        ResultSet<Item> scrolls = InventoryItemQuery.newQuery(93).results();
        return scrolls.stream().anyMatch(item -> item.getName() != null && item.getName().toLowerCase().contains("scroll"));
    }

    private int getScrollsStored() {
        return VarManager.getVarbitValue(25412);
    }

    private void summonFamiliar() {
        if (!scriptRunning) {
            return;
        }
        ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();

        Item itemToSummon = items.stream()
                .filter(item -> item.getName() != null &&
                        (item.getName().toLowerCase().contains("pouch") ||
                                item.getName().toLowerCase().contains("contract")) &&
                        !item.getName().toLowerCase().contains("rune"))
                .findFirst()
                .orElse(null);

        if (itemToSummon != null) {
            println("Attempting to summon with: " + itemToSummon.getName());
            boolean success = Backpack.interact(itemToSummon.getName(), "Summon");
            Execution.delay(RandomGenerator.nextInt(1600, 2100));

            if (success) {
                println("Summoned familiar with: " + itemToSummon.getName());
                boolean familiarSummoned = Execution.delayUntil(10000, this::isFamiliarSummoned);
                if (familiarSummoned) {
                    println(itemToSummon.getName() + " is now summoned.");
                    manageFamiliarSummoning();
                } else {
                    println("Failed to confirm the summoning of familiar with " + itemToSummon.getName() + ".");
                    LoadPresetLogic();
                }
            } else {
                println("Failed to summon familiar with: " + itemToSummon.getName());
                LoadPresetLogic();
            }
        } else {
            println("No suitable 'pouch' or 'contract' items for summoning found in Backpack.");
            botState = BotState.WALK_TO_ZAMMY_PORTAL;
        }
    }

    private void storeMaxScrolls() {
        println("Attempting to store scrolls in familiar.");
        boolean success = Objects.requireNonNull(ComponentQuery.newQuery(662).componentIndex(78).results().first()).interact(1);
        Execution.delay(RandomGenerator.nextInt(800, 1000));
        if (success) {
            println("Successfully stored scrolls in familiar.");
            botState = BotState.WALK_TO_ZAMMY_PORTAL;
        } else {
            println("Failed to store scrolls in familiar.");
            botState = BotState.IDLE;
        }
    }

    private boolean isFamiliarSummoned() {
        Component familiarComponent = ComponentQuery.newQuery(284).spriteId(26095).results().first();
        return familiarComponent != null;
    }


    public void eatFood() {
        if (getLocalPlayer() != null) {

            if (getLocalPlayer().getCurrentHealth() * 100 / getLocalPlayer().getMaximumHealth() < 50) {
                {
                    ResultSet<Item> food = InventoryItemQuery.newQuery(93).option("Eat").results();
                    if (!food.isEmpty()) {
                        Item eat = food.first();
                        Backpack.interact(eat.getName(), 1);
                        println("Eating " + eat.getName());
                        Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> getLocalPlayer().getCurrentHealth() > 8000);
                    } else {
                        botState = BotState.IDLE;
                        println("No food found!");
                    }
                }
            }
        }
    }


    private void LoadMaxGuildPresetLogic() {
        EntityResultSet<Npc> query = NpcQuery.newQuery().name("Banker").results();
        if (!query.isEmpty()) {
            println("Loading preset!");
            Npc bankChest = query.nearest();
            if (bankChest != null) {
                bankChest.interact("Load Last Preset from");
            }
            Execution.delay(RandomGenerator.nextInt(1600, 2600));
            botState = BotState.PRAYING;

            if (player.getCurrentHealth() < player.getMaximumHealth()) {
                println("Healing up!");
                Execution.delay(RandomGenerator.nextInt(600, 1000));
            }
        }
    }

    public long admireThroneofFame() {
        if (player.isMoving()) {
            return random.nextLong(1200, 1500);
        }
        int currentPrayerPoints = LocalPlayer.LOCAL_PLAYER.getPrayerPoints();
        int maxPrayerPoints = Skills.PRAYER.getActualLevel() * 100;

        if (currentPrayerPoints < maxPrayerPoints) {
            EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Throne of Fame").results();
            if (!query.isEmpty()) {
                SceneObject throne = query.nearest();
                throne.interact("Admire");
                ScriptConsole.println("Admiring Throne of Fame.");
                Execution.delayUntil(random.nextLong(15000, 20000), () -> LocalPlayer.LOCAL_PLAYER.getPrayerPoints() == maxPrayerPoints);
                botState = BotState.WALK_TO_ZAMMY_PORTAL;
            }
        } else {
            botState = BotState.WALK_TO_ZAMMY_PORTAL;
        }
        return random.nextLong(1200, 1500);
    }

    public void usePontifex() {
        ResultSet<Item> results = InventoryItemQuery.newQuery(93).ids(53032).option("Teleport (Senntisten)").results();

        if (!results.isEmpty()) {
            Item item = results.first();

            Backpack.interact(item.getName(), 2);


//            if (success) {
//                println("Successfully used Teleport (Senntisten).");
//            } else {
//                println("Failed to use Teleport (Senntisten).");
//            }
        } else {
            println("Pontifix Ring not found in inventory.");
        }

        botState = BotState.WALKTOZAMMY;

    }

    private boolean isAtPontiFexTele() {
        if (getLocalPlayer() != null) {
            Coordinate myPos = getLocalPlayer().getCoordinate();
            return myPos.getX() == 1759 && myPos.getY() == 1261;
        }
        return false;
    }

    public void moveToSpecificLocation() {
        Coordinate currentLocation = player.getCoordinate();
        if (isAtPontiFexTele()) {
            if (currentLocation.equals(new Coordinate(1759, 1261, 0))) {
                Movement.walkTo(1759, 1292, true);
                Execution.delayUntil((30000), () -> Distance.between(player.getCoordinate(), new Coordinate(1759, 1292, 0)) <= 5);

                Movement.walkTo(1747, 1313, true);
                Execution.delayUntil((30000), () -> Distance.between(player.getCoordinate(), new Coordinate(1747, 1313, 0)) <= 5);

                Movement.walkTo(1760, 1341, true);
            } else {
                println("Player is not at the starting location.");
            }
            botState = BotState.INTERACT_WITH_ZAMMY;
        }
    }

    public void FirstWalk() {
        if (WalkTo(CoordX, CoordY)) {
            botState = BotState.MOVE_TO_CEBERUS;
            println("Walking to boss!");
        }
    }

    public void Movetominiboss() {
        Execution.delay(random.nextLong(1750, 2250));
        Coordinate startCoordinate = player.getCoordinate();
        Coordinate targetCoordinate = new Coordinate(
                startCoordinate.getX() + 30,
                startCoordinate.getY() + 40,
                startCoordinate.getZ()
        );


        if (usePrayerPots) {
            usePrayerOrRestorePots();
        }
        if (useWen) {
            activateScriptureOfWen();
        }
        if (useJas) {
            activateScriptureOfJas();
        }
        if (useQuickPrayers) {
            enableQuickPrayers();
        }


        println("Moving towards the target coordinates.");
        Movement.walkTo(targetCoordinate.getX(), targetCoordinate.getY(), false);

        boolean[] surgeUsed = new boolean[2];

        boolean reached = Execution.delayUntil(30000, () -> {
            Coordinate currentPlayerPosition = player.getCoordinate();

            if (!surgeUsed[0] && player.isMoving() &&
                    currentPlayerPosition.getY() <= startCoordinate.getY() + 5 &&
                    ActionBar.getCooldownPrecise("Surge") == 0) {
                if (Math.random() <= 0.85) {
                    Execution.delay(RandomGenerator.nextInt(600, 1750));
                    println("Used Surge: " + ActionBar.useAbility("Surge"));
                    Execution.delay(200);
                    Movement.walkTo(targetCoordinate.getX(), targetCoordinate.getY(), false);
                    surgeUsed[0] = true;
                } else {
                    println("Chose not to Surge the first time.");
                    surgeUsed[0] = true;
                }
            }


            if (!surgeUsed[1] && player.isMoving() &&
                    currentPlayerPosition.getX() >= startCoordinate.getX() + 9 &&
                    currentPlayerPosition.getY() >= startCoordinate.getY() + 28 &&
                    ActionBar.getCooldownPrecise("Surge") == 0) {
                if (Math.random() <= 0.85) {
                    Execution.delay(RandomGenerator.nextInt(250, 600));
                    println("Used Surge: " + ActionBar.useAbility("Surge"));
                    Execution.delay(200);
                    Movement.walkTo(targetCoordinate.getX(), targetCoordinate.getY(), false);
                    surgeUsed[1] = true;
                } else {
                    println("Chose not to Surge the second time.");
                    surgeUsed[1] = true;
                }
            }

            return Distance.between(currentPlayerPosition, targetCoordinate) <= 1;
        });


        if (reached) {
            println("Reached Destination.");
            botState = BotState.ATTACK_CEBERUS;
        }
    }
    private void attackMiniBoss() {
        if (getLocalPlayer() != null) {
            EntityResultSet<Npc> npcQuery = NpcQuery.newQuery().name("Cerberus Juvenile").results();
            if (!npcQuery.isEmpty()) {
                println("Cerberus Juvenile found, attempting to attack.");
                Npc boss = npcQuery.nearest();
                if (boss != null) {
                    println("Attacked Cerberus Juvenile: " + boss.interact("Attack"));
                    if (useInvokeDeath) {
                        useInvokeDeath();
                    }
                    if (useOverloads) {
                        drinkOverloads();
                    }
                    while (true) {
                        // Check if prayer or essence of finality needs to be used
                        if (usePrayerPots) {
                            usePrayerOrRestorePots();
                        }
                        if (useEssenceOfFinality) {
                            essenceOfFinality();
                        }
                        if (Backpack.contains("Vulnerability bomb")) {
                            useVulnBomb();
                        }
                        if (useSmokeCloud) {
                            CastSmokeCloud();
                        }

                        // Check if the NPCs are no longer present
                        boolean noNpcsLeft = Execution.delayUntil(1000, () -> {
                            EntityResultSet<Npc> currentQuery = NpcQuery.newQuery().name("Cerberus Juvenile").results();
                            return currentQuery.nearest().getCurrentHealth() == 0;
                        });

                        // If no NPCs are left, break the loop
                        if (noNpcsLeft) {
                            break;
                        }
                    }
                    println("Assuming Cerberus Juvenile is defeated.");

                    if (killMages) {
                        // Wait until the player is out of combat
                        boolean isOutOfCombat = Execution.delayUntil(120000, () -> !getLocalPlayer().inCombat());
                        if (isOutOfCombat) {
                            if (Backpack.contains("Falador teleport")) {
                                Backpack.interact("Falador teleport", "Break");
                                Execution.delay(RandomGenerator.nextInt(2500, 3500));
                                botState = BotState.IDLE;
                            } else {
                                botState = BotState.IDLE;
                            }
                        }

                    } else {
                        if (Backpack.contains("Falador teleport")) {
                            Backpack.interact("Falador teleport", "Break");
                            Execution.delay(RandomGenerator.nextInt(2500, 3500));
                        }
                        botState = BotState.IDLE;
                    }
                } else {
                    println("Failed to interact with Cerberus Juvenile.");
                }
            } else {
                println("No Cerberus Juvenile found.");
            }
            println("Run completed.");
        } else {
            println("Failed to reach the target coordinates within the time limit.");
        }
    }

    Pattern overloads = Pattern.compile(Regex.getPatternForContainsString("overload").pattern(), Pattern.CASE_INSENSITIVE);

    public void drinkOverloads() {
        if (getLocalPlayer() != null && VarManager.getVarbitValue(26037) == 0) {

            ResultSet<Item> items = InventoryItemQuery.newQuery().results();

            Item overloadPot = items.stream()
                    .filter(item -> item.getName() != null && overloads.matcher(item.getName()).find())
                    .findFirst()
                    .orElse(null);

            if (overloadPot != null) {
                println("Drinking " + overloadPot.getName());
                Execution.delay(RandomGenerator.nextInt(600, 1500));
                Backpack.interact(overloadPot.getName(), "Drink");
                Execution.delay(RandomGenerator.nextInt(1180, 1220));
            }
        }
    }

    public void usePrayerOrRestorePots() {
        Player localPlayer = Client.getLocalPlayer();
        if (localPlayer != null) {
            int currentPrayerPoints = LocalPlayer.LOCAL_PLAYER.getPrayerPoints();
            int maxPrayerPoints = Skills.PRAYER.getActualLevel() * 100; // Assuming each prayer level gives 100 prayer points

            if (currentPrayerPoints < maxPrayerPoints * 0.5) { // Check if current prayer points are less than 50% of max prayer points
                ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();

                Item prayerOrRestorePot = items.stream()
                        .filter(item -> item.getName() != null &&
                                (item.getName().toLowerCase().contains("prayer") ||
                                        item.getName().toLowerCase().contains("restore")))
                        .findFirst()
                        .orElse(null);

                if (prayerOrRestorePot != null) {
                    println("Drinking " + prayerOrRestorePot.getName());
                    Execution.delay(RandomGenerator.nextInt(600, 1500));
                    boolean success = Backpack.interact(prayerOrRestorePot.getName(), "Drink");
                    Execution.delay(RandomGenerator.nextInt(1180, 1220));

                    if (!success) {
                        println("Failed to use " + prayerOrRestorePot.getName());
                    }
                } else {
                    println("No Prayer or Restore pots found.");
                }
            }
        }
    }

    private void useVulnBomb() {
        int vulnDebuffVarbit = VarManager.getVarbitValue(1939);
        if (vulnDebuffVarbit == 0 && !vulnBombUsed) {
            ActionBar.useItem("Vulnerability bomb", "Throw");
            vulnBombUsed = true;
            Execution.delay(RandomGenerator.nextInt(800, 1000));
            println("Vulnerability bomb used.");
        }
    }
    private void CastSmokeCloud() {
        int debuffVarbit = VarManager.getVarbitValue(49448);
        if (debuffVarbit == 0 && !smokeCloudUsed) {
            println("Used Smoke Cloud: " + ActionBar.useAbility("Smoke Cloud"));
            smokeCloudUsed = true;
        }
    }

    private void useInvokeDeath() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(53247) == 0) {
                println("Used Invoke: " + ActionBar.useAbility("Invoke Death"));
                Execution.delayUntil(RandomGenerator.nextInt(2400, 3000), () -> VarManager.getVarbitValue(53247) == 0);
            }
        }
    }



    public void enableQuickPrayers() {
        if (!quickPrayersActive && !isQuickPrayersActive()) {
            println("Activating Quick Prayers.");
            if (ActionBar.useAbility("Quick-prayers 1")) {
                println("Quick Prayers activated successfully.");
                quickPrayersActive = true;
            } else {
                println("Failed to activate Quick Prayers.");
            }
        }
    }

    public void disableQuickPrayers() {
        if (quickPrayersActive && isQuickPrayersActive()) {
            println("Deactivating Quick Prayers.");
            if (ActionBar.useAbility("Quick-prayers 1")) {
                println("Quick Prayers deactivated.");
                quickPrayersActive = false;
            } else {
                println("Failed to deactivate Quick Prayers.");
            }
        }
    }

    private boolean isQuickPrayersActive() {
        int[] varbitIds = {
                // Curses
                16761, 16762, 16763, 16786, 16764, 16765, 16787, 16788, 16765, 16766,
                16767, 16768, 16769, 16770, 16771, 16772, 16781, 16773, 16782, 16774,
                16775, 16776, 16777, 16778, 16779, 16780, 16784, 16783, 29065, 29066,
                29067, 29068, 29069, 49330, 29071, 34866, 34867, 34868, 53275, 53276,
                53277, 53278, 53279, 53280, 53281,
                // Normal
                16739, 16740, 16741, 16742, 16743, 16744, 16745, 16746, 16747, 16748,
                16749, 16750, 16751, 16752, 16753, 16754, 16755, 16756, 16757, 16758,
                16759, 16760, 53271, 53272, 53273, 53274
        };

        for (int varbitId : varbitIds) {
            if (VarManager.getVarbitValue(varbitId) == 1) {
                return true;
            }
        }
        return false;
    }
    private void activateScriptureOfJas() {
        if (VarManager.getVarbitValue(30605) == 0 && VarManager.getVarbitValue(30604) >= 60) {
            Execution.delay(RandomGenerator.nextInt(600, 1500));
            println("Activated Scripture of Jas:  " + Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate"));
        }
    }


    private void deactivateScriptureOfJas() {
        if (VarManager.getVarbitValue(30605) == 1) {
            Execution.delay(RandomGenerator.nextInt(600, 1500));
            println("Deactivated Scripture of Jas:  " + Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate"));
        }
    }


    private void activateScriptureOfWen() {
        if (VarManager.getVarbitValue(30605) == 0 && VarManager.getVarbitValue(30604) >= 60) {
            Execution.delay(RandomGenerator.nextInt(600, 1500));
            println("Activated Scripture of Wen:  " + Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate"));
        }
    }

    private void deactivateScriptureOfWen() {
        if (VarManager.getVarbitValue(30605) == 1) {
            Execution.delay(RandomGenerator.nextInt(600, 1500));
            println("Deactivated Scripture of Wen:  " + Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate"));
        }
    }

    public static int NecrosisStacksThreshold = 12;

    private void essenceOfFinality() {
        if (getLocalPlayer() != null) {
            if (getLocalPlayer().getAdrenaline() >= 250
                    && ComponentQuery.newQuery(291).spriteId(55524).results().isEmpty()
                    && ActionBar.getCooldownPrecise("Essence of Finality") == 0 && getLocalPlayer().inCombat() && getLocalPlayer().getFollowing() != null
                    && getLocalPlayer().hasTarget()
                    && ActionBar.getCooldownPrecise("Essence of Finality") == 0) {
                int currentNecrosisStacks = VarManager.getVarValue(VarDomainType.PLAYER, 10986);
                if (currentNecrosisStacks >= NecrosisStacksThreshold) {
                    boolean abilityUsed = ActionBar.useAbility("Essence of Finality");
                    if (abilityUsed) {
                        println("Used Death Grasp with " + currentNecrosisStacks + " Necrosis stacks.");
                        Execution.delayUntil(RandomGenerator.nextInt(5000, 10000), () -> ComponentQuery.newQuery(291).spriteId(55524).results().isEmpty());
                    } else {
                        println("Attempted to use Death Grasp, but ability use failed.");
                    }
                }
            }
        }
    }

    private boolean shouldBank(LocalPlayer player) {
        boolean hasShadowReefTeleport = Backpack.contains("Shadow Reef teleport");
        Pattern overloadPattern = Pattern.compile("overload", Pattern.CASE_INSENSITIVE);
        Pattern prayerRestorePattern = Pattern.compile("prayer|restore", Pattern.CASE_INSENSITIVE);

        boolean hasOverload = false;
        boolean hasPrayerOrRestorePotion = false;

        for (Item item : Backpack.getItems()) {
            String itemName = item.getName();


            if (overloadPattern.matcher(itemName).find()) {
                hasOverload = true;
            }


            if (prayerRestorePattern.matcher(itemName).find()) {
                hasPrayerOrRestorePotion = true;
            }


            if (hasOverload && hasPrayerOrRestorePotion) {
                break;
            }
        }

        return (useOverloads && !hasOverload) || (usePrayerPots && !hasPrayerOrRestorePotion) || !hasShadowReefTeleport;
    }



    private void ringOfKinship() {
        if (getLocalPlayer() != null) {
            if (Backpack.contains("Ring of kinship")) {
                Backpack.interact("Ring of kinship", "Teleport to Daemonheim");
                ScriptConsole.println("Interacted with Ring of Kinship.");
                Execution.delay(RandomGenerator.nextInt(5000, 7000));
                bankFremenikBanker();
            } else {
                println("Ring of Kinship not found in inventory.");
            }
        }
    }

    private void bankFremenikBanker() {
        EntityResultSet<Npc> banker = NpcQuery.newQuery().name("Fremennik banker").option("Bank").results();
        if (!banker.isEmpty()) {
            Npc bank = banker.nearest();
            bank.interact("Load Last Preset from");
            ScriptConsole.println("Interacted with Fremennik banker.");
            Execution.delay(random.nextInt(8500, 10000));
            if (Backpack.contains("Shadow Reef teleport")) {
                botState = BotState.IDLE;
            } else {
                buyTablets();
            }
        }
    }
    private void buyTablets() {
        EntityResultSet<Npc> bryllResultSet = NpcQuery.newQuery().name("Bryll Thoksdottir").option("Reward Shop").results();

        if (bryllResultSet.isEmpty()) {
            println("Bryll Thoksdottir not found.");
            return;
        }

        Npc bryll = bryllResultSet.nearest();
        boolean interacted = bryll.interact("Reward Shop");

        if (!interacted) {
            println("Failed to interact with Bryll Thoksdottir.");
            return;
        }

        boolean shopOpened = Execution.delayUntil(RandomGenerator.nextInt(10000, 15000), () -> Interfaces.isOpen(1594));

        if (!shopOpened) {
            println("Failed to open the reward shop.");
            return;
        }

        Execution.delay(RandomGenerator.nextInt(800, 1000));
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 104464385);
        Execution.delay(RandomGenerator.nextInt(800, 1000));
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 4, 2, 104464401);
        Execution.delay(RandomGenerator.nextInt(800, 1000));

        if (Interfaces.isOpen(1594)) {
            Execution.delay(RandomGenerator.nextInt(800, 1000));
            MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 104464438);
            botState = BotState.IDLE;
            println("Successfully bought tablets.");
        } else {
            println("Failed to buy tablets.");
        }
    }

    private void acceptDialog() {
        if (getLocalPlayer() != null) {
            if (Dialog.isOpen()) {
                for (String option : Dialog.getOptions()) {
                    if (option == null) {
                        continue;
                    }

                    if (option.contains("No")) {
                        Execution.delay(RandomGenerator.nextInt(800, 1000));
                        Dialog.interact(option);
                        println("Selected 'No' option");

                        // Wait for the "Normal mode" option to appear
                        boolean normalModeAppeared = Execution.delayUntil(5000, () -> {
                            for (String nextOption : Dialog.getOptions()) {
                                if (nextOption != null && nextOption.contains("Normal mode")) {
                                    return true;
                                }
                            }
                            return false;
                        });  // Wait up to 5 seconds

                        if (normalModeAppeared) {
                            Execution.delay(RandomGenerator.nextInt(800, 1000));
                            Dialog.interact("Normal mode");
                            println("Selected 'Normal mode' option");
                            botState = BotState.MOVE_TO_CEBERUS;
                        } else {
                            println("'Normal mode' option did not appear.");
                            botState = BotState.INTERACT_WITH_ZAMMY;
                        }

                        break;  // Exit the loop after selecting the "No" option
                    }
                }
            }
        } else {
            Execution.delay(RandomGenerator.nextInt(600, 2500));
            println("Dialog not open.");
        }
    }


    public void useMaxGuildTeleport() {
        if (getLocalPlayer() != null) {
            ActionBar.useAbility("Max guild Teleport");
            Execution.delay(RandomGenerator.nextInt(4500, 7000));
            botState = BotState.BANKING;
        }
    }

    private void useWarsTeleport() {
        if (getLocalPlayer() != null) {
            if (getLocalPlayer().getCoordinate().getRegionId() != 13214) {
                println("Using Wars Retreat Teleport");
                ActionBar.useAbility("War's Retreat Teleport");
                Execution.delay(RandomGenerator.nextInt(4500, 7000));
            }
            botState = BotState.BANKING;
        }
    }


    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }


    public boolean WalkTo(int x, int y) {
        if (getLocalPlayer() != null) {
            Coordinate myPos = getLocalPlayer().getCoordinate();
            if (myPos.getX() != x && myPos.getY() != y) {

                if (!getLocalPlayer().isMoving()) {
                    println("Walking to: " + x + ", " + y);
                    Movement.walkTo(x, y, false);
                    Execution.delay(RandomGenerator.nextInt(300, 500));
                }
                return false;
            } else {
                return true;
            }
        }
        return false;
    }



    private void useTablet() {
        if (Backpack.contains("Shadow Reef teleport")) {
            Backpack.interact("Shadow Reef teleport", "Break");
            ScriptConsole.println("Interacted with Shadow Reef teleport.");
            Execution.delay(random.nextInt(6000, 7500));
            botState = BotState.BANKING;
        } else {
            println("No Shadow Reef teleport found in inventory.");
        }
    }
    private void useTreasureChest() {
        EntityResultSet<SceneObject> treasureChest = SceneObjectQuery.newQuery().name("Treasure chest").option("Search").results();
        if (!treasureChest.isEmpty()) {
            SceneObject chest = treasureChest.nearest();
            chest.interact("Teleport");
            ScriptConsole.println("Interacted with Treasure chest.");
            Execution.delayUntil(random.nextInt(5000, 10000), () -> Interfaces.isOpen(720));
            if (Interfaces.isOpen(720)) {
                ScriptConsole.println("Opened Treasure chest interface.");
                Execution.delay(random.nextInt(800, 1000));
                MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 47185943);
                ScriptConsole.println("Interacting with Treasure chest interface.");
                botState = BotState.INTERACT_WITH_ZAMMY;
            }
        } else {
            println("No Treasure chest found.");
        }
    }

    public void saveConfiguration() {
        this.configuration.addProperty("usePrayerPots", String.valueOf(this.usePrayerPots));
        this.configuration.addProperty("useEssenceOfFinality", String.valueOf(this.useEssenceOfFinality));
        this.configuration.addProperty("useOverloads", String.valueOf(this.useOverloads));
        this.configuration.addProperty("useInvokeDeath", String.valueOf(this.useInvokeDeath));
        this.configuration.addProperty("useDarkness", String.valueOf(this.useDarkness));
        this.configuration.addProperty("usePontifexRing", String.valueOf(this.usePontifexRing));
        this.configuration.addProperty("useMaxGuild", String.valueOf(this.useMaxGuild));
        this.configuration.addProperty("useWarsRetreat", String.valueOf(this.useWarsRetreat));
        this.configuration.addProperty("useBank", String.valueOf(this.useBank));
        this.configuration.addProperty("useWen", String.valueOf(this.useWen));
        this.configuration.addProperty("useJas", String.valueOf(this.useJas));
        this.configuration.addProperty("UseSmokeCloud", String.valueOf(this.useSmokeCloud));
        this.configuration.addProperty("UseFamiliar", String.valueOf(this.useFamiliar));
        this.configuration.addProperty("NecrosisStacksThreshold", String.valueOf(NecrosisStacksThreshold));
        this.configuration.addProperty("useQuickPrayers", String.valueOf(this.useQuickPrayers));
        this.configuration.addProperty("killMages", String.valueOf(this.killMages));
        this.configuration.addProperty("useMaxGuild", String.valueOf(this.useMaxGuild));
        this.configuration.addProperty("useShadowReefTeleport", String.valueOf(this.useShadowReefTeleport));
        this.configuration.save();
    }

    public void loadConfiguration() {
        try {
            this.usePrayerPots = Boolean.parseBoolean(this.configuration.getProperty("usePrayerPots"));
            this.useEssenceOfFinality = Boolean.parseBoolean(this.configuration.getProperty("useEssenceOfFinality"));
            this.useOverloads = Boolean.parseBoolean(this.configuration.getProperty("useOverloads"));
            this.useInvokeDeath = Boolean.parseBoolean(this.configuration.getProperty("useInvokeDeath"));
            this.useDarkness = Boolean.parseBoolean(this.configuration.getProperty("useDarkness"));
            this.usePontifexRing = Boolean.parseBoolean(this.configuration.getProperty("usePontifexRing"));
            this.useMaxGuild = Boolean.parseBoolean(this.configuration.getProperty("useMaxGuild"));
            this.useWarsRetreat = Boolean.parseBoolean(this.configuration.getProperty("useWarsRetreat"));
            this.useBank = Boolean.parseBoolean(this.configuration.getProperty("useBank"));
            this.useWen = Boolean.parseBoolean(this.configuration.getProperty("useWen"));
            this.useJas = Boolean.parseBoolean(this.configuration.getProperty("useJas"));
            this.useSmokeCloud = Boolean.parseBoolean(this.configuration.getProperty("UseSmokeCloud"));
            this.useFamiliar = Boolean.parseBoolean(this.configuration.getProperty("UseFamiliar"));
            this.useQuickPrayers = Boolean.parseBoolean(this.configuration.getProperty("useQuickPrayers"));
            this.useEssenceOfFinality = Boolean.parseBoolean(this.configuration.getProperty("useEssenceOfFinality"));
            this.killMages = Boolean.parseBoolean(this.configuration.getProperty("killMages"));
            this.useMaxGuild = Boolean.parseBoolean(this.configuration.getProperty("useMaxGuild"));
            this.useShadowReefTeleport = Boolean.parseBoolean(this.configuration.getProperty("useShadowReefTeleport"));
            String necrosisThresholdValue = this.configuration.getProperty("NecrosisStacksThreshold");
            if (necrosisThresholdValue != null && !necrosisThresholdValue.isEmpty()) {
                int necrosisThreshold = Integer.parseInt(necrosisThresholdValue);
                if (necrosisThreshold < 0) necrosisThreshold = 0;
                else if (necrosisThreshold > 12) necrosisThreshold = 12;
                NecrosisStacksThreshold = necrosisThreshold;
            }
        } catch (NumberFormatException e) {
            println("Error parsing threshold values. Using defaults.");
            NecrosisStacksThreshold = 12; // Default or a logical fallback
        }
    }
}
