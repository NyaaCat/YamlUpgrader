package me.recursiveg.yamlupgrader;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main extends JavaPlugin {
    static {
        ConfigurationSerialization.registerClass(NbtItemStack.class);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (command.getName().equals("yu")) {
                if (args.length != 2) return false;
                String filename = args[1];
                File fileIn = new File(getDataFolder(), filename);
                if (!fileIn.isFile()) {
                    sender.sendMessage("bad file");
                    return true;
                }
                YamlConfiguration cfgIn = YamlConfiguration.loadConfiguration(fileIn);
                if ("tonbt".equalsIgnoreCase(args[0])) {
                    for (String key : cfgIn.getKeys(true)) {
                        if (cfgIn.isItemStack(key)) {
                            ItemStack it = cfgIn.getItemStack(key);
                            cfgIn.set(key, new NbtItemStack(it));
                        }
                    }

                    cfgIn.save(new File(getDataFolder(), filename + "-nbt112R1.yml"));
                } else if ("toyaml".equalsIgnoreCase(args[0])) {
                    for (String key : cfgIn.getKeys(true)) {
                        NbtItemStack nbt = cfgIn.getSerializable(key, NbtItemStack.class);
                        if (nbt != null) {
                            cfgIn.set(key, nbt.it);
                        }
                    }
                    cfgIn.save(new File(getDataFolder(), filename + "-yaml112R1.yml"));
                } else {
                    return false;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            sender.sendMessage(errors.toString());
        } finally {
            sender.sendMessage("Complete.");
        }
        return true;
    }
}
