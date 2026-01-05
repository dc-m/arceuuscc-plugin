package com.arceuuscc.plugin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ArceuusCCPluginTest {
    public static void main(String[] args) throws Exception {
        System.out.println("[Arceuus CC] Loading plugin...");
        try {
            ExternalPluginManager.loadBuiltin(ArceuusCCPlugin.class);
            System.out.println("[Arceuus CC] Plugin registered with ExternalPluginManager");
        } catch (Exception e) {
            System.err.println("[Arceuus CC] Failed to load plugin: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("[Arceuus CC] Starting RuneLite...");
        RuneLite.main(args);
    }
}
