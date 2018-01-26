package xt9.deepmoblearning.client.gui;

import jline.internal.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import xt9.deepmoblearning.DeepConstants;
import xt9.deepmoblearning.common.energy.DeepEnergyStorage;
import xt9.deepmoblearning.common.inventory.ContainerSimulationChamber;
import xt9.deepmoblearning.common.mobs.MobMetaData;
import xt9.deepmoblearning.common.mobs.MobMetaFactory;
import xt9.deepmoblearning.common.tiles.TileEntitySimulationChamber;
import xt9.deepmoblearning.common.util.Animation;
import xt9.deepmoblearning.common.util.DataModel;
import xt9.deepmoblearning.common.util.MathHelper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by xt9 on 2017-06-17.
 */
public class SimulationChamberGui extends GuiContainer {
    private static final int WIDTH =  232;
    private static final int HEIGHT = 230;

    private HashMap<String, Animation> animationList;
    private ItemStack currentChip = ItemStack.EMPTY;
    private TileEntitySimulationChamber tile;
    private DeepEnergyStorage energyStorage;
    private FontRenderer renderer;
    private World world;

    private static final ResourceLocation base = new ResourceLocation(DeepConstants.MODID, "textures/gui/simulation_chamber_base.png");
    private static final ResourceLocation defaultGui = new ResourceLocation(DeepConstants.MODID, "textures/gui/default_gui.png");

    public SimulationChamberGui(TileEntitySimulationChamber te, InventoryPlayer inventory, World world) {
        super(new ContainerSimulationChamber(te, inventory, world));

        this.energyStorage = (DeepEnergyStorage) te.getCapability(CapabilityEnergy.ENERGY, null);

        this.renderer = Minecraft.getMinecraft().fontRenderer;
        this.animationList = new HashMap<>();
        this.world = world;
        this.tile = te;
        xSize = WIDTH;
        ySize = HEIGHT;
    }

    /* Needed on 1.12 to render tooltips */
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;

        NumberFormat f = NumberFormat.getNumberInstance(Locale.ENGLISH);
        List<String> tooltip = new ArrayList<>();

