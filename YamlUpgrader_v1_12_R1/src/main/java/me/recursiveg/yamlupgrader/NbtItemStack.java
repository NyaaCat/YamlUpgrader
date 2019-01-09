package me.recursiveg.yamlupgrader;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class NbtItemStack implements ConfigurationSerializable {
    public ItemStack it;

    public NbtItemStack(ItemStack it) {
        this.it = it;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> ret = new HashMap<>();
        if (it == null) {
            ret.put("nbt", "null");
        } else {
            ret.put("nbt", ItemStackUtils.itemToBase64(it));
        }
        return ret;
    }

    public static NbtItemStack deserialize(Map<String, Object> map) {
        if (map.getOrDefault("nbt", 0).getClass().equals(String.class)) {
            return new NbtItemStack(ItemStackUtils.itemFromBase64((String)map.get("nbt")));
        }
        throw new RuntimeException();
    }
}
