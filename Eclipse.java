package dev.eclipse.client;

import dev.eclipse.config.ConfigManager;
import dev.eclipse.module.ModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Punkt dostępu do singletonów klienta. */
public final class Eclipse {
    public static final String NAME = "Eclipse Client";
    public static final Logger LOGGER = LoggerFactory.getLogger("Eclipse");

    private static final ModuleManager MODULES = new ModuleManager();
    private static final UiSettings UI = new UiSettings();
    private static final Notifications NOTIFICATIONS = new Notifications();
    private static final ConfigManager CONFIG = new ConfigManager(MODULES, UI);

    private Eclipse() { }
    public static ModuleManager modules() { return MODULES; }
    public static UiSettings ui() { return UI; }
    public static Notifications notifications() { return NOTIFICATIONS; }
    public static ConfigManager config() { return CONFIG; }
}
