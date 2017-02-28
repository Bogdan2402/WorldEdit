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
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.PLACEMENT;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;

/**
 * Clipboard commands.
 */
public class ClipboardCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public ClipboardCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        aliases = { "/copy" },
        flags = "em",
        desc = "Cкопировать выделенную область в буфер обмена",
        help = "Cкопировать выделеную область в буфер обмена\n" +
                "Флаги:\n" +
                "  -e для копирования сущностей в нем\n" +
                "  -m задает маску исходного, чтобы исключить блоки воздуха\n" +
                "ВНИМАНИЕ: Вставка сущностей не может быть отменена!",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.clipboard.copy")
    public void copy(Player player, LocalSession session, EditSession editSession,
                     @Selection Region region, @Switch('e') boolean copyEntities,
                     @Switch('m') Mask mask) throws WorldEditException {

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(session.getPlacementPosition(player));
        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        if (mask != null) {
            copy.setSourceMask(mask);
        }
        Operations.completeLegacy(copy);
        session.setClipboard(new ClipboardHolder(clipboard, editSession.getWorld().getWorldData()));

        player.print(region.getArea() + " блок(ов) скопировано.");
    }

    @Command(
        aliases = { "/cut" },
        flags = "em",
        usage = "[выходной-id]",
        desc = "Вырезать выделенную область в буфер обмена",
        help = "Вырезать выделенную область в буфер обмена\n" +
                "Флаги:\n" +
                "  -e для вырезания также сущностей в нем\n" +
                "  -m задает маску исходного, чтобы исключить блоки воздуха\n" +
                "ВНИМАНИЕ: Вырезание и вставка сущностей не может быть отменена!",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.clipboard.cut")
    @Logging(REGION)
    public void cut(Player player, LocalSession session, EditSession editSession,
                    @Selection Region region, @Optional("air") Pattern leavePattern, @Switch('e') boolean copyEntities,
                    @Switch('m') Mask mask) throws WorldEditException {

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(session.getPlacementPosition(player));
        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        copy.setSourceFunction(new BlockReplace(editSession, leavePattern));
        if (mask != null) {
            copy.setSourceMask(mask);
        }
        Operations.completeLegacy(copy);
        session.setClipboard(new ClipboardHolder(clipboard, editSession.getWorld().getWorldData()));

        player.print(region.getArea() + " блок(ов) скопировано.");
    }

    @Command(
        aliases = { "/paste" },
        usage = "",
        flags = "sao",
        desc = "Вставить содержимое буфера обмена",
        help =
            "Вставить содержимое буфера обмена.\n" +
            "Флаги:\n" +
            "  -a пропустить блоки воздуха\n" +
            "  -o вставить в изначальной позиции\n" +
            "  -s выбрать региона после вставки",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.clipboard.paste")
    @Logging(PLACEMENT)
    public void paste(Player player, LocalSession session, EditSession editSession,
                      @Switch('a') boolean ignoreAirBlocks, @Switch('o') boolean atOrigin,
                      @Switch('s') boolean selectPasted) throws WorldEditException {

        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        Region region = clipboard.getRegion();

        Vector to = atOrigin ? clipboard.getOrigin() : session.getPlacementPosition(player);
        Operation operation = holder
                .createPaste(editSession, editSession.getWorld().getWorldData())
                .to(to)
                .ignoreAirBlocks(ignoreAirBlocks)
                .build();
        Operations.completeLegacy(operation);

        if (selectPasted) {
            Vector clipboardOffset = clipboard.getRegion().getMinimumPoint().subtract(clipboard.getOrigin());
            Vector realTo = to.add(holder.getTransform().apply(clipboardOffset));
            Vector max = realTo.add(holder.getTransform().apply(region.getMaximumPoint().subtract(region.getMinimumPoint())));
            RegionSelector selector = new CuboidRegionSelector(player.getWorld(), realTo, max);
            session.setRegionSelector(player.getWorld(), selector);
            selector.learnChanges();
            selector.explainRegionAdjust(player, session);
        }

        player.print("Буфер обмена был вставлен в " + to);
    }

    @Command(
        aliases = { "/rotate" },
        usage = "<ось-y> [<ось-x>] [<ось-z>]",
        desc = "Повернуть содержимое буфера обмена",
        help = "Непагубно вращать содержимое буфера обмена.\n" +
               "Углы в градусах и положительный угол приведет к вращению по часовой стрелке. " +
               "Несколько оборотов могут быть уложены. Углы должны делиться на 90 градусов.\n"
    )
    @CommandPermissions("worldedit.clipboard.rotate")
    public void rotate(Player player, LocalSession session, Double yRotate, @Optional Double xRotate, @Optional Double zRotate) throws WorldEditException {
        if ((yRotate != null && Math.abs(yRotate % 90) > 0.001) ||
                xRotate != null && Math.abs(xRotate % 90) > 0.001 ||
                zRotate != null && Math.abs(zRotate % 90) > 0.001) {
            player.printDebug("Примечание: Интерполяция не поддерживается, поэтому углы рекомендуется кратные 90.");
        }

        ClipboardHolder holder = session.getClipboard();
        AffineTransform transform = new AffineTransform();
        transform = transform.rotateY(-(yRotate != null ? yRotate : 0));
        transform = transform.rotateX(-(xRotate != null ? xRotate : 0));
        transform = transform.rotateZ(-(zRotate != null ? zRotate : 0));
        holder.setTransform(holder.getTransform().combine(transform));
        player.print("Буфер обмена был повернут.");
    }

    @Command(
        aliases = { "/flip" },
        usage = "[<направление>]",
        desc = "Перевернуть содержимое буфера обмена",
        help =
            "Перевернуть содержимое буфера обмена.\n",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.clipboard.flip")
    public void flip(Player player, LocalSession session, EditSession editSession,
                     @Optional(Direction.AIM) @Direction Vector direction) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        AffineTransform transform = new AffineTransform();
        transform = transform.scale(direction.positive().multiply(-2).add(1, 1, 1));
        holder.setTransform(holder.getTransform().combine(transform));
        player.print("Содержимое буфера обмена было перевернуто.");
    }

    @Command(
        aliases = { "/load" },
        usage = "<имя_файла>",
        desc = "Загрузить схематический файл в буфер обмена",
        min = 0,
        max = 1
    )
    @Deprecated
    @CommandPermissions("worldedit.clipboard.load")
    public void load(Actor actor) {
        actor.printError("Эта команда больше не используется. Используйте //schematic load.");
    }

    @Command(
        aliases = { "/save" },
        usage = "<имя_файла>",
        desc = "Сохранить буфер обмена в схематический файл",
        min = 0,
        max = 1
    )
    @Deprecated
    @CommandPermissions("worldedit.clipboard.save")
    public void save(Actor actor) {
        actor.printError("Эта команда больше не используется. Используйте //schematic save.");
    }

    @Command(
        aliases = { "clearclipboard" },
        usage = "",
        desc = "Очистить буфер обмена",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.clipboard.clear")
    public void clearClipboard(Player player, LocalSession session, EditSession editSession) throws WorldEditException {
        session.setClipboard(null);
        player.print("Буфер обмена очищен.");
    }
}
