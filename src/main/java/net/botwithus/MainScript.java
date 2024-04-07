package net.botwithus;

import net.botwithus.api.game.hud.Dialog;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.skills.Skill;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.util.RandomGenerator;

import static net.botwithus.rs3.game.Client.getLocalPlayer;

public class MainScript extends LoopingScript {

    public boolean runScript;


    public int CoordX;
    public int CoordY;
    private BotState botState;
    boolean usePortal = false;
    boolean usePontifexRing = false;
    boolean useMaxGuild = false;

    int runCount = 0;
    long runStartTime;
    int totalTokens = 0;
    public boolean useWarsRetreat;
    public boolean useDarkness;
    boolean useOverload;
    public boolean usePrayerOrRestorePots;
    public boolean useDeflectMagic;
    public boolean useSorrow;


    enum BotState {
        WALKTOPORTAL, PRAYING, BANKING, ATED4, WALKTOZAMMY, INSIDE_ZAMMY, ACCEPT_DIALOG, FirstWalk, IDLE
    }

    public MainScript(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new SkeletonScriptGraphicsContext(getConsole(), this);
        botState = BotState.IDLE;
        this.runStartTime = System.currentTimeMillis();
    }

    @Override
    public void onLoop() {
        if(!runScript) {
            return;
        }
        switch (botState) {
            case IDLE -> {
                //dont need to log first one so maybe wait till runcount == 1 before adding tokens
                runCount++;
                totalTokens += 5000; // Increment total tokens by 5000 for each run
                if(useMaxGuild) {
                    useMaxGuildTeleport();
                }
                else if(useWarsRetreat || usePontifexRing){
                    useWarsTeleport();
                }
            }
            case BANKING -> {
                if(useMaxGuild)
                {
                    LoadMaxGuildPresetLogic();
                }
                else if(useWarsRetreat || usePontifexRing)
                {
                    LoadPresetLogic();
                }

            }
            case PRAYING -> {
                if(useMaxGuild)
                {
                    admireThroneofFame();
                }
                else if(useWarsRetreat || usePontifexRing)
                {
                    useAltarofWar();
                }
            }
            case WALKTOPORTAL -> {
                if(useMaxGuild)
                {
                    walkToPortalMaxGuild();
                }
                else if(useWarsRetreat)
                {
                    walkToPortal();
                }
                else if (usePontifexRing)
                {
                    usePontifex();
                }
            }
            case WALKTOZAMMY -> {
                moveToSpecificLocation();
            }
            case ATED4 -> {
                enterZammy();
            }
            case ACCEPT_DIALOG -> {
                acceptDialog();
            }
            case FirstWalk -> {
                FirstWalk();
            }
            case INSIDE_ZAMMY -> {
                attackMiniBoss();
            }


        }
    }

    private void walkToPortal() {
        if (getLocalPlayer() != null) {
            if (!getLocalPlayer().isMoving()) {
                EntityResultSet<SceneObject> sceneObjectQuery = SceneObjectQuery.newQuery().name("Portal (The Zamorakian Undercity)").results();
                if(!sceneObjectQuery.isEmpty())
                {
                    SceneObject portal = sceneObjectQuery.nearest();
                    portal.interact("Enter");
                    botState = BotState.ATED4;
                }
            }
        }
    }

    private void walkToPortalMaxGuild() {
        if (getLocalPlayer() != null) {
            if (!getLocalPlayer().isMoving()) {
                EntityResultSet<SceneObject> sceneObjectQuery = SceneObjectQuery.newQuery().name("The Zamorakian Undercity portal").results();
                if(!sceneObjectQuery.isEmpty())
                {
                    SceneObject portal = sceneObjectQuery.nearest();
                    portal.interact("Enter");
                    botState = BotState.ATED4;
                }
            }
        }
    }

    public void enterZammy(){
        if(getLocalPlayer() != null)
        {
            if (!getLocalPlayer().isMoving()) {
                EntityResultSet<SceneObject> sceneObjectQuery = SceneObjectQuery.newQuery().name("The Zamorakian Undercity").results();
                if(!sceneObjectQuery.isEmpty())
                {
                    SceneObject portal = sceneObjectQuery.nearest();
                    portal.interact("Enter");
                    botState = BotState.ACCEPT_DIALOG;
                }
            }
        }
    }

