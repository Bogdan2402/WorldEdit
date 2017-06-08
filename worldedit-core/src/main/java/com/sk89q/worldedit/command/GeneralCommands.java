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
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.util.command.parametric.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * General WorldEdit commands.
 */
public class GeneralCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public GeneralCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        aliases = { "/limit" },
        usage = "<лимит>",
        desc = "Изменить лимит изменения регионов",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.limit")
    public void limit(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        
        LocalConfiguration config = worldEdit.getConfiguration();
        boolean mayDisable = player.hasPermission("worldedit.limit.unrestricted");

        int limit = Math.max(-1, args.getInteger(0));
        if (!mayDisable && config.maxChangeLimit > -1) {
            if (limit > config.maxChangeLimit) {
                player.printError("Ваш максимальный лимит блоков " + config.maxChangeLimit + ".");
                return;
            }
        }

        session.setBlockChangeLimit(limit);

        if (limit != -1) {
            player.print("Лимит блоков изменен на " + limit + ". (Используйте //limit -1 чтобы вернуть значению по умолчанию.)");
        } else {
            player.print("Лимит блоков изменен на " + limit + ".");
        }
    }

    @Command(
        aliases = { "/fast" },
        usage = "[on|off]",
        desc = "Переключить скоростной режим",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.fast")
    public void fast(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        String newState = args.getString(0, null);
        if (session.hasFastMode()) {
            if ("on".equals(newState)) {
                player.printError("Скоростной режим уже включен.");
                return;
            }

            session.setFastMode(false);
            player.print("Скоротной режим отключен.");
        } else {
            if ("off".equals(newState)) {
                player.printError("Скоростной режим уже отключен.");
                return;
            }

            session.setFastMode(true);
            player.print("Скоростной режим включен. Освещение в чанках может быть неправильным и Вам придется перезайти на сервер, чтобы увидть изменения.");
        }
    }

    @Command(
        aliases = { "/gmask", "gmask" },
        usage = "[маска]",
        desc = "Задать глобальную маску",
        min = 0,
        max = -1
    )
    @CommandPermissions("worldedit.global-mask")
    public void gmask(Player player, LocalSession session, @Optional Mask mask) throws WorldEditException {
        if (mask == null) {
            session.setMask((Mask) null);
            player.print("Глобальная маска выключена.");
        } else {
            session.setMask(mask);
            player.print("Глобальная маска включена.");
        }
    }

    @Command(
        aliases = { "/toggleplace", "toggleplace" },
        usage = "",
        desc = "Переключить первую выделенную позицию и ваше текущее положение, как будто вы находитесь на первой позиции",
        min = 0,
        max = 0
    )
    public void togglePlace(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        if (session.togglePlacementPosition()) {
            player.print("Теперь вы в позиции #1.");
        } else {
            player.print("Теперь вы в позиции, где вы стоите.");
        }
    }

    @Command(
        aliases = { "/searchitem", "/l", "/search", "searchitem" },
        usage = "<запрос>",
        flags = "bi",
        desc = "Найти предмет или блок",
        help =
            "Найти предмет или блок.\n" +
            "Флаги:\n" +
            "  -b искать только среди блоков\n" +
            "  -i искать только среди предметов",
        min = 1,
        max = 1
    )
    public void searchItem(Actor actor, CommandContext args) throws WorldEditException {
        
        String query = args.getString(0).trim().toLowerCase();
        boolean blocksOnly = args.hasFlag('b');
        boolean itemsOnly = args.hasFlag('i');

        try {
            int id = Integer.parseInt(query);

            ItemType type = ItemType.fromID(id);

            if (type != null) {
                actor.print("#" + type.getID() + " (" + type.getName() + ")");
            } else {
                actor.printError("Предмет не найден с таким ID " + id);
            }

            return;
        } catch (NumberFormatException ignored) {
        }

        if (query.length() <= 2) {
            actor.printError("В запросе слишком мало символов (должно быть больше двух).");
            return;
        }

        if (!blocksOnly && !itemsOnly) {
            actor.print("Результат для: " + query);
        } else if (blocksOnly && itemsOnly) {
            actor.printError("Вы не можете одновременно использовать флаги 'b' и 'i'.");
            return;
        } else if (blocksOnly) {
            actor.print("Результат для блоков: " + query);
        } else {
            actor.print("Результат для предметов: " + query);
        }

        int found = 0;

        for (ItemType type : ItemType.values()) {
            if (found >= 15) {
                actor.print("Слишком много было найдено!");
                break;
            }

            if (blocksOnly && type.getID() > 255) {
                continue;
            }

            if (itemsOnly && type.getID() <= 255) {
                continue;
            }

            for (String alias : type.getAliases()) {
                if (alias.contains(query)) {
                    actor.print("#" + type.getID() + " (" + type.getName() + ")");
                    ++found;
                    break;
                }
            }
        }

        if (found == 0) {
            actor.printError("Предмет не найден.");
        }
    }

}
