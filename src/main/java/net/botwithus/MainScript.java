package net.botwithus;

import net.botwithus.api.game.hud.Dialog;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
import net.botwithus.rs3.game.movement.Movement;
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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.botwithus.rs3.game.Client.getLocalPlayer;

public class MainScript extends LoopingScript {

    public boolean runScript;


    public int CoordX;
    public int CoordY;
    private BotState botState;
    boolean usePortal = false;
    boolean usePontifexRing = false;
    boolean useMaxGuild = false;
    boolean usePrayer;
    boolean useEssenceOfFinality;
    private boolean scriptRunning = false;
    long runStartTime;
    private Instant scriptStartTime;

    int runCount = 0;
    int totalTokens = 0;
    boolean useWarsRetreat = true;
    public boolean useDarkness;
    boolean useOverload;
    boolean useInvokeDeath;
    public boolean useDeflectMagic;
    public boolean useSorrow;
    public boolean useWeaponPoison;
    public boolean useBank;
    public boolean useRuination;

    public int getCurrentDungTokens() {
        return VarManager.getVarValue(VarDomainType.PLAYER, 1097);
    }


    enum BotState {
        WALKTOPORTAL, PRAYING, BANKING, INTERACT_WITH_ZAMMY, WALKTOZAMMY, MOVE_TO_CEBERUS, ACCEPT_DIALOG, FirstWalk, IDLE
    }

    public MainScript(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
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
                if (VarManager.getVarbitValue(16768) == 1 && useDeflectMagic) {
                    ActionBar.usePrayer("Deflect Magic");
                    println("Deactivated Deflect Magic prayer.");
                    Execution.delay(RandomGenerator.nextInt(100, 500));
                }
                if (VarManager.getVarbitValue(53280) == 1 && useRuination) {
                    ActionBar.usePrayer("Ruination");
                    println("Deactivated Ruination.");
                    Execution.delay(RandomGenerator.nextInt(100, 500));
                }
                if (useMaxGuild) {
                    useMaxGuildTeleport();
                } else if (useWarsRetreat || usePontifexRing) {
                    useWarsTeleport();
                }
            }
            case BANKING -> {
                if (useMaxGuild) {
                    LoadMaxGuildPresetLogic();
                } else if (useWarsRetreat || usePontifexRing) {
                    if (useBank)
                        LoadPresetLogic();
                    if (!usePrayer)
                        botState = BotState.PRAYING;
                    else {
                        ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();

                        Item prayerOrRestorePot = items.stream()
                                .filter(item -> item.getName() != null &&
                                        (item.getName().toLowerCase().contains("prayer") ||
                                                item.getName().toLowerCase().contains("restore")))
                                .findFirst()
                                .orElse(null);

                        if (prayerOrRestorePot == null) {
                            println("No Prayer or Restore pots found, interacting with bank chest.");
                            LoadPresetLogic();
                        } else {
                            botState = BotState.WALKTOPORTAL;
                        }
                    }
                } else {
                    botState = BotState.IDLE;
                }
            }

