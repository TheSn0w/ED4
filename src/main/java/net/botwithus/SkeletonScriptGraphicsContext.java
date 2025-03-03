package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

public class SkeletonScriptGraphicsContext extends ScriptGraphicsContext {

    private ED4 script;

    public SkeletonScriptGraphicsContext(ScriptConsole scriptConsole, ED4 script) {
        super(scriptConsole);
        this.script = script;
    }

    private int startingDungTokens = -1; // Initialized to -1 to indicate it's unset
    private int CurrentDungTokens;
    private int DifferenceDungTokens;
    boolean isScriptRunning = false;
    private String saveSettingsFeedbackMessage = "";

    private static float RGBToFloat(int rgbValue) {
        return rgbValue / 255.0f;
    }

    @Override
    public void drawSettings() {
        ImGui.PushStyleColor(0, RGBToFloat(173), RGBToFloat(216), RGBToFloat(230), 1.0f); // Button color
        ImGui.PushStyleColor(21, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); // Button color
        ImGui.PushStyleColor(18, RGBToFloat(173), RGBToFloat(216), RGBToFloat(230), 0.5f); // Checkbox Tick color
        ImGui.PushStyleColor(5, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); // Border Colour
        ImGui.PushStyleColor(2, RGBToFloat(0), RGBToFloat(0), RGBToFloat(0), 0.9f); // Background color
        ImGui.PushStyleColor(7, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); // Checkbox Background color
        ImGui.PushStyleColor(11, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); // Header Colour
        ImGui.PushStyleColor(22, RGBToFloat(173), RGBToFloat(216), RGBToFloat(230), 0.5f); // Highlighted button color
        ImGui.PushStyleColor(13, RGBToFloat(255), RGBToFloat(255), RGBToFloat(255), 0.5f); // Highlighted button color
        ImGui.PushStyleColor(27, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); //ImGUI separator Colour
        ImGui.PushStyleColor(30, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); //Corner Extender colour
        ImGui.PushStyleColor(31, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); //Corner Extender colour
        ImGui.PushStyleColor(32, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); //Corner Extender colour
        ImGui.PushStyleColor(33, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); //Corner Extender colour
        ImGui.PushStyleColor(3, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 0.5f); //ChildBackground


        ImGui.SetWindowSize(200.f, 200.f);
        if (ImGui.Begin("Snows Token Farmer", ImGuiWindowFlag.None.getValue())) {
            ImGui.PushStyleVar(1, 10.f, 5f);
            ImGui.PushStyleVar(2, 10.f, 5f); //spacing between side of window and checkbox
            ImGui.PushStyleVar(3, 10.f, 5f);
            ImGui.PushStyleVar(4, 10.f, 10f);
            ImGui.PushStyleVar(5, 10.f, 5f);
            ImGui.PushStyleVar(6, 10.f, 5f);
            ImGui.PushStyleVar(7, 10.f, 5f);
            ImGui.PushStyleVar(8, 10.f, 5f); //spacing between seperator and text
            ImGui.PushStyleVar(9, 10.f, 5f);
            ImGui.PushStyleVar(10, 10.f, 5f);
            ImGui.PushStyleVar(11, 10.f, 5f); // button sizes
            ImGui.PushStyleVar(12, 10.f, 5f);
            ImGui.PushStyleVar(13, 10.f, 5f);
            ImGui.PushStyleVar(14, 10.f, 5f); // spaces between options ontop such as overlays, debug etc
            ImGui.PushStyleVar(15, 10.f, 5f); // spacing between Text/tabs and checkboxes
            ImGui.PushStyleVar(16, 10.f, 5f);
            ImGui.PushStyleVar(17, 10.f, 5f);
            if (isScriptRunning) {
                if (ImGui.Button("Stop Script")) {
                    script.stopScript();
                    isScriptRunning = false;
                }
            } else {
                if (ImGui.Button("Start Script")) {
                    script.startScript();
                    isScriptRunning = true;
                }
            }
            ImGui.SameLine();
            if (ImGui.Button("Save Settings")) {
                try {
                    script.saveConfiguration();
                    saveSettingsFeedbackMessage = "Settings saved successfully.";
                } catch (Exception e) {
                    saveSettingsFeedbackMessage = "Failed to save settings: " + e.getMessage();
                }
            }

            if (!saveSettingsFeedbackMessage.isEmpty()) {
                ImGui.Text(saveSettingsFeedbackMessage);
            }
            script.useWarsRetreat = ImGui.Checkbox("Use Wars Retreat", script.useWarsRetreat);
            ImGui.SameLine();
            script.useBank = ImGui.Checkbox("Use Bank", script.useBank);
            script.useMaxGuild = ImGui.Checkbox("Use Max Guild", script.useMaxGuild);
            script.useShadowReefTeleport = ImGui.Checkbox("Use Shadow Reef Teleport", script.useShadowReefTeleport);

            if (script.useMaxGuild) {
                script.useWarsRetreat = false;
                script.useBank = false;
                script.useShadowReefTeleport = false;
            }


                ImGui.SeparatorText("INSTRUCTIONS");
                ImGui.Text("1) Choose teleport option from above, ALL SUPPORT BANKING when low on supplies`");
                ImGui.Text("2) Choose Combat options");
                ImGui.Text("3) The use Bank toggle only works with wars retreat, it constantly banks after every kill (disable)");
                ImGui.Text("4) Shadow reef teleport required ring of kinship in inventory, it will buy tokens when you run out");
                ImGui.Text("5) Kill mages will kill the 4 mages before teleporting out (enabled by necessity on max guild)");
                ImGui.Text("6) you can use Falador teleport tablets if you dont have the above, just leave blank");
                ImGui.Separator();
                script.killMages = ImGui.Checkbox("Kill Mages then teleport?", script.killMages);
                script.useSmokeCloud = ImGui.Checkbox("Use Smoke Cloud", script.useSmokeCloud);
                script.useJas = ImGui.Checkbox("Use Jas Book", script.useJas);
                script.useWen = ImGui.Checkbox("Use Wen Book", script.useWen);
                script.useQuickPrayers = ImGui.Checkbox("Use Quick Prayers", script.useQuickPrayers);
                script.useFamiliar = ImGui.Checkbox("Use Familiar", script.useFamiliar);
                script.useOverloads = ImGui.Checkbox("Use Overload", script.useOverloads);
                script.useInvokeDeath = ImGui.Checkbox("Use Invoke Death", script.useInvokeDeath);
                script.usePrayerPots = ImGui.Checkbox("Use Prayer Potions", script.usePrayerPots);
                script.useEssenceOfFinality = ImGui.Checkbox("Use Essence of Finality", script.useEssenceOfFinality);
                if (ImGui.IsItemHovered()) {
                    ImGui.SetTooltip("Do not have Finger of Death in Revo bar.");
                }
                ImGui.SetItemWidth(110.0F);
                ImGui.SameLine();
                ED4.NecrosisStacksThreshold = ImGui.InputInt("Necrosis Stacks Threshold (0-12)", ED4.NecrosisStacksThreshold);
                if (ImGui.IsItemHovered()) {
                    ImGui.SetTooltip("Stacks to cast at");
                }
                if (ED4.NecrosisStacksThreshold < 0) {
                    ED4.NecrosisStacksThreshold = 0;
                } else if (ED4.NecrosisStacksThreshold > 12) {
                    ED4.NecrosisStacksThreshold = 12;
                }
                ImGui.Text("My scripts state is: " + script.getBotState());
                ImGui.SeparatorText("Quick Stats");
                updateAndDisplayDungTokens(script);
                ImGui.PopStyleVar(3);

            }

        ImGui.PopStyleColor(100);
        ImGui.PopStyleVar(100);
        ImGui.End();
        }

    private String timeRunningFormatted() {
        long timeRunning = System.currentTimeMillis() - script.runStartTime;
        long seconds = timeRunning / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return hours + "h " + minutes % 60 + "m " + seconds % 60 + "s";
    }
    public void updateAndDisplayDungTokens(ED4 script) {
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
