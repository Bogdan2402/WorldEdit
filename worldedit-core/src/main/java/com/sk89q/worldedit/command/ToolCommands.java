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
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.command.tool.*;
import com.sk89q.worldedit.util.TreeGenerator;

public class ToolCommands {
    private final WorldEdit we;

    public ToolCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        aliases = { "none" },
        usage = "",
        desc = "Отменить привязку к текущему инструменту",
        min = 0,
        max = 0
    )
    public void none(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        session.setTool(player.getItemInHand(), null);
        player.print("Инструмент больше не связан с текущим предметом.");
    }

    @Command(
        aliases = { "info" },
        usage = "",
        desc = "Инструмент информации о блоке",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.tool.info")
    public void info(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        session.setTool(player.getItemInHand(), new QueryTool());
        player.print("Информационный инструмент связан с "
                + ItemType.toHeldName(player.getItemInHand()) + ".");
    }

    @Command(
        aliases = { "tree" },
        usage = "[тип]",
        desc = "Инструмент генерации деревьев",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.tool.tree")
    @SuppressWarnings("deprecation")
    public void tree(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        TreeGenerator.TreeType type = args.argsLength() > 0 ?
                type = TreeGenerator.lookup(args.getString(0))
                : TreeGenerator.TreeType.TREE;

        if (type == null) {
            player.printError("Тип дерева '" + args.getString(0) + "' неизвестен.");
            return;
        }

        session.setTool(player.getItemInHand(), new TreePlanter(new TreeGenerator(type)));
        player.print("Инструмент генерации деревьев связан с "
                + ItemType.toHeldName(player.getItemInHand()) + ".");
    }

    @Command(
        aliases = { "repl" },
        usage = "<блок>",
        desc = "Инструмент блокозаменитель",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.tool.replacer")
    public void repl(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        BaseBlock targetBlock = we.getBlock(player, args.getString(0));
        session.setTool(player.getItemInHand(), new BlockReplacer(targetBlock));
        player.print("Инструмент блокозаменитель связан с "
                + ItemType.toHeldName(player.getItemInHand()) + ".");
    }

    @Command(
        aliases = { "cycler" },
        usage = "",
        desc = "Инструмент изменения метадаты блока (изменение типа деревьев и т.п.",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.tool.data-cycler")
    public void cycler(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        session.setTool(player.getItemInHand(), new BlockDataCyler());
        player.print("Инструмент изменения метадаты блока связан с "
                + ItemType.toHeldName(player.getItemInHand()) + ".");
    }

    @Command(
        aliases = { "floodfill", "flood" },
        usage = "<шаблон> <диапазон>",
        desc = "Инструмент заливки блоков (изменение однотипных соприкасающихся блоков)",
        min = 2,
        max = 2
    )
    @CommandPermissions("worldedit.tool.flood-fill")
    public void floodFill(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();
        int range = args.getInteger(1);

        if (range > config.maxSuperPickaxeSize) {
            player.printError("Максимальный размер: " + config.maxSuperPickaxeSize);
            return;
        }

        Pattern pattern = we.getBlockPattern(player, args.getString(0));
        session.setTool(player.getItemInHand(), new FloodFillTool(range, pattern));
        player.print("Инструмент заливки блоков связан с "
                + ItemType.toHeldName(player.getItemInHand()) + ".");
    }

    @Command(
            aliases = { "deltree" },
            usage = "",
            desc = "Инструмент удаления летающих деревьев",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.tool.deltree")
    public void deltree(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

    session.setTool(player.getItemInHand(), new FloatingTreeRemover());
    player.print("Инструмент удаления летающих деревьев связан с "
            + ItemType.toHeldName(player.getItemInHand()) + ".");
    }

    @Command(
            aliases = { "farwand" },
            usage = "",
            desc = "Инструмент выделения на расстоянии",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.tool.farwand")
    public void farwand(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        session.setTool(player.getItemInHand(), new DistanceWand());
        player.print("Инструмент выделения на расстоянии связан с " + ItemType.toHeldName(player.getItemInHand()) + ".");
    }

    @Command(
            aliases = { "lrbuild", "/lrbuild" },
            usage = "<левый клик по блоку> <правый клин по блоку>",
            desc = "Инструмент разрушения и строительства на расстоянии",
            min = 2,
            max = 2
    )
    @CommandPermissions("worldedit.tool.lrbuild")
    public void longrangebuildtool(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        BaseBlock secondary = we.getBlock(player, args.getString(0));
        BaseBlock primary = we.getBlock(player, args.getString(1));
        session.setTool(player.getItemInHand(), new LongRangeBuildTool(primary, secondary));
        player.print("Инструмент разрушения и строительства на расстоянии связан с " + ItemType.toHeldName(player.getItemInHand()) + ".");
        player.print("ЛКМ установить на " + ItemType.toName(secondary.getType()) + "; ПКМ установить на "
                + ItemType.toName(primary.getType()) + ".");
    }
}
