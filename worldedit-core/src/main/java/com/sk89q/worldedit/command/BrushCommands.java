/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.ButcherBrush;
import com.sk89q.worldedit.command.tool.brush.ClipboardBrush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.SmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Commands to set brush shape.
 */
public class BrushCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public BrushCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        aliases = { "sphere", "s" },
        usage = "<шаблон> [радиус]",
        flags = "h",
        desc = "Выбрать сферичную кисть с радиусом",
        help =
            "Выбрать сферичную кисть с радиусом.\n" +
            "Флаг -h создает полые сферы.",
        min = 1,
        max = 2
    )
    @CommandPermissions("worldedit.brush.sphere")
    public void sphereBrush(Player player, LocalSession session, Pattern fill,
                            @Optional("2") double radius, @Switch('h') boolean hollow) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setFill(fill);
        tool.setSize(radius);

        if (hollow) {
            tool.setBrush(new HollowSphereBrush(), "worldedit.brush.sphere");
        } else {
            tool.setBrush(new SphereBrush(), "worldedit.brush.sphere");
        }

        player.print(String.format("Форма сферичной кисти сформирована (%.0f).", radius));
    }

    @Command(
        aliases = { "cylinder", "cyl", "c" },
        usage = "<блок> [радиус] [высота]",
        flags = "h",
        desc = "Выбрать цилиндрическую кисть",
        help =
            "Выбрать цилиндрическую кисть.\n" +
            "Флаг -h создает полые цилиндра.",
        min = 1,
        max = 3
    )
    @CommandPermissions("worldedit.brush.cylinder")
    public void cylinderBrush(Player player, LocalSession session, Pattern fill,
                              @Optional("2") double radius, @Optional("1") int height, @Switch('h') boolean hollow) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(height);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setFill(fill);
        tool.setSize(radius);

        if (hollow) {
            tool.setBrush(new HollowCylinderBrush(height), "worldedit.brush.cylinder");
        } else {
            tool.setBrush(new CylinderBrush(height), "worldedit.brush.cylinder");
        }

        player.print(String.format("Форма цилиндрической кисти сформирована (%.0f к %d).", radius, height));
    }

    @Command(
        aliases = { "clipboard", "copy" },
        usage = "",
        desc = "Выбрать кисть из буфера обмена",
        help =
            "Выбрать кисть из буфера обмена.\n" +
            "-a чтобы пропустить блоки воздуха.\n" +
            "Без флага -p ,вставка будет отображаться по центру в целевом месте. " +
            "С флагом, вставка будет появляться относительно того, где вы были " +
            "по отношению к скопированной области при копировании его."
    )
    @CommandPermissions("worldedit.brush.clipboard")
    public void clipboardBrush(Player player, LocalSession session, @Switch('a') boolean ignoreAir, @Switch('p') boolean usingOrigin) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();

        Vector size = clipboard.getDimensions();

        worldEdit.checkMaxBrushRadius(size.getBlockX());
        worldEdit.checkMaxBrushRadius(size.getBlockY());
        worldEdit.checkMaxBrushRadius(size.getBlockZ());

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setBrush(new ClipboardBrush(holder, ignoreAir, usingOrigin), "worldedit.brush.clipboard");

        player.print("Форма кисти сформирвана.");
    }

    @Command(
        aliases = { "smooth" },
        usage = "[размер] [повторение]",
        flags = "n",
        desc = "Выбрать кисть сглаживания поверхности",
        help =
            "Выбрать кисть сглаживания поверхности.\n" +
            "-n чтобы сглаживать только натуральные (природные) структуры.",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.brush.smooth")
    public void smoothBrush(Player player, LocalSession session,
                            @Optional("2") double radius, @Optional("4") int iterations, @Switch('n')
                            boolean naturalBlocksOnly) throws WorldEditException {

        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new SmoothBrush(iterations, naturalBlocksOnly), "worldedit.brush.smooth");

        player.print(String.format("Сглаживательная кисть сформирована (%.0f x %dx, используя " + (naturalBlocksOnly ? "только натуральные блоки" : "любой блок") + ").",
                radius, iterations));
    }

    @Command(
        aliases = { "ex", "extinguish" },
        usage = "[радиус]",
        desc = "Выбрать огнетушительую кисть",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.brush.ex")
    public void extinguishBrush(Player player, LocalSession session, EditSession editSession, @Optional("5") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        Pattern fill = new BlockPattern(new BaseBlock(0));
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setMask(new BlockMask(editSession, new BaseBlock(BlockID.FIRE)));
        tool.setBrush(new SphereBrush(), "worldedit.brush.ex");

        player.print(String.format("форма огнетушительной кисти сформирована (%.0f).", radius));
    }

    @Command(
            aliases = { "gravity", "grav" },
            usage = "[радиус]",
            flags = "h",
            desc = "Выбрать кисть симуляции гравитации",
            help =
                "Выбрать кисть симуляции гравитации (заставляет блоки падать).\n" +
                "Флаг -h влияет на блоки , начиная с мира max y, " +
                    "вместо нажатия блока y + радиус.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.gravity")
    public void gravityBrush(Player player, LocalSession session, @Optional("5") double radius, @Switch('h') boolean fromMaxY) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new GravityBrush(fromMaxY), "worldedit.brush.gravity");

        player.print(String.format("Форма гравитационной кисти сформиована (%.0f).",
                radius));
    }
    
    @Command(
            aliases = { "butcher", "kill" },
            usage = "[радиус]",
            flags = "plangbtfr",
            desc = "Выбрат кисть уничтожения мобов",
            help = "Уничтожение мобов с определенным радиусом.\n" +
                    "Флаги:\n" +
                    "  -p убить питомцев.\n" +
                    "  -n убить NPC.\n" +
                    "  -g убить Големов.\n" +
                    "  -a убить животных.\n" +
                    "  -b убить окружающих мобов.\n" +
                    "  -t убить мобов с именами.\n" +
                    "  -f соединить все предыдущие флаги.\n" +
                    "  -r разружить Стойки для брони.\n" +
                    "  -l в настоящее время ничего не делает.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.butcher")
    public void butcherBrush(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();

        double radius = args.argsLength() > 0 ? args.getDouble(0) : 5;
        double maxRadius = config.maxBrushRadius;
        // hmmmm not horribly worried about this because -1 is still rather efficient,
        // the problem arises when butcherMaxRadius is some really high number but not infinite
        // - original idea taken from https://github.com/sk89q/worldedit/pull/198#issuecomment-6463108
        if (player.hasPermission("worldedit.butcher")) {
            maxRadius = Math.max(config.maxBrushRadius, config.butcherMaxRadius);
        }
        if (radius > maxRadius) {
            player.printError("Максимально допустимый радиус кисти: " + maxRadius);
            return;
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.fromCommand(args);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new ButcherBrush(flags), "worldedit.brush.butcher");

        player.print(String.format("Форма уничтожительной кисти сформирована (%.0f).", radius));
    }
}
