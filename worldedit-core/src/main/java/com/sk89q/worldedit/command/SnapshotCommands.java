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

// $Id$

package com.sk89q.worldedit.command;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.snapshot.InvalidSnapshotException;
import com.sk89q.worldedit.world.snapshot.Snapshot;
import com.sk89q.worldedit.world.storage.MissingWorldException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

/**
 * Snapshot commands.
 */
public class SnapshotCommands {

    private static final Logger logger = Logger.getLogger("Minecraft.WorldEdit");
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    
    private final WorldEdit we;

    public SnapshotCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
            aliases = { "list" },
            usage = "[num]",
            desc = "Список резервных копий",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.snapshots.list")
    public void list(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Резервная копия не настроена.");
            return;
        }

        try {
            List<Snapshot> snapshots = config.snapshotRepo.getSnapshots(true, player.getWorld().getName());

            if (!snapshots.isEmpty()) {

                int num = args.argsLength() > 0 ? Math.min(40, Math.max(5, args.getInteger(0))) : 5;

                player.print("Резервные копии для мира: '" + player.getWorld().getName() + "'");
                for (byte i = 0; i < Math.min(num, snapshots.size()); i++) {
                    player.print((i + 1) + ". " + snapshots.get(i).getName());
                }

                player.print("Используйте /snap use [snapshot] или /snap.");
            } else {
                player.printError("Резервных копий не найдено. Смотрите консоль для деталей.");

                // Okay, let's toss some debugging information!
                File dir = config.snapshotRepo.getDirectory();

                try {
                    logger.info("WorldEdit не нашел резервных копий: смотрел в: "
                            + dir.getCanonicalPath());
                } catch (IOException e) {
                    logger.info("WorldEdit не нашел резервных копий: смотрел в "
                            + "(NON-RESOLVABLE PATH - существует ли она?): "
                            + dir.getPath());
                }
            }
        } catch (MissingWorldException ex) {
            player.printError("Резервных копий для этого мира нет.");
        }
    }

    @Command(
            aliases = { "use" },
            usage = "<snapshot>",
            desc = "Выбрать резервную копию для использования",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void use(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Резервная копия не настроена.");
            return;
        }

        String name = args.getString(0);

        // Want the latest snapshot?
        if (name.equalsIgnoreCase("latest")) {
            try {
                Snapshot snapshot = config.snapshotRepo.getDefaultSnapshot(player.getWorld().getName());

                if (snapshot != null) {
                    session.setSnapshot(null);
                    player.print("Используется новейшая резервная копия.");
                } else {
                    player.printError("Резервная копия не найдена.");
                }
            } catch (MissingWorldException ex) {
                player.printError("Резервных копий для этого мира нет.");
            }
        } else {
            try {
                session.setSnapshot(config.snapshotRepo.getSnapshot(name));
                player.print("Резервная копия загружена под именем: " + name);
            } catch (InvalidSnapshotException e) {
                player.printError("Эта резервная копия не найдена или повреждена.");
            }
        }
    }

    @Command(
            aliases = { "sel" },
            usage = "<index>",
            desc = "Выбрать резервную копию из списка",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void sel(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Резервная копия не настроена.");
            return;
        }

        int index = -1;
        try {
            index = Integer.parseInt(args.getString(0));
        } catch (NumberFormatException e) {
            player.printError("Неверный индекс, " + args.getString(0) + " не является целым числом.");
            return;
        }

        if (index < 1) {
            player.printError("Индекс должен быть равен или выше одного.");
            return;
        }

        try {
            List<Snapshot> snapshots = config.snapshotRepo.getSnapshots(true, player.getWorld().getName());
            if (snapshots.size() < index) {
                player.printError("Неверный индекс, должен быть между 1 и " + snapshots.size() + ".");
                return;
            }
            Snapshot snapshot = snapshots.get(index - 1);
            if (snapshot == null) {
                player.printError("Эта резервная копия не найдена или повреждена.");
                return;
            }
            session.setSnapshot(snapshot);
            player.print("Резервная копия загружена под именем: " + snapshot.getName());
        } catch (MissingWorldException e) {
            player.printError("Резервных копий для этого мира нет.");
        }
    }

    @Command(
            aliases = { "before" },
            usage = "<дата>",
            desc = "Выбрать ближайшую резервную копию до даты",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void before(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Резервная копия не настроена.");
            return;
        }

        Calendar date = session.detectDate(args.getJoinedStrings(0));

        if (date == null) {
            player.printError("Не удалось найти введенную дату.");
        } else {
            try {
                Snapshot snapshot = config.snapshotRepo.getSnapshotBefore(date, player.getWorld().getName());

                if (snapshot == null) {
                    dateFormat.setTimeZone(session.getTimeZone());
                    player.printError("Не удалось найти резервную копию до "
                            + dateFormat.format(date.getTime()) + ".");
                } else {
                    session.setSnapshot(snapshot);
                    player.print("Резервная копия загружена под именем: " + snapshot.getName());
                }
            } catch (MissingWorldException ex) {
                player.printError("Резервных копий для этого мира нет.");
            }
        }
    }

    @Command(
            aliases = { "after" },
            usage = "<дата>",
            desc = "Выбрать ближайшую резервную копию после даты",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.snapshots.restore")
    public void after(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Резервная копия не настроена.");
            return;
        }

        Calendar date = session.detectDate(args.getJoinedStrings(0));

        if (date == null) {
            player.printError("Не удалось найти введенную дату.");
        } else {
            try {
                Snapshot snapshot = config.snapshotRepo.getSnapshotAfter(date, player.getWorld().getName());
                if (snapshot == null) {
                    dateFormat.setTimeZone(session.getTimeZone());
                    player.printError("Не удалось найти резервную копию до "
                            + dateFormat.format(date.getTime()) + ".");
                } else {
                    session.setSnapshot(snapshot);
                    player.print("Резервная копия загружена под именем: " + snapshot.getName());
                }
            } catch (MissingWorldException ex) {
                player.printError("Резервных копий для этого мира нет.");
            }
        }
    }

}
