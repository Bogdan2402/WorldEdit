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
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.command.parametric.Optional;

/**
 * Tool commands.
 */
public class ToolUtilCommands {
    private final WorldEdit we;

    public ToolUtilCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        aliases = { "/", "," },
        usage = "[on|off]",
        desc = "Переключить состояние суперкирки",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.superpickaxe")
    public void togglePickaxe(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        String newState = args.getString(0, null);
        if (session.hasSuperPickAxe()) {
            if ("on".equals(newState)) {
                player.printError("Супер кирка уже включена.");
                return;
            }

            session.disableSuperPickAxe();
            player.print("Супер кирка отключена.");
        } else {
            if ("off".equals(newState)) {
                player.printError("Супер кирка уже отключена.");
                return;
            }
            session.enableSuperPickAxe();
            player.print("Супер кирка включена.");
        }

    }

    @Command(
        aliases = { "mask" },
        usage = "[маска]",
        desc = "Задать маску кисти",
        min = 0,
        max = -1
    )
    @CommandPermissions("worldedit.brush.options.mask")
    public void mask(Player player, LocalSession session, EditSession editSession, @Optional Mask mask) throws WorldEditException {
        if (mask == null) {
            session.getBrushTool(player.getItemInHand()).setMask(null);
            player.print("Маска кисти отключена.");
        } else {
            session.getBrushTool(player.getItemInHand()).setMask(mask);
            player.print("Маска кисти установлена.");
        }
    }

    @Command(
        aliases = { "mat", "material" },
        usage = "[шаблон]",
        desc = "Задать материал кисти",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.brush.options.material")
    public void material(Player player, LocalSession session, EditSession editSession, Pattern pattern) throws WorldEditException {
        session.getBrushTool(player.getItemInHand()).setFill(pattern);
        player.print("Материал кисти установлен.");
    }

    @Command(
            aliases = { "range" },
            usage = "[шаблон]",
            desc = "Задать диапазон кисти",
            min = 1,
            max = 1
        )
    @CommandPermissions("worldedit.brush.options.range")
    public void range(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        int range = args.getInteger(0);
        session.getBrushTool(player.getItemInHand()).setRange(range);
        player.print("Диапазон кисти установлен.");
    }

    @Command(
        aliases = { "size" },
        usage = "[шаблон]",
        desc = "Задать размер кисти(Максимум 6)",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.brush.options.size")
    public void size(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        int radius = args.getInteger(0);
        we.checkMaxBrushRadius(radius);

        session.getBrushTool(player.getItemInHand()).setSize(radius);
        player.print("Размер кисти установлен.");
    }
}
