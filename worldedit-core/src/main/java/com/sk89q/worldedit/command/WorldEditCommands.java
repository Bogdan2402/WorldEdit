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
import com.sk89q.worldedit.event.platform.ConfigurationLoadEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.PlatformManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class WorldEditCommands {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    
    private final WorldEdit we;
    
    public WorldEditCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        aliases = { "version", "ver" },
        usage = "",
        desc = "Показать версию WorldGuard",
        min = 0,
        max = 0
    )
    public void version(Actor actor) throws WorldEditException {
        actor.print("§eВерсия WorldEdit 6.1.7-SNAPSHOT;");
        actor.print("§ahttp://vk.com/b_o_d_ik §7§l| §7Перевел §eDarkFort");
    }

    @Command(
        aliases = { "reload" },
        usage = "",
        desc = "Перезагрузить WorldEdit",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.reload")
    public void reload(Actor actor) throws WorldEditException {
        we.getServer().reload();
        we.getEventBus().post(new ConfigurationLoadEvent(we.getPlatformManager().queryCapability(Capability.CONFIGURATION).getConfiguration()));
        actor.print("Конфигурация перезагружена!");
    }

    @Command(
        aliases = { "cui" },
        usage = "",
        desc = "Связаться с модом WorldEditCUI",
        min = 0,
        max = 0
    )
    public void cui(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        session.setCUISupport(true);
        session.dispatchCUISetup(player);
    }

    @Command(
        aliases = { "tz" },
        usage = "[часовой пояс]",
        desc = "Установить вашу временную(Часовой пояс) зону",
        min = 1,
        max = 1
    )
    public void tz(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        TimeZone tz = TimeZone.getTimeZone(args.getString(0));
        session.setTimezone(tz);
        player.print("Временная зона установлена для этой сессии на: " + tz.getDisplayName());
        player.print("Время в вашей временной зоне: "
                + dateFormat.format(Calendar.getInstance(tz).getTime()));
    }

    @Command(
        aliases = { "help" },
        usage = "[<команда>]",
            desc = "Показать помощь по определенной команде или вывести список всех команд",
        min = 0,
        max = -1
    )
    @CommandPermissions("worldedit.help")
    public void help(Actor actor, CommandContext args) throws WorldEditException {
        UtilityCommands.help(args, we, actor);
    }
}
