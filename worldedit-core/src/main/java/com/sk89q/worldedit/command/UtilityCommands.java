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

import com.google.common.base.Joiner;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.command.util.EntityRemover;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.patterns.SingleBlockPattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.command.CommandCallable;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.PrimaryAliasComparator;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.formatting.ColorCodeBuilder;
import com.sk89q.worldedit.util.formatting.Style;
import com.sk89q.worldedit.util.formatting.StyledFragment;
import com.sk89q.worldedit.util.formatting.component.Code;
import com.sk89q.worldedit.util.formatting.component.CommandListBox;
import com.sk89q.worldedit.util.formatting.component.CommandUsageBox;
import com.sk89q.worldedit.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.sk89q.minecraft.util.commands.Logging.LogMode.PLACEMENT;

/**
 * Utility commands.
 */
public class UtilityCommands {

    private final WorldEdit we;

    public UtilityCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        aliases = { "/fill" },
        usage = "<блок> <радиус> [глубина]",
        desc = "Заполнить отверстия блоком",
        min = 2,
        max = 3
    )
    @CommandPermissions("worldedit.fill")
    @Logging(PLACEMENT)
    public void fill(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        Pattern pattern = we.getBlockPattern(player, args.getString(0));
        double radius = Math.max(1, args.getDouble(1));
        we.checkMaxRadius(radius);
        int depth = args.argsLength() > 2 ? Math.max(1, args.getInteger(2)) : 1;

        Vector pos = session.getPlacementPosition(player);
        int affected = 0;
        if (pattern instanceof SingleBlockPattern) {
            affected = editSession.fillXZ(pos,
                    ((SingleBlockPattern) pattern).getBlock(),
                    radius, depth, false);
        } else {
            affected = editSession.fillXZ(pos, pattern, radius, depth, false);
        }
        player.print(affected + " блок(ов) было создано.");
    }

    @Command(
        aliases = { "/fillr" },
        usage = "<блок> <радиус> [глубина]",
        desc = "Рекурсивно заполнить отверстия блоком",
        min = 2,
        max = 3
    )
    @CommandPermissions("worldedit.fill.recursive")
    @Logging(PLACEMENT)
    public void fillr(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        Pattern pattern = we.getBlockPattern(player, args.getString(0));
        double radius = Math.max(1, args.getDouble(1));
        we.checkMaxRadius(radius);
        int depth = args.argsLength() > 2 ? Math.max(1, args.getInteger(2)) : Integer.MAX_VALUE;

        Vector pos = session.getPlacementPosition(player);
        int affected = 0;
        if (pattern instanceof SingleBlockPattern) {
            affected = editSession.fillXZ(pos,
                    ((SingleBlockPattern) pattern).getBlock(),
                    radius, depth, true);
        } else {
            affected = editSession.fillXZ(pos, pattern, radius, depth, true);
        }
        player.print(affected + " блок(ов) было создано.");
    }

    @Command(
        aliases = { "/drain" },
        usage = "<радиус>",
        desc = "Осушить бассейн воды/лавы в радиусе",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.drain")
    @Logging(PLACEMENT)
    public void drain(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double radius = Math.max(0, args.getDouble(0));
        we.checkMaxRadius(radius);
        int affected = editSession.drainArea(
                session.getPlacementPosition(player), radius);
        player.print(affected + " блок(ов) было изменено.");
    }

    @Command(
        aliases = { "/fixlava", "fixlava" },
        usage = "<радиус>",
        desc = "Выровнять уровень лавы в радиусе",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.fixlava")
    @Logging(PLACEMENT)
    public void fixLava(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double radius = Math.max(0, args.getDouble(0));
        we.checkMaxRadius(radius);
        int affected = editSession.fixLiquid(
                session.getPlacementPosition(player), radius, 10, 11);
        player.print(affected + " блок(ов) было изменено.");
    }

    @Command(
        aliases = { "/fixwater", "fixwater" },
        usage = "<радиус>",
        desc = "Выровнять уровень воды в радиусе",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.fixwater")
    @Logging(PLACEMENT)
    public void fixWater(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double radius = Math.max(0, args.getDouble(0));
        we.checkMaxRadius(radius);
        int affected = editSession.fixLiquid(
                session.getPlacementPosition(player), radius, 8, 9);
        player.print(affected + " блок(ов) было изменено.");
    }

    @Command(
        aliases = { "/removeabove", "removeabove" },
        usage = "[размер] [высота]",
        desc = "Проделать отверстие в блоках над вашей головой.",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.removeabove")
    @Logging(PLACEMENT)
    public void removeAbove(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        
        int size = args.argsLength() > 0 ? Math.max(1, args.getInteger(0)) : 1;
        we.checkMaxRadius(size);
        World world = player.getWorld();
        int height = args.argsLength() > 1 ? Math.min((world.getMaxY() + 1), args.getInteger(1) + 2) : (world.getMaxY() + 1);

        int affected = editSession.removeAbove(
                session.getPlacementPosition(player), size, height);
        player.print(affected + " блок(ов) было удалено.");
    }

    @Command(
        aliases = { "/removebelow", "removebelow" },
        usage = "[размер] [глубина]",
        desc = "Проделать отверстие в блоках под вашими ногами.",
        min = 0,
        max = 2
    )
    @CommandPermissions("worldedit.removebelow")
    @Logging(PLACEMENT)
    public void removeBelow(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        int size = args.argsLength() > 0 ? Math.max(1, args.getInteger(0)) : 1;
        we.checkMaxRadius(size);
        World world = player.getWorld();
        int height = args.argsLength() > 1 ? Math.min((world.getMaxY() + 1), args.getInteger(1) + 2) : (world.getMaxY() + 1);

        int affected = editSession.removeBelow(session.getPlacementPosition(player), size, height);
        player.print(affected + " блок(ов) было удалено.");
    }

    @Command(
        aliases = { "/removenear", "removenear" },
        usage = "<блок> [размер]",
        desc = "Удалить блоки вокруг вас.",
        min = 1,
        max = 2
    )
    @CommandPermissions("worldedit.removenear")
    @Logging(PLACEMENT)
    public void removeNear(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        BaseBlock block = we.getBlock(player, args.getString(0), true);
        int size = Math.max(1, args.getInteger(1, 50));
        we.checkMaxRadius(size);

        int affected = editSession.removeNear(session.getPlacementPosition(player), block.getType(), size);
        player.print(affected + " блок(ов) было удалено.");
    }

    @Command(
        aliases = { "/replacenear", "replacenear" },
        usage = "<размер> <из-id> <в-id>",
        desc = "Заменить заданные блоки на блоки вокруг вас",
        flags = "f",
        min = 3,
        max = 3
    )
    @CommandPermissions("worldedit.replacenear")
    @Logging(PLACEMENT)
    public void replaceNear(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        
        int size = Math.max(1, args.getInteger(0));
        int affected;
        Set<BaseBlock> from;
        Pattern to;
        if (args.argsLength() == 2) {
            from = null;
            to = we.getBlockPattern(player, args.getString(1));
        } else {
            from = we.getBlocks(player, args.getString(1), true, !args.hasFlag('f'));
            to = we.getBlockPattern(player, args.getString(2));
        }

        Vector base = session.getPlacementPosition(player);
        Vector min = base.subtract(size, size, size);
        Vector max = base.add(size, size, size);
        Region region = new CuboidRegion(player.getWorld(), min, max);

        if (to instanceof SingleBlockPattern) {
            affected = editSession.replaceBlocks(region, from, ((SingleBlockPattern) to).getBlock());
        } else {
            affected = editSession.replaceBlocks(region, from, to);
        }
        player.print(affected + " блок(ов) было заменено.");
    }

    @Command(
        aliases = { "/snow", "snow" },
        usage = "[радиус]",
        desc = "Покрыть снегом территорию радиусом",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.snow")
    @Logging(PLACEMENT)
    public void snow(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;

        int affected = editSession.simulateSnow(session.getPlacementPosition(player), size);
        player.print(affected + " территорий покрыто снегом. Теперь зима=)");
    }

    @Command(
        aliases = {"/thaw", "thaw"},
        usage = "[радиус]",
        desc = "Убрать снег на территории радиусом",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.thaw")
    @Logging(PLACEMENT)
    public void thaw(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;

        int affected = editSession.thaw(session.getPlacementPosition(player), size);
        player.print(affected + " территории очищено от снега.");
    }

    @Command(
        aliases = { "/green", "green" },
        usage = "[радиус]",
        desc = "Озеленить (заменить грязь травой) зону",
        flags = "f",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.green")
    @Logging(PLACEMENT)
    public void green(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        final double size = args.argsLength() > 0 ? Math.max(1, args.getDouble(0)) : 10;
        final boolean onlyNormalDirt = !args.hasFlag('f');

        final int affected = editSession.green(session.getPlacementPosition(player), size, onlyNormalDirt);
        player.print(affected + " территорий озелено.");
    }

    @Command(
            aliases = { "/ex", "/ext", "/extinguish", "ex", "ext", "extinguish" },
            usage = "[радиус]",
            desc = "Потушить все пожары в радиусе",
            min = 0,
            max = 1
        )
    @CommandPermissions("worldedit.extinguish")
    @Logging(PLACEMENT)
    public void extinguish(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        int defaultRadius = config.maxRadius != -1 ? Math.min(40, config.maxRadius) : 40;
        int size = args.argsLength() > 0 ? Math.max(1, args.getInteger(0))
                : defaultRadius;
        we.checkMaxRadius(size);

        int affected = editSession.removeNear(session.getPlacementPosition(player), 51, size);
        player.print(affected + " блок(ов) было удалено.");
    }

    @Command(
        aliases = { "butcher" },
        usage = "[радиус]",
        flags = "plangbtfr",
        desc = "Убить всех враждебных мобов",
        help =
            "Убить всех враждебных мобов в радиусе.\n" +
            "Флаги:\n" +
            "  -p убить питомцев.\n" +
            "  -n убить NPC.\n" +
            "  -g убить Големов.\n" +
            "  -a убить животных.\n" +
            "  -b убить остальных мобов.\n" +
            "  -t убить мобов с именем.\n" +
            "  -f объединить все предыдущие флаги.\n" +
            "  -r сломать стойки для брони.\n" +
            "  -l ударить молнией по каждому убиваемому мобу.",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.butcher")
    @Logging(PLACEMENT)
    public void butcher(Actor actor, CommandContext args) throws WorldEditException {
        LocalConfiguration config = we.getConfiguration();
        Player player = actor instanceof Player ? (Player) actor : null;

        // technically the default can be larger than the max, but that's not my problem
        int radius = config.butcherDefaultRadius;

        // there might be a better way to do this but my brain is fried right now
        if (args.argsLength() > 0) { // user inputted radius, override the default
            radius = args.getInteger(0);
            if (radius < -1) {
                actor.printError("Используйте -1 для удаления всех сущностей в заруженных чанках");
                return;
            }
            if (config.butcherMaxRadius != -1) { // clamp if there is a max
                if (radius == -1) {
                    radius = config.butcherMaxRadius;
                } else { // Math.min does not work if radius is -1 (actually highest possible value)
                    radius = Math.min(radius, config.butcherMaxRadius);
                }
            }
        }

        CreatureButcher flags = new CreatureButcher(actor);
        flags.fromCommand(args);

        List<EntityVisitor> visitors = new ArrayList<EntityVisitor>();
        LocalSession session = null;
        EditSession editSession = null;

        if (player != null) {
            session = we.getSessionManager().get(player);
            Vector center = session.getPlacementPosition(player);
            editSession = session.createEditSession(player);
            List<? extends Entity> entities;
            if (radius >= 0) {
                CylinderRegion region = CylinderRegion.createRadius(editSession, center, radius);
                entities = editSession.getEntities(region);
            } else {
                entities = editSession.getEntities();
            }
            visitors.add(new EntityVisitor(entities.iterator(), flags.createFunction(editSession.getWorld().getWorldData().getEntityRegistry())));
        } else {
            Platform platform = we.getPlatformManager().queryCapability(Capability.WORLD_EDITING);
            for (World world : platform.getWorlds()) {
                List<? extends Entity> entities = world.getEntities();
                visitors.add(new EntityVisitor(entities.iterator(), flags.createFunction(world.getWorldData().getEntityRegistry())));
            }
        }

        int killed = 0;
        for (EntityVisitor visitor : visitors) {
            Operations.completeLegacy(visitor);
            killed += visitor.getAffected();
        }

        actor.print("Убито " + killed + (killed != 1 ? " мобов" : " моба") + (radius < 0 ? "" : " в радиусе " + radius) + ".");

        if (editSession != null) {
            session.remember(editSession);
            editSession.flushQueue();
        }
    }

    @Command(
        aliases = { "remove", "rem", "rement" },
        usage = "<тип> <радиус>",
        desc = "Удалить все сущности",
        min = 2,
        max = 2
    )
    @CommandPermissions("worldedit.remove")
    @Logging(PLACEMENT)
    public void remove(Actor actor, CommandContext args) throws WorldEditException, CommandException {
        String typeStr = args.getString(0);
        int radius = args.getInteger(1);
        Player player = actor instanceof Player ? (Player) actor : null;

        if (radius < -1) {
            actor.printError("Используйте -1 для удаления всех сущностей в заруженных чанках");
            return;
        }

        EntityRemover remover = new EntityRemover();
        remover.fromString(typeStr);

        List<EntityVisitor> visitors = new ArrayList<EntityVisitor>();
        LocalSession session = null;
        EditSession editSession = null;

        if (player != null) {
            session = we.getSessionManager().get(player);
            Vector center = session.getPlacementPosition(player);
            editSession = session.createEditSession(player);
            List<? extends Entity> entities;
            if (radius >= 0) {
                CylinderRegion region = CylinderRegion.createRadius(editSession, center, radius);
                entities = editSession.getEntities(region);
            } else {
                entities = editSession.getEntities();
            }
            visitors.add(new EntityVisitor(entities.iterator(), remover.createFunction(editSession.getWorld().getWorldData().getEntityRegistry())));
        } else {
            Platform platform = we.getPlatformManager().queryCapability(Capability.WORLD_EDITING);
            for (World world : platform.getWorlds()) {
                List<? extends Entity> entities = world.getEntities();
                visitors.add(new EntityVisitor(entities.iterator(), remover.createFunction(world.getWorldData().getEntityRegistry())));
            }
        }

        int removed = 0;
        for (EntityVisitor visitor : visitors) {
            Operations.completeLegacy(visitor);
            removed += visitor.getAffected();
        }

        actor.print("Отмеченно " + removed + (removed != 1 ? " существ" : " существо") + " для удаления.");

        if (editSession != null) {
            session.remember(editSession);
            editSession.flushQueue();
        }
    }

    @Command(
        aliases = { "/calc", "/calculate", "/eval", "/evaluate", "/solve" },
        usage = "<выражение>",
        desc = "Вычислить математическое выражение"
    )
    @CommandPermissions("worldedit.calc")
    public void calc(Actor actor, @Text String input) throws CommandException {
        try {
            Expression expression = Expression.compile(input);
            actor.print("= " + expression.evaluate());
        } catch (EvaluationException e) {
            actor.printError(String.format(
                    "'%s' не может быть разобрано как действительное выражение", input));
        } catch (ExpressionException e) {
            actor.printError(String.format(
                    "'%s' не может быть вычислено (ошибка: %s)", input, e.getMessage()));
        }
    }

    @Command(
        aliases = { "/help" },
        usage = "[<команда>]",
        desc = "Показать список всех доступных команд или помощь по определенной команде",
        min = 0,
        max = -1
    )
    @CommandPermissions("worldedit.help")
    public void help(Actor actor, CommandContext args) throws WorldEditException {
        help(args, we, actor);
    }

    private static CommandMapping detectCommand(Dispatcher dispatcher, String command, boolean isRootLevel) {
        CommandMapping mapping;

        // First try the command as entered
        mapping = dispatcher.get(command);
        if (mapping != null) {
            return mapping;
        }

        // Then if we're looking at root commands and the user didn't use
        // any slashes, let's try double slashes and then single slashes.
        // However, be aware that there exists different single slash
        // and double slash commands in WorldEdit
        if (isRootLevel && !command.contains("/")) {
            mapping = dispatcher.get("//" + command);
            if (mapping != null) {
                return mapping;
            }

            mapping = dispatcher.get("/" + command);
            if (mapping != null) {
                return mapping;
            }
        }

        return null;
    }

    public static void help(CommandContext args, WorldEdit we, Actor actor) {
        CommandCallable callable = we.getPlatformManager().getCommandManager().getDispatcher();

        int page = 0;
        final int perPage = actor instanceof Player ? 8 : 20; // More pages for console
        int effectiveLength = args.argsLength();

        // Detect page from args
        try {
            if (args.argsLength() > 0) {
                page = args.getInteger(args.argsLength() - 1);
                if (page <= 0) {
                    page = 1;
                } else {
                    page--;
                }

                effectiveLength--;
            }
        } catch (NumberFormatException ignored) {
        }

        boolean isRootLevel = true;
        List<String> visited = new ArrayList<String>();

        // Drill down to the command
        for (int i = 0; i < effectiveLength; i++) {
            String command = args.getString(i);

            if (callable instanceof Dispatcher) {
                // Chop off the beginning / if we're are the root level
                if (isRootLevel && command.length() > 1 && command.charAt(0) == '/') {
                    command = command.substring(1);
                }

                CommandMapping mapping = detectCommand((Dispatcher) callable, command, isRootLevel);
                if (mapping != null) {
                    callable = mapping.getCallable();
                } else {
                    if (isRootLevel) {
                        actor.printError(String.format("Команда '%s' не найдена.", args.getString(i)));
                        return;
                    } else {
                        actor.printError(String.format("Суб-команда '%s' в '%s' не найдена.",
                                command, Joiner.on(" ").join(visited)));
                        return;
                    }
                }

                visited.add(args.getString(i));
                isRootLevel = false;
            } else {
                actor.printError(String.format("'%s' не имеет суб-команд. (Может быть, '%s' это для параметра?)",
                        Joiner.on(" ").join(visited), command));
                return;
            }
        }

        // Create the message
        if (callable instanceof Dispatcher) {
            Dispatcher dispatcher = (Dispatcher) callable;

            // Get a list of aliases
            List<CommandMapping> aliases = new ArrayList<CommandMapping>(dispatcher.getCommands());
            Collections.sort(aliases, new PrimaryAliasComparator(CommandManager.COMMAND_CLEAN_PATTERN));

            // Calculate pagination
            int offset = perPage * page;
            int pageTotal = (int) Math.ceil(aliases.size() / (double) perPage);

            // Box
            CommandListBox box = new CommandListBox(String.format("Страница помощи: стр. %d/%d ", page + 1, pageTotal));
            StyledFragment contents = box.getContents();
            StyledFragment tip = contents.createFragment(Style.GRAY);

            if (offset >= aliases.size()) {
                tip.createFragment(Style.RED).append(String.format("Страница %d не найдена (общее число страниц %d).", page + 1, pageTotal)).newLine();
            } else {
                List<CommandMapping> list = aliases.subList(offset, Math.min(offset + perPage, aliases.size()));

                tip.append("Введите ");
                tip.append(new Code().append("//help ").append("<команда> [<страница>]"));
                tip.append(" для получения полной информации.").newLine();

                // Add each command
                for (CommandMapping mapping : list) {
                    StringBuilder builder = new StringBuilder();
                    if (isRootLevel) {
                        builder.append("/");
                    }
                    if (!visited.isEmpty()) {
                        builder.append(Joiner.on(" ").join(visited));
                        builder.append(" ");
                    }
                    builder.append(mapping.getPrimaryAlias());
                    box.appendCommand(builder.toString(), mapping.getDescription().getDescription());
                }
            }

            actor.printRaw(ColorCodeBuilder.asColorCodes(box));
        } else {
            CommandUsageBox box = new CommandUsageBox(callable, Joiner.on(" ").join(visited));
            actor.printRaw(ColorCodeBuilder.asColorCodes(box));
        }
    }

}