    private void useAltarofWar() {
        if(WalkTo(3303, 10127)) {
            EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Altar of War").results();
            if (!query.isEmpty()) {
                SceneObject altar = query.nearest();
                altar.interact("Pray");
                Execution.delay(RandomGenerator.nextInt(2000,3000));
                botState = BotState.WALKTOPORTAL;
            }
        }
    }

    private void LoadPresetLogic() {
        if(WalkTo(3299, 10131)) {
            EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Bank chest").results();
            if (!query.isEmpty()) {
                println("Loading preset!");
                SceneObject bankChest = query.nearest();
                bankChest.interact("Load Last Preset from");
                //wait at bank if we aren't full health
                if(getLocalPlayer().getCurrentHealth() < getLocalPlayer().getMaximumHealth())
                {
                    println("Healing up!");
                    Execution.delay(RandomGenerator.nextInt(600,1000));
                }else {
                    botState = BotState.PRAYING;
                }
            }
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

        if(WalkTo(2276, 3311)) {
            if(isAtMaxGuildBank())
            {
            EntityResultSet<Npc> query = NpcQuery.newQuery().name("Banker").results();
            if (!query.isEmpty()) {
                println("Loading preset!");
                Npc bankChest = query.nearest();
                if(bankChest != null)
                {
                        bankChest.interact("Load Last Preset from");
                }
                Execution.delay(RandomGenerator.nextInt(1600,2600));
                botState = BotState.PRAYING;

                //wait at bank if we aren't full health
                if(getLocalPlayer().getCurrentHealth() < getLocalPlayer().getMaximumHealth())
                {
                    println("Healing up!");
                    Execution.delay(RandomGenerator.nextInt(600,1000));
                }

            }
        }
        }
    }

    public void admireThroneofFame()
    {
        if(WalkTo(2276, 3306))
        {
            EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Throne of Fame").results();
            if (!query.isEmpty()) {
                SceneObject throne = query.nearest();
                throne.interact("Admire");
                Execution.delay(RandomGenerator.nextInt(600,1000));

            }
            botState = BotState.WALKTOPORTAL;
        }
    }

    public void usePontifex()
    {
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
        if(isAtPontiFexTele()) {
            if (currentLocation.equals(new Coordinate(1759, 1261, 0))) {
                Travel.walkTo(new Coordinate(1759, 1292, 0));
                Execution.delayUntil((30000), () -> Distance.between(getLocalPlayer().getCoordinate(), new Coordinate(1759, 1292, 0)) <= 5);

                Travel.walkTo(new Coordinate(1747, 1313, 0));
                Execution.delayUntil((30000), () -> Distance.between(getLocalPlayer().getCoordinate(), new Coordinate(1747, 1313, 0)) <= 5);

                Travel.walkTo(new Coordinate(1760, 1341, 0));
            } else {
                println("Player is not at the starting location.");
            }
            botState = BotState.ATED4;
        }
    }
    public void FirstWalk() {
        if(WalkTo(CoordX, CoordY)) {
            botState = BotState.INSIDE_ZAMMY;
            println("Walking to boss!");
        }
    }

    public void attackMiniBoss(){
        if (WalkTo(getLocalPlayer().getCoordinate().getX() + 25, getLocalPlayer().getCoordinate().getY())) {
            if (getLocalPlayer() != null) {
                if (!getLocalPlayer().isMoving()) {
                    EntityResultSet<Npc> npcQuery = NpcQuery.newQuery().name("Cerberus Juvenile").results();
                    if (!npcQuery.isEmpty()) {
                        Npc boss = npcQuery.nearest();
                        if(getLocalPlayer().getTarget() == null && boss.validate()) {
                            boss.interact("Attack");
                            println("Attacking boss!");
                            Execution.delayUntil(RandomGenerator.nextInt(600, 1000), () -> !boss.validate());
                        }
                        eatFood();
                        //
                        if(useDarkness)
                        {
                            useDarkness();
                            Execution.delay(RandomGenerator.nextInt(20, 80));
                        }
                        if(useOverload)
                        {
                            drinkOverloads();
                            Execution.delay(RandomGenerator.nextInt(20, 80));
                        }
                        if(usePrayerOrRestorePots)
                        {
                            if(getLocalPlayer().getPrayerPoints() < 4000)
                            {
                                usePrayerOrRestorePots();
                                Execution.delay(RandomGenerator.nextInt(20, 80));
                            }
                        }
                        if(VarManager.getVarbitValue(16768) == 0 && useDeflectMagic)
                        {
                            ActionBar.usePrayer("Deflect Magic");
                            Execution.delay(RandomGenerator.nextInt(10, 20));
                        }

                    } else {
                        if(VarManager.getVarbitValue(16768) == 1 && useDeflectMagic)
                        {
                            ActionBar.usePrayer("Deflect Magic");
                            Execution.delay(RandomGenerator.nextInt(10, 20));
                        }
                        botState = BotState.IDLE;

                        println("No boss found!");
                    }
                }
            }
        }
    }
    public void usePrayerOrRestorePots() {
        Skill prayerSkill = Skills.PRAYER.getSkill();
        int currentPrayerLevel = prayerSkill.getLevel();
        int maxPrayerLevel = prayerSkill.getMaxLevel();


        int currentPrayerPoints = currentPrayerLevel * 100;
        int maxPrayerPoints = maxPrayerLevel * 100;


        if (currentPrayerPoints < maxPrayerPoints * 0.3) {
            ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();

            Item prayerOrRestorePot = items.stream()
                    .filter(item -> item.getName() != null && (item.getName().toLowerCase().contains("prayer") || item.getName().toLowerCase().contains("restore")))
                    .findFirst()
                    .orElse(null);

            if (prayerOrRestorePot != null) {
                println("Drinking " + prayerOrRestorePot.getName());
                boolean success = Backpack.interact(prayerOrRestorePot.getName(), 1);
                Execution.delay(RandomGenerator.nextInt(1600, 2100));

                if (!success) {
                    println("Failed to use " + prayerOrRestorePot.getName());
                }
            } else {
                println("No Prayer or Restore pots found.");
            }
        } else {
            println("Current Prayer points are above 30% of maximum. No need to use Prayer or Restore pot.");
        }
    }

    public void drinkOverloads() {
        if (getLocalPlayer() != null) {
            if (!getLocalPlayer().isMoving()) {
                if (VarManager.getVarbitValue(26037) == 0) {
                    if(Client.getLocalPlayer().getAnimationId() == 18000)
                        return;

                    ResultSet<Item> overload = InventoryItemQuery.newQuery().name("overload", String::contains).results();
                    if (!overload.isEmpty()) {
                        Item overloadItem = overload.first();
                        Backpack.interact(overloadItem.getName(), "Drink");
                        println("Drinking overload " + overloadItem.getName() + "ID: "+overloadItem.getId());
                        Execution.delay(RandomGenerator.nextInt(10, 20));
                    }
                }
            }
        }
    }

    public boolean isDarknessActive()
    {
        Component darkness = ComponentQuery.newQuery(284).spriteId(30122).results().first();
        return darkness != null;
    }

    public void useDarkness(){
        if(getLocalPlayer() != null) {
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
                        println(option);
                        if (option == null)
                            return;

                        if(option.contains("No"))
                        {
                            Execution.delay(RandomGenerator.nextInt(200, 300));
                            Dialog.interact(option);
                            println("Accepting first dialog");
                        }



                        if (option.contains("Normal mode")) {
                            Execution.delay(RandomGenerator.nextInt(500, 550));
                            Dialog.interact(option);
                            println("Accepting dialog!");
                            botState = BotState.FirstWalk;
                            Execution.delay(RandomGenerator.nextInt(500, 1500));
                            CoordX = getLocalPlayer().getCoordinate().getX() + 5;
                            CoordY = getLocalPlayer().getCoordinate().getY() + 28;
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
            ActionBar.useAbility("War's Retreat Teleport");
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
            if(myPos.getX() != x && myPos.getY() != y) {

                if (!getLocalPlayer().isMoving()) {
                    println("Walking to: " + x + ", " + y);
                    Travel.walkTo(x, y);
                    Execution.delay(RandomGenerator.nextInt(300, 500));
                }
                return false;
            }else {
                return true;
            }
        }
        return false;
    }

}
//test
