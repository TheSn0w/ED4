package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

public class SkeletonScriptGraphicsContext extends ScriptGraphicsContext {

    private MainScript script;

    public SkeletonScriptGraphicsContext(ScriptConsole scriptConsole, MainScript script) {
        super(scriptConsole);
        this.script = script;
    }
    private int startingDungTokens = -1; // Initialized to -1 to indicate it's unset
    private int CurrentDungTokens;
    private int DifferenceDungTokens;
    private static float RGBToFloat(int rgbValue) {
        return rgbValue / 255.0f;
    }

    @Override
    public void drawSettings() {
        ImGui.PushStyleColor(21, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Button color
        ImGui.PushStyleColor(18, RGBToFloat(255), RGBToFloat(255), RGBToFloat(255), 1.0f); // Checkbox Tick color
        ImGui.PushStyleColor(5, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Border Colour
        ImGui.PushStyleColor(2, RGBToFloat(0), RGBToFloat(0), RGBToFloat(0), 0.9f); // Background color
        ImGui.PushStyleColor(7, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Checkbox Background color
        ImGui.PushStyleColor(11, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Header Colour
        ImGui.PushStyleColor(22, RGBToFloat(64), RGBToFloat(67), RGBToFloat(67), 1.0f); // Highlighted button color
        ImGui.PushStyleColor(27, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //ImGUI separator Colour
        ImGui.PushStyleColor(30, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour
        if (ImGui.Begin("ED4 Bot", ImGuiWindowFlag.None.getValue())) {
            ImGui.PushStyleVar(11, 50.f, 5f);
            if (ImGui.BeginTabBar("My bar", ImGuiWindowFlag.None.getValue())) {
                if (ImGui.BeginTabItem("Settings", ImGuiWindowFlag.None.getValue())) {
                    script.runScript = ImGui.Checkbox("Run script", script.runScript);
                    script.useWarsRetreat = ImGui.Checkbox("Use Wars Retreat", script.useWarsRetreat);
                    script.useBank = ImGui.Checkbox("Use Bank", script.useBank);
                    /*if(!script.useMaxGuild && !script.usePontifexRing)
                    {
                        script.useWarsRetreat = ImGui.Checkbox("Use Wars Retreat", script.useWarsRetreat);
                        if (script.useWarsRetreat) {
                            script.useMaxGuild = false;
                            script.usePontifexRing = false;
                        }
                    }
                    ImGui.SameLine();
                    if(!script.useWarsRetreat && !script.usePontifexRing)
                    {
                        script.useMaxGuild = ImGui.Checkbox("Use Max Guild", script.useMaxGuild);
                        if (script.useMaxGuild) {
                            script.useWarsRetreat = false;
                            script.usePontifexRing = false;
                        }
                    }
                    ImGui.SameLine();
                    if(!script.useMaxGuild && !script.useWarsRetreat)
                    {
                        script.usePontifexRing = ImGui.Checkbox("Use Pontifex Ring", script.usePontifexRing);
                        if (script.usePontifexRing) {
                            script.useMaxGuild = false;
                            script.useWarsRetreat = false;
                        }
                    }
                    script.useDarkness = ImGui.Checkbox("Use Darkness", script.useDarkness);
                    script.useOverload = ImGui.Checkbox("Use Overload", script.useOverload);
                    script.usePrayerOrRestorePots = ImGui.Checkbox("Use Prayer Potions", script.usePrayerOrRestorePots);*/
                    script.useDeflectMagic = ImGui.Checkbox("Use Deflect Magic", script.useDeflectMagic);
                    /*script.useSorrow = ImGui.Checkbox("Use Sorrow", script.useSorrow);*/
                    script.useRuination = ImGui.Checkbox("Use Ruination", script.useRuination);
                    script.useOverload = ImGui.Checkbox("Use Overload", script.useOverload);
                    script.useWeaponPoison = ImGui.Checkbox("Use Weapon Poison", script.useWeaponPoison);
                    script.useInvokeDeath = ImGui.Checkbox("Use Invoke Death", script.useInvokeDeath);
                    ImGui.Text("My scripts state is: " + script.getBotState());
                    /*if(ImGui.Button("Set State past portal (DEBUG)"))
                    {
                        script.setBotState(MainScript.BotState.ATED4);
                    }
                    if(ImGui.Button("Set State to Idle (DEBUG)"))
                    {
                        script.setBotState(MainScript.BotState.IDLE);
                    }*/
                    ImGui.SeparatorText("Quick Stats");
                    updateAndDisplayDungTokens(script);
                    ImGui.PopStyleVar(3);
                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("Statistics", ImGuiWindowFlag.None.getValue())) {

                    ImGui.Text("Total tokens: " + script.totalTokens);
                    ImGui.Text("Total runs: " + script.runCount);
                    ImGui.Text("Time running: " + timeRunningFormatted());
                    ImGui.EndTabItem();
                }
                ImGui.EndTabBar();
            }
            ImGui.PopStyleColor(100);
            ImGui.End();
        }

    }

    private String timeRunningFormatted() {
        long timeRunning = System.currentTimeMillis() - script.runStartTime;
        long seconds = timeRunning / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return hours + "h " + minutes % 60 + "m " + seconds % 60 + "s";
    }
    public void updateAndDisplayDungTokens(MainScript script) {
        if (startingDungTokens == -1) {
            startingDungTokens = script.getCurrentDungTokens(); // Initialize starting value
        }

        CurrentDungTokens = script.getCurrentDungTokens(); // Always fetch the latest value
        DifferenceDungTokens = CurrentDungTokens - startingDungTokens; // Calculate difference

        double hoursElapsed = getElapsedTimeHours(script.runStartTime);
        long tokensPerHour = 0;
        if (hoursElapsed > 0) { // Avoid division by zero
            tokensPerHour = (long) (DifferenceDungTokens / hoursElapsed); // Calculate tokens earned per hour
        }

        // Displaying the information
        ImGui.Text("Starting Tokens: " + startingDungTokens);
        ImGui.Text("Current Amount: " + CurrentDungTokens);
        ImGui.Text("Tokens earned: " + DifferenceDungTokens);
        ImGui.SeparatorText("Approx. Tokens/Hour: " + tokensPerHour);
    }
    private double getElapsedTimeHours(long startTime) {
        long timeRunning = System.currentTimeMillis() - startTime; // Elapsed time in milliseconds
        return timeRunning / 3600000.0; // Convert milliseconds to hours
    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}
