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
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.generator.FloraGenerator;
import com.sk89q.worldedit.function.generator.ForestGenerator;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.NoiseFilter2D;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.Patterns;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.convolution.GaussianKernel;
import com.sk89q.worldedit.math.convolution.HeightMap;
import com.sk89q.worldedit.math.convolution.HeightMapFilter;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.command.parametric.Optional;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.*;
import static com.sk89q.worldedit.regions.Regions.*;

/**
 * Commands that operate on regions.
 */
public class RegionCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public RegionCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            aliases = { "/line" },
            usage = "<блок> [толщина]",
            desc = "Построить линию между углами выделенного кубоида",
            help =
                "Строит линию между углами выделенного кубоида.\n" +
                "Может быть использовано только с кубоидным выделением.\n" +
                "Флаги:\n" +
                "  -h генерировть только поверхность",
            flags = "h",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.region.line")
    @Logging(REGION)
    public void line(Player player, EditSession editSession,
                     @Selection Region region,
                     Pattern pattern,
                     @Optional("0") @Range(min = 0) int thickness,
                     @Switch('h') boolean shell) throws WorldEditException {

        if (!(region instanceof CuboidRegion)) {
            player.printError("//line работает только с кубоидным выделением");
            return;
        }

        CuboidRegion cuboidregion = (CuboidRegion) region;
        Vector pos1 = cuboidregion.getPos1();
        Vector pos2 = cuboidregion.getPos2();
        int blocksChanged = editSession.drawLine(Patterns.wrap(pattern), pos1, pos2, thickness, !shell);

        player.print(blocksChanged + " блок(ов) было изменено.");
    }

    @Command(
            aliases = { "/curve" },
            usage = "<блок> [толщина]",
            desc = "Построить кривую через выделенные точки",
            help =
                "Строит кривую через выделенные точки.\n" +
                "Может быть использована только с многоугольным выделением.\n" +
                "Флаги:\n" +
                "  -h построить только стены",
            flags = "h",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.region.curve")
    @Logging(REGION)
    public void curve(Player player, EditSession editSession,
                      @Selection Region region,
                      Pattern pattern,
                      @Optional("0") @Range(min = 0) int thickness,
                      @Switch('h') boolean shell) throws WorldEditException {
        if (!(region instanceof ConvexPolyhedralRegion)) {
            player.printError("//curve работает только с выпуклыми многогранными выделениями");
            return;
        }

        ConvexPolyhedralRegion cpregion = (ConvexPolyhedralRegion) region;
        List<Vector> vectors = new ArrayList<Vector>(cpregion.getVertices());

        int blocksChanged = editSession.drawSpline(Patterns.wrap(pattern), vectors, 0, 0, 0, 10, thickness, !shell);

        player.print(blocksChanged + " блок(ов) было изменено.");
    }

    @Command(
        aliases = { "/replace", "/re", "/rep" },
        usage = "[из-блока] <в-блок>",
        desc = "Заменить определенные блоки на другие",
        flags = "f",
        min = 1,
        max = 2
    )
    @CommandPermissions("worldedit.region.replace")
    @Logging(REGION)
    public void replace(Player player, EditSession editSession, @Selection Region region, @Optional Mask from, Pattern to) throws WorldEditException {
        if (from == null) {
            from = new ExistingBlockMask(editSession);
        }
        int affected = editSession.replaceBlocks(region, from, Patterns.wrap(to));
        player.print(affected + " блок(ов) было заменено.");
    }

    @Command(
        aliases = { "/overlay" },
        usage = "<блок>",
        desc = "Наложить блоки поверх выделенной территории",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.region.overlay")
    @Logging(REGION)
    public void overlay(Player player, EditSession editSession, @Selection Region region, Pattern pattern) throws WorldEditException {
        int affected = editSession.overlayCuboidBlocks(region, Patterns.wrap(pattern));
        player.print(affected + " блок(ов) было наложено.");
    }

    @Command(
        aliases = { "/center", "/middle" },
        usage = "<блок>",
        desc = "Задать центральный(е) блок(и)",
        min = 1,
        max = 1
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.region.center")
    public void center(Player player, EditSession editSession, @Selection Region region, Pattern pattern) throws WorldEditException {
        int affected = editSession.center(region, Patterns.wrap(pattern));
        player.print("Центральный(е) блок(и) ("+ affected + " изменены)");
    }

    @Command(
        aliases = { "/naturalize" },
        usage = "",
        desc = "Создать 3 слоя грязи сверху и камень под ней",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.region.naturalize")
    @Logging(REGION)
    public void naturalize(Player player, EditSession editSession, @Selection Region region) throws WorldEditException {
        int affected = editSession.naturalizeCuboidBlocks(region);
        player.print(affected + " блок(ов) было изменено.");
    }

    @Command(
        aliases = { "/walls" },
        usage = "<блок>",
        desc = "Создать стены ыделенной терртории",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.region.walls")
    @Logging(REGION)
    public void walls(Player player, EditSession editSession, @Selection Region region, Pattern pattern) throws WorldEditException {
        int affected = editSession.makeCuboidWalls(region, Patterns.wrap(pattern));
        player.print(affected + " блок(ов) было изменено.");
    }

    @Command(
        aliases = { "/faces", "/outline" },
        usage = "<блок>",
        desc = "Построить стены, пол и потолок вокруг выделенной территории",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.region.faces")
    @Logging(REGION)
    public void faces(Player player, EditSession editSession, @Selection Region region, Pattern pattern) throws WorldEditException {
        int affected = editSession.makeCuboidFaces(region, Patterns.wrap(pattern));
        player.print(affected + " блок(ов) было изменено.");
    }

    @Command(
        aliases = { "/smooth" },
        usage = "[повторения]",
        flags = "n",
        desc = "Сгладить (сделать реалистичнее) выбранную территорию",
        help =
            "Сгладить (сделать реалистичнее) выбранную территорию.\n" +
            "-n сглаживать только натуральные (природные) структуры.",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.region.smooth")
    @Logging(REGION)
    public void smooth(Player player, EditSession editSession, @Selection Region region, @Optional("1") int iterations, @Switch('n') boolean affectNatural) throws WorldEditException {
        HeightMap heightMap = new HeightMap(editSession, region, affectNatural);
        HeightMapFilter filter = new HeightMapFilter(new GaussianKernel(5, 1.0));
        int affected = heightMap.applyFilter(filter, iterations);
        player.print("Ландшафт изменен. " + affected + " блок(ов) изменено.");

    }

    @Command(
        aliases = { "/move" },
        usage = "[количество] [направление] [leave-id]",
        flags = "s",
        desc = "Перемещение содержимого выделенной территории",
        help =
            "Сдвинуть блоки в выделенном регионе.\n" +
            "-s cдвигает выделение к целевому местоположению.\n" +
            "По желанию заполняет старое местоположение <выходной-id>.",
        min = 0,
        max = 3
    )
    @CommandPermissions("worldedit.region.move")
    @Logging(ORIENTATION_REGION)
    public void move(Player player, EditSession editSession, LocalSession session,
                     @Selection Region region,
                     @Optional("1") @Range(min = 1) int count,
                     @Optional(Direction.AIM) @Direction Vector direction,
                     @Optional("air") BaseBlock replace,
                     @Switch('s') boolean moveSelection) throws WorldEditException {

        int affected = editSession.moveRegion(region, direction, count, true, replace);

        if (moveSelection) {
            try {
                region.shift(direction.multiply(count));

                session.getRegionSelector(player.getWorld()).learnChanges();
                session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);
            } catch (RegionOperationException e) {
                player.printError(e.getMessage());
            }
        }

        player.print(affected + " блок(ов) было сдвинуто.");
    }

    @Command(
        aliases = { "/stack" },
        usage = "[количество] [направление]",
        flags = "sa",
        desc = "Повторить содержимое выделенного региона",
        help =
            "Повторить содержимое выделенного региона.\n" +
            "Флаги:\n" +
            "  -s смещает выбор на последней сложенной копии\n" +
            "  -a пропустить блоки воздуха",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.region.stack")
    @Logging(ORIENTATION_REGION)
    public void stack(Player player, EditSession editSession, LocalSession session,
                      @Selection Region region,
                      @Optional("1") @Range(min = 1) int count,
                      @Optional(Direction.AIM) @Direction Vector direction,
                      @Switch('s') boolean moveSelection,
                      @Switch('a') boolean ignoreAirBlocks) throws WorldEditException {
        int affected = editSession.stackCuboidRegion(region, direction, count, !ignoreAirBlocks);

        if (moveSelection) {
            try {
                final Vector size = region.getMaximumPoint().subtract(region.getMinimumPoint());

                final Vector shiftVector = direction.multiply(count * (Math.abs(direction.dot(size)) + 1));
                region.shift(shiftVector);

                session.getRegionSelector(player.getWorld()).learnChanges();
                session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);
            } catch (RegionOperationException e) {
                player.printError(e.getMessage());
            }
        }

        player.print(affected + " блок(ов) было изменено. Отменить //undo");
    }

    @Command(
        aliases = { "/regen" },
        usage = "",
        desc = "Регенерация содержимого выделенной территории",
        help =
            "Регенерация содержимого выделенной территории.\n" +
            "Эта команда может повлиять на вещи вне выделения,\n" +
            "если они находятся в одном чанке.",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.regen")
    @Logging(REGION)
    public void regenerateChunk(Player player, LocalSession session, EditSession editSession, @Selection Region region) throws WorldEditException {
        Mask mask = session.getMask();
        try {
            session.setMask((Mask) null);
            player.getWorld().regenerate(region, editSession);
        } finally {
            session.setMask(mask);
        }
        player.print("Регион регенерирован.");
    }

    @Command(
            aliases = { "/deform" },
            usage = "<выражение>",
            desc = "Деформировать выделенную территорию",
            help =
                "Деформировать выделенную территорию\n" +
                "Выполняется для каждого блока\n" +
                "изменить переменные х, у и г, чтобы указать на новый блок\n" +
                "для выборки. Смотрите также tinyurl.com/wesyntax.",
            flags = "ro",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.region.deform")
    @Logging(ALL)
    public void deform(Player player, LocalSession session, EditSession editSession,
                       @Selection Region region,
                       @Text String expression,
                       @Switch('r') boolean useRawCoords,
                       @Switch('o') boolean offset) throws WorldEditException {
        final Vector zero;
        Vector unit;

        if (useRawCoords) {
            zero = Vector.ZERO;
            unit = Vector.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(player);
            unit = Vector.ONE;
        } else {
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            zero = max.add(min).multiply(0.5);
            unit = max.subtract(zero);

            if (unit.getX() == 0) unit = unit.setX(1.0);
            if (unit.getY() == 0) unit = unit.setY(1.0);
            if (unit.getZ() == 0) unit = unit.setZ(1.0);
        }

        try {
            final int affected = editSession.deformRegion(region, zero, unit, expression);
            player.findFreePosition();
            player.print(affected + " блок(ов) было деформировано.");
        } catch (ExpressionException e) {
            player.printError(e.getMessage());
        }
    }

    @Command(
        aliases = { "/hollow" },
        usage = "[<толщина>[ <блок>]]",
        desc = "Создать впадинч из обьектов, содержащихся в этой территории",
        help =
            "Создать впадины из обьектов, содержащихся в этой территории.\n" +
            "Может заполнять впадины определенным блоком.\n" +
            "Толщина измеряется в manhattan расстоянии.",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.region.hollow")
    @Logging(REGION)
    public void hollow(Player player, EditSession editSession,
                       @Selection Region region,
                       @Optional("0") @Range(min = 0) int thickness,
                       @Optional("air") Pattern pattern) throws WorldEditException {

        int affected = editSession.hollowOutRegion(region, thickness, Patterns.wrap(pattern));
        player.print(affected + " блок(ов) было изменено.");
    }

    @Command(
            aliases = { "/forest" },
            usage = "[тип] [плотность]",
            desc = "Создать лес в регионе",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.region.forest")
    @Logging(REGION)
    public void forest(Player player, EditSession editSession, @Selection Region region, @Optional("tree") TreeType type,
                       @Optional("5") @Range(min = 0, max = 100) double density) throws WorldEditException {
        density = density / 100;
        ForestGenerator generator = new ForestGenerator(editSession, new TreeGenerator(type));
        GroundFunction ground = new GroundFunction(new ExistingBlockMask(editSession), generator);
        LayerVisitor visitor = new LayerVisitor(asFlatRegion(region), minimumBlockY(region), maximumBlockY(region), ground);
        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density));
        Operations.completeLegacy(visitor);

        player.print(ground.getAffected() + " деревьев сгенерировано.");
    }

    @Command(
            aliases = { "/flora" },
            usage = "[плотность]",
            desc = "Создать флору в регионе",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.region.flora")
    @Logging(REGION)
    public void flora(Player player, EditSession editSession, @Selection Region region, @Optional("10") @Range(min = 0, max = 100) double density) throws WorldEditException {
        density = density / 100;
        FloraGenerator generator = new FloraGenerator(editSession);
        GroundFunction ground = new GroundFunction(new ExistingBlockMask(editSession), generator);
        LayerVisitor visitor = new LayerVisitor(asFlatRegion(region), minimumBlockY(region), maximumBlockY(region), ground);
        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density));
        Operations.completeLegacy(visitor);

        player.print(ground.getAffected() + " флора создана.");
    }

}
