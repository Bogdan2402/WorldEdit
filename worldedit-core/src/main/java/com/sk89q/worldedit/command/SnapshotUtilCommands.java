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
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.snapshot.InvalidSnapshotException;
import com.sk89q.worldedit.world.snapshot.Snapshot;
import com.sk89q.worldedit.world.snapshot.SnapshotRestore;
import com.sk89q.worldedit.world.storage.ChunkStore;
import com.sk89q.worldedit.world.storage.MissingWorldException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;

public class SnapshotUtilCommands {

    private static final Logger logger = Logger.getLogger("Minecraft.WorldEdit");

    private final WorldEdit we;

    public SnapshotUtilCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
            aliases = { "restore", "/restore" },
            usage = "[snapshot]",
            desc = "Восстановить выделенную территорию из резервной копии",
            min = 0,
            max = 1
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.snapshots.restore")
    public void restore(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Востановление резервной копии не настроено.");
            return;
        }

        Region region = session.getSelection(player.getWorld());
        Snapshot snapshot;

        if (args.argsLength() > 0) {
            try {
                snapshot = config.snapshotRepo.getSnapshot(args.getString(0));
            } catch (InvalidSnapshotException e) {
                player.printError("Эта резервная копия не доступна или не найдена.");
                return;
            }
        } else {
            snapshot = session.getSnapshot();
        }

        // No snapshot set?
        if (snapshot == null) {
            try {
                snapshot = config.snapshotRepo.getDefaultSnapshot(player.getWorld().getName());

                if (snapshot == null) {
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

                    return;
                }
            } catch (MissingWorldException ex) {
                player.printError("Резервных копий в этом мире нет.");
                return;
            }
        }

        ChunkStore chunkStore = null;

        // Load chunk store
        try {
            chunkStore = snapshot.getChunkStore();
            player.print("Резервная копия '" + snapshot.getName() + "' загружена; восстанавление...");
        } catch (DataException e) {
            player.printError("Ошибка при загрузке резервной копии: " + e.getMessage());
            return;
        } catch (IOException e) {
            player.printError("Ошибка при загрузке резервной копии: " + e.getMessage());
            return;
        }

        try {
            // Restore snapshot
            SnapshotRestore restore = new SnapshotRestore(chunkStore, editSession, region);
            //player.print(restore.getChunksAffected() + " chunk(s) will be loaded.");

            restore.restore();

            if (restore.hadTotalFailure()) {
                String error = restore.getLastErrorMessage();
                if (error != null) {
                    player.printError("Ошибка восстановления блоков.");
                    player.printError("Последняя ошибка: " + error);
                } else {
                    player.printError("Нет чанков для загрузки. (Плохой архив??)");
                }
            } else {
                player.print(String.format("Восстановлено; %d "
                        + "пропущено чанков %d и других ошибок.",
                        restore.getMissingChunks().size(),
                        restore.getErrorChunks().size()));
            }
        } finally {
            try {
                chunkStore.close();
            } catch (IOException ignored) {
            }
        }
    }
}