            case PRAYING -> {
                if (useMaxGuild) {
                    admireThroneofFame();
                } else if (useWarsRetreat || usePontifexRing) {
                    useAltarofWar();
                }
            }
            case WALKTOPORTAL -> {
                if (useMaxGuild) {
                    walkToPortalMaxGuild();
                } else if (useWarsRetreat) {
                    walkToPortal();
                } else if (usePontifexRing) {
                    usePontifex();
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
                attackMiniBoss();
            }


        }
    }

    private void walkToPortal() {
        if (getLocalPlayer() == null) {
            return;
        }
        EntityResultSet<SceneObject> ZammyPortal = SceneObjectQuery.newQuery().name("Portal (The Zamorakian Undercity)").results();
        if (!ZammyPortal.isEmpty()) {
            SceneObject Portal = ZammyPortal.nearest();
            if (Portal != null) {
                Portal.interact("Enter");
                println("Interacting with portal...");

                boolean surgedAtCorrectLocation = Execution.delayUntil(15000, () -> {
                    Coordinate playerCoord = getLocalPlayer().getCoordinate();
                    if (playerCoord.getX() <= 3295 && playerCoord.getY() <= 10134) {
                        Execution.delay(RandomGenerator.nextInt(600, 1500));
                        ScriptConsole.println("Used Surge: " + ActionBar.useAbility("Surge"));
                        return true;
                    }
                    return false;
                });

                if (surgedAtCorrectLocation) {
                    Execution.delay(RandomGenerator.nextInt(200, 400));
                    Portal.interact("Enter");
                    println("Interacting with portal...");
                    Execution.delayUntil(15000, () -> getLocalPlayer().getCoordinate().getRegionId() != 13214);
                    botState = BotState.INTERACT_WITH_ZAMMY;
                }
            }
        }
    }

    private void walkToPortalMaxGuild() {
        if (getLocalPlayer() != null) {
            if (!getLocalPlayer().isMoving()) {
                EntityResultSet<SceneObject> sceneObjectQuery = SceneObjectQuery.newQuery().name("The Zamorakian Undercity portal").results();
                if (!sceneObjectQuery.isEmpty()) {
                    SceneObject portal = sceneObjectQuery.nearest();
                    portal.interact("Enter");
                    botState = BotState.INTERACT_WITH_ZAMMY;
                }
            }
        }
    }

    public void enterZammy() {
        if (getLocalPlayer() != null) {
            if (!getLocalPlayer().isMoving()) {
                EntityResultSet<SceneObject> sceneObjectQuery = SceneObjectQuery.newQuery().name("The Zamorakian Undercity").results();
                if (!sceneObjectQuery.isEmpty()) {
                    SceneObject portal = sceneObjectQuery.nearest();
                    portal.interact("Enter");
                    botState = BotState.ACCEPT_DIALOG;
                }
            }
        }
    }

    private void useAltarofWar() {
        // Check if the local player is available
        if (getLocalPlayer() == null) {
            println("Local player not found.");
            return;
        }

        // Check if the player's prayer points are below their maximum.
        if (getLocalPlayer().getPrayerPoints() < Skills.PRAYER.getActualLevel() * 100) {
            EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Altar of War").results();

            if (!query.isEmpty()) {
                SceneObject altar = query.nearest();
                if (altar != null && altar.validate()) {
                    boolean interacted = altar.interact("Pray");
                    if (interacted) {
                        println("Interacting with Altar of War.");
                        Execution.delay(RandomGenerator.nextInt(3500, 4900)); // Wait for the prayer points to potentially replenish
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

        // After attempting to replenish prayer points or if they're already at maximum
        if (getLocalPlayer().getPrayerPoints() >= Skills.PRAYER.getActualLevel()) {
            println("Prayer points are at or above maximum, moving to next state.");
            botState = BotState.WALKTOPORTAL;
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
            botState = BotState.PRAYING;
        }
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

    private boolean isAtMaxGuildBank() {
        if (getLocalPlayer() != null) {
            Coordinate myPos = getLocalPlayer().getCoordinate();
            return myPos.getX() == 2276 && myPos.getY() == 3311;
        }
        return false;
    }

    private void LoadMaxGuildPresetLogic() {

        if (WalkTo(2276, 3311)) {
            if (isAtMaxGuildBank()) {
                EntityResultSet<Npc> query = NpcQuery.newQuery().name("Banker").results();
                if (!query.isEmpty()) {
                    println("Loading preset!");
                    Npc bankChest = query.nearest();
                    if (bankChest != null) {
                        bankChest.interact("Load Last Preset from");
                    }
                    Execution.delay(RandomGenerator.nextInt(1600, 2600));
                    botState = BotState.PRAYING;

                    //wait at bank if we aren't full health
                    if (getLocalPlayer().getCurrentHealth() < getLocalPlayer().getMaximumHealth()) {
                        println("Healing up!");
                        Execution.delay(RandomGenerator.nextInt(600, 1000));
                    }

                }
            }
        }
    }

    public void admireThroneofFame() {
        if (WalkTo(2276, 3306)) {
            EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Throne of Fame").results();
            if (!query.isEmpty()) {
                SceneObject throne = query.nearest();
                throne.interact("Admire");
                Execution.delay(RandomGenerator.nextInt(600, 1000));

            }
            botState = BotState.WALKTOPORTAL;
        }
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
            // Item is not available or does not have the specified option
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
        Coordinate currentLocation = getLocalPlayer().getCoordinate();
        if (isAtPontiFexTele()) {
            if (currentLocation.equals(new Coordinate(1759, 1261, 0))) {
                Movement.walkTo(1759, 1292, true);
                Execution.delayUntil((30000), () -> Distance.between(getLocalPlayer().getCoordinate(), new Coordinate(1759, 1292, 0)) <= 5);

                Movement.walkTo(1747, 1313, true);
                Execution.delayUntil((30000), () -> Distance.between(getLocalPlayer().getCoordinate(), new Coordinate(1747, 1313, 0)) <= 5);

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

    public void attackMiniBoss() {

        Coordinate targetCoordinate = new Coordinate(
                getLocalPlayer().getCoordinate().getX() + 30,
                getLocalPlayer().getCoordinate().getY() + 40,
                getLocalPlayer().getCoordinate().getZ()
        );
        if (usePrayer) {
            usePrayerOrRestorePots();
        }

        if (VarManager.getVarbitValue(16768) == 0 && useDeflectMagic) {
            ActionBar.usePrayer("Deflect Magic");
            println("Activated Deflect Magic prayer.");
            Execution.delay(RandomGenerator.nextInt(100, 500));
        }


        println("Moving towards the target coordinates.");
        Movement.walkTo(targetCoordinate.getX(), targetCoordinate.getY(), false);
        boolean reached = Execution.delayUntil(30000, () -> Distance.between(getLocalPlayer().getCoordinate(), targetCoordinate) <= 1);

        if (reached) {
            println("Reached target coordinates.");
            if (VarManager.getVarbitValue(53280) == 0 && useRuination) {
                ActionBar.usePrayer("Ruination");
                println("Activated Ruination.");
                Execution.delay(RandomGenerator.nextInt(100, 500));
            }

            EntityResultSet<Npc> npcQuery = NpcQuery.newQuery().name("Cerberus Juvenile").results();
            if (!npcQuery.isEmpty()) {
                println("Cerberus Juvenile found, attempting to attack.");
                Npc boss = npcQuery.nearest();
                if (boss != null && boss.interact("Attack")) {
                    println("Attacked Cerberus Juvenile, now invoking death.");
                    if (useInvokeDeath) {
                        useInvokeDeath();
                    }
                    if (useOverload) {
                        drinkOverloads();
                    }
                    if (useWeaponPoison) {
                        useWeaponPoison();
                    }
                    while (true) {
                        // Check if prayer or essence of finality needs to be used
                        if (usePrayer) {
                            usePrayerOrRestorePots();
                        }
                        if (useEssenceOfFinality) {
                            essenceOfFinality();
                        }

                        // Check if the NPCs are no longer present
                        boolean noNpcsLeft = Execution.delayUntil(1000, () -> { // Check every 10 seconds
                            EntityResultSet<Npc> currentQuery = NpcQuery.newQuery().name("Cerberus Juvenile").results();
                            return currentQuery.isEmpty();
                        });

                        // If no NPCs are left, break the loop
                        if (noNpcsLeft) {
                            break;
                        }
                    }
                    println("Assuming Cerberus Juvenile is defeated.");
                    botState = BotState.IDLE;
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

    private void useInvokeDeath() {
        if (getLocalPlayer() != null) {
            if (VarManager.getVarbitValue(53247) == 0) {
                println("Used Invoke: " + ActionBar.useAbility("Invoke Death"));
                Execution.delayUntil(RandomGenerator.nextInt(2400, 3000), () -> VarManager.getVarbitValue(53247) == 0);
            }
        }
    }

    public void useWeaponPoison() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer != null) {
            if (VarManager.getVarbitValue(2102) <= 3 && getLocalPlayer().getAnimationId() != 18068) { // 2102 = time remaining 18068, animation ID for drinking / 45317 = 4 on weapon poison+++
                ResultSet<Item> items = InventoryItemQuery.newQuery().results();
                Pattern poisonPattern = Pattern.compile("weapon poison\\+*?", Pattern.CASE_INSENSITIVE);

                Item weaponPoisonItem = items.stream()
                        .filter(item -> {
                            if (item.getName() == null) return false;
                            Matcher matcher = poisonPattern.matcher(item.getName());
                            return matcher.find();
                        })
                        .findFirst()
                        .orElse(null);

                if (weaponPoisonItem != null) {
                    println("Applying " + weaponPoisonItem.getName() + " ID: " + weaponPoisonItem.getId());
                    Execution.delay(RandomGenerator.nextInt(600, 1500));
                    Backpack.interact(weaponPoisonItem.getName(), "Apply");
                    println(weaponPoisonItem.getName() + "Has been applied");
                    Execution.delay(RandomGenerator.nextInt(600, 700));

                }
            }
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

    public boolean isDarknessActive() {
        Component darkness = ComponentQuery.newQuery(284).spriteId(30122).results().first();
        return darkness != null;
    }

    public void useDarkness() {
        if (getLocalPlayer() != null) {
            if (!isDarknessActive()) {
                ActionBar.useAbility("Darkness");
                println("Using darkness!");
                Execution.delay(RandomGenerator.nextInt(700, 1000));
            }
        }
    }

    private void acceptDialog() {
        if (Dialog.isOpen()) {
            Dialog.getOptions().forEach(option -> {
                /*println(option);*/
                if (option == null)
                    return;

                if (option.contains("No")) {
                    Execution.delay(RandomGenerator.nextInt(200, 300));
                    Dialog.interact(option);
                    println("Accepting first dialog");
                }


                if (option.contains("Normal mode")) {
                    Execution.delay(RandomGenerator.nextInt(500, 550));
                    Dialog.interact(option);
                    println("Accepting dialog!");
                    Execution.delay(RandomGenerator.nextInt(500, 1500));
                    botState = BotState.MOVE_TO_CEBERUS;
                }
            });
        }

    }

    public void useMaxGuildTeleport() {
        if (getLocalPlayer() != null) {
            ActionBar.useAbility("Max Guild Teleport");
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

    public void usePrayerOrRestorePots() {
        Player localPlayer = Client.getLocalPlayer();
        if (localPlayer != null) {
            int currentPrayerPoints = LocalPlayer.LOCAL_PLAYER.getPrayerPoints();
            if (currentPrayerPoints < 2500) {
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

    public void saveConfiguration() {
        this.configuration.addProperty("useWeaponPoison", String.valueOf(this.useWeaponPoison));
        this.configuration.addProperty("usePrayer", String.valueOf(this.usePrayer));
        this.configuration.addProperty("useEssenceOfFinality", String.valueOf(this.useEssenceOfFinality));
        this.configuration.addProperty("useOverload", String.valueOf(this.useOverload));
        this.configuration.addProperty("useInvokeDeath", String.valueOf(this.useInvokeDeath));
        this.configuration.addProperty("useDeflectMagic", String.valueOf(this.useDeflectMagic));
        this.configuration.addProperty("useSorrow", String.valueOf(this.useSorrow));
        this.configuration.addProperty("useRuination", String.valueOf(this.useRuination));
        this.configuration.addProperty("useDarkness", String.valueOf(this.useDarkness));
        this.configuration.addProperty("usePontifexRing", String.valueOf(this.usePontifexRing));
        this.configuration.addProperty("useMaxGuild", String.valueOf(this.useMaxGuild));
        this.configuration.addProperty("useWarsRetreat", String.valueOf(this.useWarsRetreat));
        this.configuration.addProperty("useBank", String.valueOf(this.useBank));
        this.configuration.addProperty("NecrosisStacksThreshold", String.valueOf(MainScript.NecrosisStacksThreshold));
        this.configuration.save();
    }

    public void loadConfiguration() {
        try {
            this.useWeaponPoison = Boolean.parseBoolean(this.configuration.getProperty("useWeaponPoison"));
            this.usePrayer = Boolean.parseBoolean(this.configuration.getProperty("usePrayer"));
            this.useEssenceOfFinality = Boolean.parseBoolean(this.configuration.getProperty("useEssenceOfFinality"));
            this.useOverload = Boolean.parseBoolean(this.configuration.getProperty("useOverload"));
            this.useInvokeDeath = Boolean.parseBoolean(this.configuration.getProperty("useInvokeDeath"));
            this.useDeflectMagic = Boolean.parseBoolean(this.configuration.getProperty("useDeflectMagic"));
            this.useSorrow = Boolean.parseBoolean(this.configuration.getProperty("useSorrow"));
            this.useRuination = Boolean.parseBoolean(this.configuration.getProperty("useRuination"));
            this.useDarkness = Boolean.parseBoolean(this.configuration.getProperty("useDarkness"));
            this.usePontifexRing = Boolean.parseBoolean(this.configuration.getProperty("usePontifexRing"));
            this.useMaxGuild = Boolean.parseBoolean(this.configuration.getProperty("useMaxGuild"));
            this.useWarsRetreat = Boolean.parseBoolean(this.configuration.getProperty("useWarsRetreat"));
            this.useBank = Boolean.parseBoolean(this.configuration.getProperty("useBank"));
            this.useEssenceOfFinality = Boolean.parseBoolean(this.configuration.getProperty("useEssenceOfFinality"));
            String necrosisThresholdValue = this.configuration.getProperty("NecrosisStacksThreshold");
            if (necrosisThresholdValue != null && !necrosisThresholdValue.isEmpty()) {
                int necrosisThreshold = Integer.parseInt(necrosisThresholdValue);
                if (necrosisThreshold < 0) necrosisThreshold = 0;
                else if (necrosisThreshold > 12) necrosisThreshold = 12;
                MainScript.NecrosisStacksThreshold = necrosisThreshold;
            }
        } catch (NumberFormatException e) {
            println("Error parsing threshold values. Using defaults.");
            MainScript.NecrosisStacksThreshold = 12; // Default or a logical fallback
        }
    }
}
