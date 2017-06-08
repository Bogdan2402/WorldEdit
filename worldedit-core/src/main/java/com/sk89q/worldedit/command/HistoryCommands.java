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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Commands to undo, redo, and clear history.
 */
public class HistoryCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public HistoryCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        aliases = { "/undo", "undo" },
        usage = "[время] [игрок]",
        desc = "Отменить действие",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.history.undo")
    public void undo(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        int times = Math.max(1, args.getInteger(0, 1));
        for (int i = 0; i < times; ++i) {
            EditSession undone;
            if (args.argsLength() < 2) {
                undone = session.undo(session.getBlockBag(player), player);
            } else {
                player.checkPermission("worldedit.history.undo.other");
                LocalSession sess = worldEdit.getSession(args.getString(1));
                if (sess == null) {
                    player.printError("Не удалось найти сессию " + args.getString(1));
                    break;
                }
                undone = sess.undo(session.getBlockBag(player), player);
            }
            if (undone != null) {
                player.print("Успешно отменено.");
                worldEdit.flushBlockBag(player, undone);
            } else {
                player.printError("Больше нечего отменять.");
                break;
            }
        }
    }

    @Command(
        aliases = { "/redo", "redo" },
        usage = "[время] [игрок]",
        desc = "Возвратить действие (из истории)",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.history.redo")
    public void redo(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        
        int times = Math.max(1, args.getInteger(0, 1));

        for (int i = 0; i < times; ++i) {
            EditSession redone;
            if (args.argsLength() < 2) {
                redone = session.redo(session.getBlockBag(player), player);
            } else {
                player.checkPermission("worldedit.history.redo.other");
                LocalSession sess = worldEdit.getSession(args.getString(1));
                if (sess == null) {
                    player.printError("Не удалось найти сессию " + args.getString(1));
                    break;
                }
                redone = sess.redo(session.getBlockBag(player), player);
            }
            if (redone != null) {
                player.print("Успешно возвращено.");
                worldEdit.flushBlockBag(player, redone);
            } else {
                player.printError("Больше нечего возвращать.");
            }
        }
    }

    @Command(
        aliases = { "/clearhistory", "clearhistory" },
        usage = "",
        desc = "Очистить историю действий",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.history.clear")
    public void clearHistory(Player player, LocalSession session) throws WorldEditException {
        session.clearHistory();
        player.print("История очищена.");
    }

}
