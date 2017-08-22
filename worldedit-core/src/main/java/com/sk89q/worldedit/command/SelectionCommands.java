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

import com.google.common.base.Optional;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.ConvexPolyhedralRegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.CylinderRegionSelector;
import com.sk89q.worldedit.regions.selector.EllipsoidRegionSelector;
import com.sk89q.worldedit.regions.selector.ExtendingCuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;
import com.sk89q.worldedit.regions.selector.SphereRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.formatting.ColorCodeBuilder;
import com.sk89q.worldedit.util.formatting.Style;
import com.sk89q.worldedit.util.formatting.StyledFragment;
import com.sk89q.worldedit.util.formatting.component.CommandListBox;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.storage.ChunkStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.sk89q.minecraft.util.commands.Logging.LogMode.POSITION;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;

/**
 * Selection commands.
 */
public class SelectionCommands {

    private final WorldEdit we;
    
    public SelectionCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        aliases = { "/pos1" },
        usage = "[координаты]",
        desc = "Установить первую позицию",
        min = 0,
        max = 1
    )
    @Logging(POSITION)
    @CommandPermissions("worldedit.selection.pos")
    public void pos1(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        Vector pos;

        if (args.argsLength() == 1) {
            if (args.getString(0).matches("-?\\d+,-?\\d+,-?\\d+")) {
                String[] coords = args.getString(0).split(",");
                pos = new Vector(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
            } else {
                player.printError("Неверные координаты " + args.getString(0));
                return;
            }
        } else {
            pos = player.getBlockIn();
        }

        if (!session.getRegionSelector(player.getWorld()).selectPrimary(pos, ActorSelectorLimits.forActor(player))) {
            player.printError("Позиция 1 уже установлена.");
            return;
        }

        session.getRegionSelector(player.getWorld())
                .explainPrimarySelection(player, session, pos);
    }

    @Command(
        aliases = { "/pos2" },
        usage = "[координаты]",
        desc = "Установить вторую позицию",
        min = 0,
        max = 1
    )
    @Logging(POSITION)
    @CommandPermissions("worldedit.selection.pos")
    public void pos2(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        Vector pos;
        if (args.argsLength() == 1) {
            if (args.getString(0).matches("-?\\d+,-?\\d+,-?\\d+")) {
                String[] coords = args.getString(0).split(",");
                pos = new Vector(Integer.parseInt(coords[0]),
                        Integer.parseInt(coords[1]),
                        Integer.parseInt(coords[2]));
            } else {
                player.printError("Неверные координаты " + args.getString(0));
                return;
            }
        } else {
            pos = player.getBlockIn();
        }

        if (!session.getRegionSelector(player.getWorld()).selectSecondary(pos, ActorSelectorLimits.forActor(player))) {
            player.printError("Позиция 2 уже установлена.");
            return;
        }

        session.getRegionSelector(player.getWorld())
                .explainSecondarySelection(player, session, pos);
    }

    @Command(
        aliases = { "/hpos1" },
        usage = "",
        desc = "Установить первую позицию на блок, на который смотрите",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.selection.hpos")
    public void hpos1(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        
        Vector pos = player.getBlockTrace(300);

        if (pos != null) {
            if (!session.getRegionSelector(player.getWorld()).selectPrimary(pos, ActorSelectorLimits.forActor(player))) {
                player.printError("Позиция 1 уже установлена.");
                return;
            }

            session.getRegionSelector(player.getWorld())
                    .explainPrimarySelection(player, session, pos);
        } else {
            player.printError("Нет блока в поле зрения!");
        }
    }

    @Command(
        aliases = { "/hpos2" },
        usage = "",
        desc = "Установить первую позицию на блок, на который смотрите",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.selection.hpos")
    public void hpos2(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        
        Vector pos = player.getBlockTrace(300);

        if (pos != null) {
            if (!session.getRegionSelector(player.getWorld()).selectSecondary(pos, ActorSelectorLimits.forActor(player))) {
                player.printError("Позиция 2 уже установлена.");
                return;
            }

            session.getRegionSelector(player.getWorld())
                    .explainSecondarySelection(player, session, pos);
        } else {
            player.printError("Нет блока в поле зрения!");
        }
    }

    @Command(
        aliases = { "/chunk" },
        usage = "[координаты x,z]",
        flags = "sc",
        desc = "Выделить чанк, где вы находитесь.",
        help =
            "Выделить чанк, где вы находитесь.\n" +
            "С флагом -s flag, ваше выделение расширяется, чтобы\n" +
            "все чанки, чьи части захватывает ваше выделение.\n\n" +
            "Указаные координаты будут использованы вместо вашего\n"+
            "текущего положения. Используйте -c для указания координат чанка,\n" +
            "в противном случае будут подразумеваться полные координаты.\n" +
            "(например, координаты 5,5 совпадают с -c 0,0)",
        min = 0,
        max = 1
    )
    @Logging(POSITION)
    @CommandPermissions("worldedit.selection.chunk")
    public void chunk(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        final Vector min;
        final Vector max;
        final World world = player.getWorld();
        if (args.hasFlag('s')) {
            Region region = session.getSelection(world);

            final Vector2D min2D = ChunkStore.toChunk(region.getMinimumPoint());
            final Vector2D max2D = ChunkStore.toChunk(region.getMaximumPoint());

            min = new Vector(min2D.getBlockX() * 16, 0, min2D.getBlockZ() * 16);
            max = new Vector(max2D.getBlockX() * 16 + 15, world.getMaxY(), max2D.getBlockZ() * 16 + 15);

            player.print("Выделенный чанк: ("
                    + min2D.getBlockX() + ", " + min2D.getBlockZ() + ") - ("
                    + max2D.getBlockX() + ", " + max2D.getBlockZ() + ")");
        } else {
            final Vector2D min2D;
            if (args.argsLength() == 1) {
                // coords specified
                String[] coords = args.getString(0).split(",");
                if (coords.length != 2) {
                    throw new InsufficientArgumentsException("Неправильно указаны коардинаты чанка.");
                }
                int x = Integer.parseInt(coords[0]);
                int z = Integer.parseInt(coords[1]);
                Vector2D pos = new Vector2D(x, z);
                min2D = (args.hasFlag('c')) ? pos : ChunkStore.toChunk(pos.toVector());
            } else {
                // use player loc
                min2D = ChunkStore.toChunk(player.getBlockIn());
            }

            min = new Vector(min2D.getBlockX() * 16, 0, min2D.getBlockZ() * 16);
            max = min.add(15, world.getMaxY(), 15);

            player.print("Выделенный чанк: "
                    + min2D.getBlockX() + ", " + min2D.getBlockZ());
        }

        final CuboidRegionSelector selector;
        if (session.getRegionSelector(world) instanceof ExtendingCuboidRegionSelector) {
            selector = new ExtendingCuboidRegionSelector(world);
        } else {
            selector = new CuboidRegionSelector(world);
        }
        selector.selectPrimary(min, ActorSelectorLimits.forActor(player));
        selector.selectSecondary(max, ActorSelectorLimits.forActor(player));
        session.setRegionSelector(world, selector);

        session.dispatchCUISelection(player);

    }

    @Command(
        aliases = { "/wand" },
        usage = "",
        desc = "Получить предмет для выделения",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.wand")
    public void wand(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        player.giveItem(we.getConfiguration().wandItem, 1);
        player.print("ЛКМ - выделить точку #1; ПКМ - выделить точку #2");
    }

    @Command(
        aliases = { "toggleeditwand" },
        usage = "",
        desc = "Переключить функциональность предмета для выделения позиции",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.wand.toggle")
    public void toggleWand(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        
        session.setToolControl(!session.isToolControlEnabled());

        if (session.isToolControlEnabled()) {
            player.print("Предмет выделения включен.");
        } else {
            player.print("Предмет выделения выключен.");
        }
    }

    @Command(
        aliases = { "/expand" },
        usage = "<количество> [reverse-amount] <направление>",
        desc = "Расширить выделенную область",
        min = 1,
        max = 3
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.expand")
    public void expand(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        // Special syntax (//expand vert) to expand the selection between
        // sky and bedrock.
        if (args.getString(0).equalsIgnoreCase("vert")
                || args.getString(0).equalsIgnoreCase("vertical")) {
            Region region = session.getSelection(player.getWorld());
            try {
                int oldSize = region.getArea();
                region.expand(
                        new Vector(0, (player.getWorld().getMaxY() + 1), 0),
                        new Vector(0, -(player.getWorld().getMaxY() + 1), 0));
                session.getRegionSelector(player.getWorld()).learnChanges();
                int newSize = region.getArea();
                session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);
                player.print("Регион увеличен на " + (newSize - oldSize)
                        + " блоков [сверху к низу].");
            } catch (RegionOperationException e) {
                player.printError(e.getMessage());
            }

            return;
        }

        List<Vector> dirs = new ArrayList<Vector>();
        int change = args.getInteger(0);
        int reverseChange = 0;

        switch (args.argsLength()) {
        case 2:
            // Either a reverse amount or a direction
            try {
                reverseChange = args.getInteger(1);
                dirs.add(we.getDirection(player, "me"));
            } catch (NumberFormatException e) {
                if (args.getString(1).contains(",")) {
                    String[] split = args.getString(1).split(",");
                    for (String s : split) {
                        dirs.add(we.getDirection(player, s.toLowerCase()));
                    }
                } else {
                    dirs.add(we.getDirection(player, args.getString(1).toLowerCase()));
                }
            }
            break;

        case 3:
            // Both reverse amount and direction
            reverseChange = args.getInteger(1);
            if (args.getString(2).contains(",")) {
                String[] split = args.getString(2).split(",");
                for (String s : split) {
                    dirs.add(we.getDirection(player, s.toLowerCase()));
                }
            } else {
                dirs.add(we.getDirection(player, args.getString(2).toLowerCase()));
            }
            break;

        default:
            dirs.add(we.getDirection(player, "me"));
            break;

        }

        Region region = session.getSelection(player.getWorld());
        int oldSize = region.getArea();

        if (reverseChange == 0) {
            for (Vector dir : dirs) {
                region.expand(dir.multiply(change));
            }
        } else {
            for (Vector dir : dirs) {
                region.expand(dir.multiply(change), dir.multiply(-reverseChange));
            }
        }

        session.getRegionSelector(player.getWorld()).learnChanges();
        int newSize = region.getArea();
        
        session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);

        player.print("Регион увеличен на " + (newSize - oldSize) + " блоков.");
    }

    @Command(
        aliases = { "/contract" },
        usage = "<количество> [reverse-amount] [направление]",
        desc = "Уменьшить выбранный регион",
        min = 1,
        max = 3
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.contract")
    public void contract(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        List<Vector> dirs = new ArrayList<Vector>();
        int change = args.getInteger(0);
        int reverseChange = 0;

        switch (args.argsLength()) {
        case 2:
            // Either a reverse amount or a direction
            try {
                reverseChange = args.getInteger(1);
                dirs.add(we.getDirection(player, "me"));
            } catch (NumberFormatException e) {
                if (args.getString(1).contains(",")) {
                    String[] split = args.getString(1).split(",");
                    for (String s : split) {
                        dirs.add(we.getDirection(player, s.toLowerCase()));
                    }
                } else {
                    dirs.add(we.getDirection(player, args.getString(1).toLowerCase()));
                }
            }
            break;

        case 3:
            // Both reverse amount and direction
            reverseChange = args.getInteger(1);
            if (args.getString(2).contains(",")) {
                String[] split = args.getString(2).split(",");
                for (String s : split) {
                    dirs.add(we.getDirection(player, s.toLowerCase()));
                }
            } else {
                dirs.add(we.getDirection(player, args.getString(2).toLowerCase()));
            }
            break;

        default:
            dirs.add(we.getDirection(player, "me"));
            break;
        }

        try {
            Region region = session.getSelection(player.getWorld());
            int oldSize = region.getArea();
            if (reverseChange == 0) {
                for (Vector dir : dirs) {
                    region.contract(dir.multiply(change));
                }
            } else {
                for (Vector dir : dirs) {
                    region.contract(dir.multiply(change), dir.multiply(-reverseChange));
                }
            }
            session.getRegionSelector(player.getWorld()).learnChanges();
            int newSize = region.getArea();
            
            session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);


            player.print("Регион уменьшен на " + (oldSize - newSize) + " блоков.");
        } catch (RegionOperationException e) {
            player.printError(e.getMessage());
        }
    }

    @Command(
        aliases = { "/shift" },
        usage = "<количество> [направление]",
        desc = "Переместить выделенную область",
        min = 1,
        max = 2
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.shift")
    public void shift(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        List<Vector> dirs = new ArrayList<Vector>();
        int change = args.getInteger(0);
        if (args.argsLength() == 2) {
            if (args.getString(1).contains(",")) {
                for (String s : args.getString(1).split(",")) {
                    dirs.add(we.getDirection(player, s.toLowerCase()));
                }
            } else {
                dirs.add(we.getDirection(player, args.getString(1).toLowerCase()));
            }
        } else {
            dirs.add(we.getDirection(player, "me"));
        }

        try {
            Region region = session.getSelection(player.getWorld());

            for (Vector dir : dirs) {
                region.shift(dir.multiply(change));
            }

            session.getRegionSelector(player.getWorld()).learnChanges();

            session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);

            player.print("Регион перемещен.");
        } catch (RegionOperationException e) {
            player.printError(e.getMessage());
        }
    }

    @Command(
        aliases = { "/outset" },
        usage = "<количество>",
        desc = "Расширить выделенную область",
        help =
            "Расширяет выделение во всех направлениях.\n" +
            "Флаги:\n" +
            "  -h раширить только по горизонтале\n" +
            "  -v расширить только по вертикали\n",
        flags = "hv",
        min = 1,
        max = 1
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.outset")
    public void outset(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        Region region = session.getSelection(player.getWorld());
        region.expand(getChangesForEachDir(args));
        session.getRegionSelector(player.getWorld()).learnChanges();
        session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);
        player.print("Регион расширен во всех направлениях.");
    }

    @Command(
        aliases = { "/inset" },
        usage = "<количество>",
        desc = "Сузить выбранный регион",
        help =
            "Сужает выделение во всех направлениях.\n" +
            "Флаги:\n" +
            "  -h сузить только по горизонтали\n" +
            "  -v сузить только по вертикали\n",
        flags = "hv",
        min = 1,
        max = 1
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.inset")
    public void inset(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        Region region = session.getSelection(player.getWorld());
        region.contract(getChangesForEachDir(args));
        session.getRegionSelector(player.getWorld()).learnChanges();
        session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);
        player.print("Регион сужен.");
    }

    private Vector[] getChangesForEachDir(CommandContext args) {
        List<Vector> changes = new ArrayList<Vector>(6);
        int change = args.getInteger(0);

        if (!args.hasFlag('h')) {
            changes.add((new Vector(0, 1, 0)).multiply(change));
            changes.add((new Vector(0, -1, 0)).multiply(change));
        }

        if (!args.hasFlag('v')) {
            changes.add((new Vector(1, 0, 0)).multiply(change));
            changes.add((new Vector(-1, 0, 0)).multiply(change));
            changes.add((new Vector(0, 0, 1)).multiply(change));
            changes.add((new Vector(0, 0, -1)).multiply(change));
        }

        return changes.toArray(new Vector[0]);
    }

    @Command(
        aliases = { "/size" },
        flags = "c",
        usage = "",
        desc = "Получить информацию о размере выделении",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.selection.size")
    public void size(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        
        if (args.hasFlag('c')) {
            ClipboardHolder holder = session.getClipboard();
            Clipboard clipboard = holder.getClipboard();
            Region region = clipboard.getRegion();
            Vector size = region.getMaximumPoint().subtract(region.getMinimumPoint());
            Vector origin = clipboard.getOrigin();

            player.print("Размер: " + size);
            player.print("Смещение: " + origin);
            player.print("Расстояние кубов: " + size.distance(Vector.ONE));
            player.print("Количество блоков: " + (int) (size.getX() * size.getY() * size.getZ()));
            return;
        }
        
        Region region = session.getSelection(player.getWorld());
        Vector size = region.getMaximumPoint()
                .subtract(region.getMinimumPoint())
                .add(1, 1, 1);
        
        player.print("Тип: " + session.getRegionSelector(player.getWorld())
                .getTypeName());
        
        for (String line : session.getRegionSelector(player.getWorld())
                .getInformationLines()) {
            player.print(line);
        }
        player.print("Размер: " + size);
        player.print("Расстояние кубов: " + region.getMaximumPoint().distance(region.getMinimumPoint()));
        player.print("Количество блоков: " + region.getArea());
    }


    @Command(
        aliases = { "/count" },
        usage = "<блок>",
        desc = "Показать количество определенных блоков",
        flags = "d",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.analysis.count")
    public void count(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        boolean useData = args.hasFlag('d');
        if (args.getString(0).contains(":")) {
            useData = true; //override d flag, if they specified data they want it
        }
        if (useData) {
            Set<BaseBlock> searchBlocks = we.getBlocks(player, args.getString(0), true);
            int count = editSession.countBlocks(session.getSelection(player.getWorld()), searchBlocks);
            player.print("Определенных блоков: " + count);
        } else {
            Set<Integer> searchIDs = we.getBlockIDs(player, args.getString(0), true);
            int count = editSession.countBlock(session.getSelection(player.getWorld()), searchIDs);
            player.print("Определенных блоков: " + count);
        }
    }

    @Command(
        aliases = { "/distr" },
        usage = "",
        desc = "Показать количество блоков в выделении",
        help =
            "Показать распределение блоков в выделении.\n" +
            "-c показать распределение блоков в буфере обмена.\n" +
            "-d разделяет одинаковые типы блоков с разными данными (типы древесины, цвета шерсти и т.д)",
        flags = "cd",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.analysis.distr")
    public void distr(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException, CommandException {

        int size;
        boolean useData = args.hasFlag('d');
        List<Countable<Integer>> distribution = null;
        List<Countable<BaseBlock>> distributionData = null;

        if (args.hasFlag('c')) {
            // TODO: Update for new clipboard
            throw new CommandException("Должно быть переписано еще раз");
        } else {
            if (useData) {
                distributionData = editSession.getBlockDistributionWithData(session.getSelection(player.getWorld()));
            } else {
                distribution = editSession.getBlockDistribution(session.getSelection(player.getWorld()));
            }
            size = session.getSelection(player.getWorld()).getArea();
        }

        if ((useData && distributionData.size() <= 0)
                || (!useData && distribution.size() <= 0)) {  // *Should* always be false
            player.printError("Не найдено таких блоков.");
            return;
        }

        player.print("# всего блоков: " + size);

        if (useData) {
            for (Countable<BaseBlock> c : distributionData) {
                String name = BlockType.fromID(c.getID().getId()).getName();
                String str = String.format("%-7s (%.3f%%) %s #%d:%d",
                        String.valueOf(c.getAmount()),
                        c.getAmount() / (double) size * 100,
                        name == null ? "Неизвестно" : name,
                        c.getID().getType(), c.getID().getData());
                player.print(str);
            }
        } else {
            for (Countable<Integer> c : distribution) {
                BlockType block = BlockType.fromID(c.getID());
                String str = String.format("%-7s (%.3f%%) %s #%d",
                        String.valueOf(c.getAmount()),
                        c.getAmount() / (double) size * 100,
                        block == null ? "Неизвестно" : block.getName(), c.getID());
                player.print(str);
            }
        }
    }

    @Command(
        aliases = { "/sel", ";", "/desel", "/deselect" },
        flags = "d",
        usage = "[cuboid|extend|poly|ellipsoid|sphere|cyl|convex]",
        desc = "Выбрать тип выделения (кубоид, сфера и т.д.)",
        min = 0,
        max = 1
    )
    public void select(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        final World world = player.getWorld();
        if (args.argsLength() == 0) {
            session.getRegionSelector(world).clear();
            session.dispatchCUISelection(player);
            player.print("Выделение очищено.");
            return;
        }

        final String typeName = args.getString(0);
        final RegionSelector oldSelector = session.getRegionSelector(world);

        final RegionSelector selector;
        if (typeName.equalsIgnoreCase("cuboid")) {
            selector = new CuboidRegionSelector(oldSelector);
            player.print("Кубоид: ЛКМ для точки 1, ПКМ для точки 2");
        } else if (typeName.equalsIgnoreCase("extend")) {
            selector = new ExtendingCuboidRegionSelector(oldSelector);
            player.print("Кубоид: ЛКМ для начальной точки, ПКМ, чтобы расширить");
        } else if (typeName.equalsIgnoreCase("poly")) {
            selector = new Polygonal2DRegionSelector(oldSelector);
            player.print("2D полигон: ЛКМ/ПКМ чтобы добавить точку.");
            Optional<Integer> limit = ActorSelectorLimits.forActor(player).getPolygonVertexLimit();
            if (limit.isPresent()) {
                player.print(limit.get() + " точек максимум.");
            }
        } else if (typeName.equalsIgnoreCase("ellipsoid")) {
            selector = new EllipsoidRegionSelector(oldSelector);
            player.print("Элипсоид: ЛКМ=центр, ПКМ, чтобы расширить");
        } else if (typeName.equalsIgnoreCase("sphere")) {
            selector = new SphereRegionSelector(oldSelector);
            player.print("Сфера: ЛКМ=центр, ПКМ, чтобы установить радиус");
        } else if (typeName.equalsIgnoreCase("cyl")) {
            selector = new CylinderRegionSelector(oldSelector);
            player.print("Цилиндр: ЛКМ=центр, ПКМ, чтобы расширить.");
        } else if (typeName.equalsIgnoreCase("convex") || typeName.equalsIgnoreCase("hull") || typeName.equalsIgnoreCase("polyhedron")) {
            selector = new ConvexPolyhedralRegionSelector(oldSelector);
            player.print("Выпуклый полиэдральное выделение: ЛКМ=Первая вершина, ПКМ, чтобы расширить.");
            Optional<Integer> limit = ActorSelectorLimits.forActor(player).getPolyhedronVertexLimit();
            if (limit.isPresent()) {
                player.print(limit.get() + " максимальное количество очков.");
            }
        } else {
            CommandListBox box = new CommandListBox("Режимы выбора");
            StyledFragment contents = box.getContents();
            StyledFragment tip = contents.createFragment(Style.RED);
            tip.append("Выберите один из перечисленных ниже режимов:").newLine();

            box.appendCommand("cuboid", "Выберите два угла прямоугольного кубоид");
            box.appendCommand("extend", "Режим быстрого выбора кубоида");
            box.appendCommand("poly", "Выберите 2D многоугольник с высоты");
            box.appendCommand("ellipsoid", "Выбрать эллипсоид");
            box.appendCommand("sphere", "Выбрать сферу");
            box.appendCommand("cyl", "Выбрать цилиндр");
            box.appendCommand("convex", "Выберите выпуклый многогранник");

            player.printRaw(ColorCodeBuilder.asColorCodes(box));
            return;
        }

        if (args.hasFlag('d')) {
            RegionSelectorType found = null;
            for (RegionSelectorType type : RegionSelectorType.values()) {
                if (type.getSelectorClass() == selector.getClass()) {
                    found = type;
                    break;
                }
            }

            if (found != null) {
                session.setDefaultRegionSelector(found);
                player.print("Ваш регион выбран по умолчанию " + found.name() + ".");
            } else {
                throw new RuntimeException("Что-то пошло не так. Пожалуйста, сообщите об этом.");
            }
        }

        session.setRegionSelector(world, selector);
        session.dispatchCUISelection(player);
    }

}
