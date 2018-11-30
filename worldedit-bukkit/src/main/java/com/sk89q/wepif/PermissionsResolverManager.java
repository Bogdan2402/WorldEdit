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

package com.sk89q.wepif;

import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PermissionsResolverManager implements PermissionsResolver {

    private static final String CONFIG_HEADER = "#\r\n" +
            "# Конфигурационный файл WEPIF\r\n" +
            "#\r\n" +
            "# Этот файл обрабатывает конфигурацию разрешений для каждого плагина, используя WEPIF\r\n" +
            "#\r\n" +
            "# О редактировании этого файла:\r\n" +
            "# - НЕ ИСПОЛЬЗОВАТЬ TAB. Вы должны использовать пробелы иначе Bukkit будет жаловаться. Если\r\n" +
            "#   вы используете редактор, такой как Notepad++ (рекомендуется для пользователей Windows), вы\r\n" +
            "#   должны настроить его на \"заменить TAB на пробелы.\" В Notepad++, это можно\r\n" +
            "#   изменить в Опции > Настройки > Настройки табуляци.\r\n" +
            "# - Не избавиляйтесь от отступов. Они смещены, поэтому некоторые записи\r\n" +
            "#   в категориях (как \"enforce-single-session\" находится в категории \"protection\")\r\n" +
            "# - Если вы хотите проверить формат этого файла, прежде чем положить его\r\n" +
            "#   в WEPIF, вставьте его в http://yaml-online-parser.appspot.com/\r\n" +
            "#   и посмотрите полученные \"ОШИБКА:\".\r\n" +
            "# - Строки, начинающие с # называются комментариями, поэтому они игнорируются.\r\n" +
            "#\r\n" +
            "# О разрешениях конфигурации\r\n" +
            "# - Смотрите http://wiki.sk89q.com/wiki/WorldEdit/Permissions/Bukkit\r\n" +
            "# - Теперь с поддержкой мультимиров (смотрите пример)\r\n" +
            "\r\n";

    private static PermissionsResolverManager instance;

    public static void initialize(Plugin plugin) {
        if (!isInitialized()) {
            instance = new PermissionsResolverManager(plugin);
        }
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static PermissionsResolverManager getInstance() {
        if (!isInitialized()) {
            throw new WEPIFRuntimeException("WEPIF еще не был инициализирован!");
        }
        return instance;
    }

    private Server server;
    private PermissionsResolver permissionResolver;
    private YAMLProcessor config;
    private Logger logger = Logger.getLogger(getClass().getCanonicalName());
    private List<Class<? extends PermissionsResolver>> enabledResolvers = new ArrayList<>();

    @SuppressWarnings("unchecked")
    protected Class<? extends PermissionsResolver>[] availableResolvers = new Class[] {
            PluginPermissionsResolver.class,
            PermissionsExResolver.class,
            bPermissionsResolver.class,
            GroupManagerResolver.class,
            NijiPermissionsResolver.class,
            VaultResolver.class,
            DinnerPermsResolver.class,
            FlatFilePermissionsResolver.class
    };

    protected PermissionsResolverManager(Plugin plugin) {
        this.server = plugin.getServer();
        (new ServerListener()).register(plugin); // Register the events

        loadConfig(new File("wepif.yml"));
        findResolver();
    }

    public void findResolver() {
        for (Class<? extends PermissionsResolver> resolverClass : enabledResolvers) {
            try {
                Method factoryMethod = resolverClass.getMethod("factory", Server.class, YAMLProcessor.class);

                this.permissionResolver = (PermissionsResolver) factoryMethod.invoke(null, this.server, this.config);

                if (this.permissionResolver != null) {
                    break;
                }
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Ошибка в фабричном методе для " + resolverClass.getSimpleName(), e);
                continue;
            }
        }
        if (permissionResolver == null) {
            permissionResolver = new ConfigurationPermissionsResolver(config);
        }
        permissionResolver.load();
        logger.info("WEPIF: " + permissionResolver.getDetectionMessage());
    }

    public void setPluginPermissionsResolver(Plugin plugin) {
        if (!(plugin instanceof PermissionsProvider)) {
            return;
        }

        permissionResolver = new PluginPermissionsResolver((PermissionsProvider) plugin, plugin);
        logger.info("WEPIF: " + permissionResolver.getDetectionMessage());
    }

    @Override
    public void load() {
        findResolver();
    }

    @Override
    public boolean hasPermission(String name, String permission) {
        return permissionResolver.hasPermission(name, permission);
    }

    @Override
    public boolean hasPermission(String worldName, String name, String permission) {
        return permissionResolver.hasPermission(worldName, name, permission);
    }

    @Override
    public boolean inGroup(String player, String group) {
        return permissionResolver.inGroup(player, group);
    }

    @Override
    public String[] getGroups(String player) {
        return permissionResolver.getGroups(player);
    }

    @Override
    public boolean hasPermission(OfflinePlayer player, String permission) {
        return permissionResolver.hasPermission(player, permission);
    }

    @Override
    public boolean hasPermission(String worldName, OfflinePlayer player, String permission) {
        return permissionResolver.hasPermission(worldName, player, permission);
    }

    @Override
    public boolean inGroup(OfflinePlayer player, String group) {
        return permissionResolver.inGroup(player, group);
    }

    @Override
    public String[] getGroups(OfflinePlayer player) {
        return permissionResolver.getGroups(player);
    }

    @Override
    public String getDetectionMessage() {
        return "Использую WEPIF для разрешений";
    }

    private boolean loadConfig(File file) {
        boolean isUpdated = false;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Не удалось создать новый файл конфигурации", e);
            }
        }
        config = new YAMLProcessor(file, false, YAMLFormat.EXTENDED);
        try {
            config.load();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Ошибка загрузки конфигурации WEPIF", e);
        }
        List<String> keys = config.getKeys(null);
        config.setHeader(CONFIG_HEADER);

        if (!keys.contains("ignore-nijiperms-bridges")) {
            config.setProperty("ignore-nijiperms-bridges", true);
            isUpdated = true;
        }

        if (!keys.contains("resolvers")) {
            //List<String> resolverKeys = config.getKeys("resolvers");
            List<String> resolvers = new ArrayList<>();
            for (Class<?> clazz : availableResolvers) {
                resolvers.add(clazz.getSimpleName());
            }
            enabledResolvers.addAll(Arrays.asList(availableResolvers));
            config.setProperty("resolvers.enabled", resolvers);
            isUpdated = true;
        } else {
            List<String> disabledResolvers = config.getStringList("resolvers.disabled", new ArrayList<>());
            List<String> stagedEnabled = config.getStringList("resolvers.enabled", null);
            for (Iterator<String> i = stagedEnabled.iterator(); i.hasNext();) {
                String nextName = i.next();
                Class<?> next = null;
                try {
                    next = Class.forName(getClass().getPackage().getName() + "." + nextName);
                } catch (ClassNotFoundException e) {}

                if (next == null || !PermissionsResolver.class.isAssignableFrom(next)) {
                    logger.warning("WEPIF: Недопустимый или неизвестный класс включен в распознаватели: "
                            + nextName + ". Перемещение в отключенный список решающих устройств.");
                    i.remove();
                    disabledResolvers.add(nextName);
                    isUpdated = true;
                    continue;
                }
                enabledResolvers.add(next.asSubclass(PermissionsResolver.class));
            }

            for (Class<?> clazz : availableResolvers) {
                if (!stagedEnabled.contains(clazz.getSimpleName()) &&
                        !disabledResolvers.contains(clazz.getSimpleName())) {
                    disabledResolvers.add(clazz.getSimpleName());
                    logger.info("Новые разрешения разрешены: "
                            + clazz.getSimpleName() + " обнаружено. " +
                            "Добавление в отключенный список решающих устройств.");
                    isUpdated = true;
                }
            }
            config.setProperty("resolvers.disabled", disabledResolvers);
            config.setProperty("resolvers.enabled", stagedEnabled);
        }

        if (keys.contains("dinner-perms") || keys.contains("dinnerperms")) {
            config.removeProperty("dinner-perms");
            config.removeProperty("dinnerperms");
            isUpdated = true;
        }
        if (!keys.contains("permissions")) {
            ConfigurationPermissionsResolver.generateDefaultPerms(
                    config.addNode("permissions"));
            isUpdated = true;
        }
        if (isUpdated) {
            logger.info("WEPIF: Обновление конфигурационного файла");
            config.save();
        }
        return isUpdated;
    }

    public static class MissingPluginException extends Exception {
    }

    class ServerListener implements org.bukkit.event.Listener {
        @EventHandler
        public void onPluginEnable(PluginEnableEvent event) {
            Plugin plugin = event.getPlugin();
            String name = plugin.getDescription().getName();
            if (plugin instanceof PermissionsProvider) {
                setPluginPermissionsResolver(plugin);
            } else if ("permissions".equalsIgnoreCase(name) || "permissionsex".equalsIgnoreCase(name)
                    || "bpermissions".equalsIgnoreCase(name) || "groupmanager".equalsIgnoreCase(name)
                    || "vault".equalsIgnoreCase(name)) {
                load();
            }
        }

        @EventHandler
        public void onPluginDisable(PluginDisableEvent event) {
            String name = event.getPlugin().getDescription().getName();

            if (event.getPlugin() instanceof PermissionsProvider
                    || "permissions".equalsIgnoreCase(name) || "permissionsex".equalsIgnoreCase(name)
                    || "bpermissions".equalsIgnoreCase(name) || "groupmanager".equalsIgnoreCase(name)
                    || "vault".equalsIgnoreCase(name)) {
                load();
            }
        }

        void register(Plugin plugin) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

}