        if(47 <= y && y < 135) {
            if(13 <= x && x < 22) {
                // Tooltip for Chip exp bar
                if(tile.hasChip()) {
                    if(DataModel.getTier(tile.getChip()) != DeepConstants.MOB_CHIP_MAXIMUM_TIER) {
                        tooltip.add(DataModel.getCurrentTierSimulationCountWithKills(tile.getChip()) + "/" + DataModel.getTierRoof(tile.getChip()) + " Data collected");
                    } else {
                        tooltip.add("This data model has reached the max tier.");
                    }
                } else {
                    tooltip.add("Machine is missing a data model");
                }
                drawHoveringText(tooltip, x + 2, y + 2);
            } else if(211 <= x && x < 220) {
                // Tooltip for energy
                tooltip.add(f.format(energyStorage.getEnergyStored()) + "/" + f.format(energyStorage.getMaxEnergyStored()) + " RF");
                if(tile.hasChip()) {
                    MobMetaData data = MobMetaFactory.createMobMetaData(tile.getChip());
                    tooltip.add("Simulations with current data model drains " + f.format(data.getSimulationTickCost()) + "RF/t");
                }
                drawHoveringText(tooltip, x - 90, y - 16);
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        DecimalFormat f = new DecimalFormat("0.#");
        int left = getGuiLeft() + 8;
        int top = getGuiTop();
        int spacing = 12;
        int topStart = top - 3;
        MobMetaData data = MobMetaFactory.createMobMetaData(tile.getChip());

        if(chipChanged()) {
            resetAnimations();
        }

        // Draw the main GUI
        Minecraft.getMinecraft().getTextureManager().bindTexture(base);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(left, top, 0, 0, 216, 141);

        // Draw chip slot
        drawTexturedModalRect(left - 22, top, 0, 141, 18, 18);

        // Draw current energy
        int energyBarHeight = MathHelper.ensureRange((int) ((float) energyStorage.getEnergyStored() / (energyStorage.getMaxEnergyStored() - data.getSimulationTickCost()) * 87), 0, 87);
        int energyBarOffset = 87 - energyBarHeight;
        drawTexturedModalRect(left + 203,  top + 48 + energyBarOffset, 25, 141, 7, energyBarHeight);


        String[] lines;

        if(!tile.hasChip()) {
            lines = new String[] {"Please insert a data model", "to begin the simulation"};

            Animation a1 = getAnimation("pleaseInsert1");
            Animation a2 = getAnimation("pleaseInsert2");

            animateString(lines[0], a1, null, 5, false, left + 10, topStart + spacing, 16777215);
            animateString(lines[1], a2, a1, 5, false, left + 10, topStart + (spacing * 2), 16777215);

        } else if(DataModel.getTier(tile.getChip()) == 0) {

            lines = new String[] {"Insufficient data in model", "please insert a basic model", "or better "};

            Animation insufData = getAnimation("insufData1");
            Animation insufData2 = getAnimation("insufData2");
            Animation insufData3 = getAnimation("insufData3");

            animateString(lines[0], insufData, null, 5, false, left + 10, topStart + spacing, 16777215);
            animateString(lines[1], insufData2, insufData, 5, false,  left + 10, topStart + (spacing * 2), 16777215);
            animateString(lines[2], insufData3, insufData2, 5, false,  left + 10, topStart + (spacing * 3), 16777215);

        } else {
            // Draw current chip experience
            if(DataModel.getTier(tile.getChip()) == DeepConstants.MOB_CHIP_MAXIMUM_TIER) {
                drawTexturedModalRect(left + 6,  top + 48, 18, 141, 7, 87);
            } else {
                int collectedData = DataModel.getCurrentTierSimulationCountWithKills(tile.getChip());
                int tierRoof = DataModel.getTierRoof(tile.getChip());

                int experienceBarHeight = (int) (((float) collectedData / tierRoof * 87));
                int experienceBarOffset = 87 - experienceBarHeight;
                drawTexturedModalRect(left + 6,  top + 48 + experienceBarOffset, 18, 141, 7, experienceBarHeight);
            }

            drawString(renderer, "Tier: " + DataModel.getTierName(tile.getChip(), false), left + 10, topStart + spacing, 16777215);
            drawString(renderer, "Iterations: " + f.format(DataModel.getTotalSimulationCount(tile.getChip())), left + 10, topStart + spacing * 2, 16777215);
            drawString(renderer, "Pristine chance: " + DataModel.getPristineChance(tile.getChip()) + "%", left + 10, topStart + spacing * 3, 16777215);
        }

        // Draw player inventory
        Minecraft.getMinecraft().getTextureManager().bindTexture(defaultGui);
        drawTexturedModalRect(left + 20, top + 145, 0, 0, 176, 90);


        drawConsoleText(left, top, spacing);
    }


    private void drawConsoleText(int left, int top, int spacing) {
        String[] lines;

        if(!tile.hasChip() || DataModel.getTier(tile.getChip()) == 0) {
            animateString("_", getAnimation("blinkingUnderline"), null, 100, true, left + 21, top + 49, 16777215);

        } else if(!tile.hasPolymerClay()) {
            lines = new String[] {"Cannot begin simulation", "Missing polymer medium", "_"};
            Animation a1 = getAnimation("inputSlotEmpty1");
            Animation a2 = getAnimation("inputSlotEmpty2");
            Animation a3 = getAnimation("blinkingUnderline1");

            animateString(lines[0], a1, null, 5, false, left + 21, top + 51, 16777215);
            animateString(lines[1], a2, a1, 5, false, left + 21, top + 51 + spacing, 16777215);
            animateString(lines[2], a3, a2, 100, true, left + 21, top + 51 + (spacing * 2), 16777215);

        } else if(!hasEnergy() && !tile.isCrafting) {
            lines = new String[] {"Cannot begin simulation", "System energy levels critical", "_"};
            Animation a1 = getAnimation("lowEnergy1");
            Animation a2 = getAnimation("lowEnergy2");
            Animation a3 = getAnimation("blinkingUnderline2");

            animateString(lines[0], a1, null, 5, false, left + 21, top + 51, 16777215);
            animateString(lines[1], a2, a1, 5, false, left + 21, top + 51 + spacing, 16777215);
            animateString(lines[2], a3, a2, 100, true, left + 21, top + 51 + (spacing * 2), 16777215);
        } else if(tile.outputIsFull() || tile.pristineIsFull()) {
            lines = new String[] {"Cannot begin simulation", "Output or pristine buffer is full", "_"};
            Animation a1 = getAnimation("outputSlotFilled1");
            Animation a2 = getAnimation("outputSlotFilled2");
            Animation a3 = getAnimation("blinkingUnderline3");

            animateString(lines[0], a1, null, 5, false, left + 21, top + 51, 16777215);
            animateString(lines[1], a2, a1, 5, false, left + 21, top + 51 + spacing, 16777215);
            animateString(lines[2], a3, a2, 100, true, left + 21, top + 51 + (spacing * 2), 16777215);
        } else if(tile.isCrafting) {
            drawString(renderer, tile.percentDone + "%", left + 176, top + 123, 6478079);

            drawString(renderer, tile.getSimulationText("simulationProgressLine1"), left + 21, top + 51, 16777215);
            drawString(renderer, tile.getSimulationText("simulationProgressLine1Version"), left + 124, top + 51, 16777215);

            drawString(renderer, tile.getSimulationText("simulationProgressLine2"), left + 21, top + 51 + spacing, 16777215);

            drawString(renderer, tile.getSimulationText("simulationProgressLine3"), left + 21, top + 51 + (spacing * 2), 16777215);
            drawString(renderer, tile.getSimulationText("simulationProgressLine4"), left + 21, top + 51 + (spacing * 3), 16777215);
            drawString(renderer, tile.getSimulationText("simulationProgressLine5"), left + 21, top + 51 + (spacing * 4), 16777215);

            drawString(renderer, tile.getSimulationText("simulationProgressLine6"), left + 21, top + 51 + (spacing * 5), 16777215);
            drawString(renderer, tile.getSimulationText("simulationProgressLine6Result"), left + 140, top + 51 + (spacing * 5), 16777215);

            drawString(renderer, tile.getSimulationText("simulationProgressLine7"), left + 21, top + 51 + (spacing * 6), 16777215);
            drawString(renderer, tile.getSimulationText("blinkingDots1"), left + 128, top + 51 + (spacing * 6), 16777215);
        } else {
            animateString("_", getAnimation("blinkingUnderline"), null, 250, true, left + 21, top + 49, 16777215);
        }
    }

    private boolean hasEnergy() {
        return tile.hasEnergyForSimulation();
    }

    private boolean chipChanged() {
        if(ItemStack.areItemStacksEqual(currentChip, tile.getChip())) {
            return false;
        } else {
            this.currentChip = tile.getChip();
            return true;
        }
    }

    private void resetAnimations() {
        this.animationList = new HashMap<>();
    }

    private Animation getAnimation(String key) {
        if(animationList.containsKey(key)) {
            return animationList.get(key);
        } else {
            animationList.put(key, new Animation());
            return animationList.get(key);
        }
    }

    private void animateString(String string, Animation anim, @Nullable Animation precedingAnim, int delay, boolean loop, int left, int top, int color) {
        if(precedingAnim != null) {
            if (precedingAnim.hasFinished()) {
                String result = anim.animate(string, delay, loop);
                drawString(renderer, result, left, top, color);
            } else {
                return;
            }
        }
        String result = anim.animate(string, delay, loop);
        drawString(renderer, result, left, top, color);
    }
}
