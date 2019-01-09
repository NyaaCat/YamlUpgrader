package me.recursiveg.yamlupgrader;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.DeflaterInputStream;
import java.util.zip.InflaterInputStream;

public final class ItemStackUtils {
    private static final String NYAACORE_ITEMSTACK_DATAVERSION_KEY = "nyaacore_itemstack_dataversion";
    private static final int NYAACORE_ITEMSTACK_DEFAULT_DATAVERSION = 1139;
    private static NBTReadLimiter unlimitedNBTReadLimiter = null;
    private static Cache<String, List<ItemStack>> itemDeserializerCache = CacheBuilder.newBuilder()
            .weigher((String k, List<ItemStack> v) -> k.getBytes().length)
            .maximumWeight(100L * 1024 * 1024).build(); // Hard Coded 100M

    /**
     * Get the binary representation of ItemStack
     * for fast ItemStack serialization
     *
     * @param itemStack the item to be serialized
     * @return binary NBT representation of the item stack
     */
    public static byte[] itemToBinary(ItemStack itemStack) throws IOException {
        net.minecraft.server.v1_12_R1.ItemStack nativeItemStack = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nativeItemStack.save(nbtTagCompound);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        Accessor.nbtTagCompoundWrite(nbtTagCompound, dos);
        byte[] outputByteArray = baos.toByteArray();
        dos.close();
        baos.close();
        return outputByteArray;
    }

    /**
     * Get the ItemStack from its binary representation
     * for fast ItemStack deserialization
     *
     * @param nbt binary item nbt data
     * @return constructed item
     */
    public static ItemStack itemFromBinary(byte[] nbt) throws ReflectiveOperationException, IOException {
        return itemFromBinary(nbt, 0, nbt.length);
    }

    public static ItemStack itemFromBinary(byte[] nbt, int offset, int len) throws ReflectiveOperationException, IOException {
        if (unlimitedNBTReadLimiter == null) {
            unlimitedNBTReadLimiter = NBTReadLimiter.a;
        }

        //Constructor<?> constructNativeItemStackFromNBTTagCompound = classNativeItemStack.getConstructor(classNBTTagCompound);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(nbt, offset, len);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        NBTTagCompound reconstructedNBTTagCompound = new NBTTagCompound();
        Accessor.nbtTagCompoundLoad(reconstructedNBTTagCompound, dataInputStream, 0, unlimitedNBTReadLimiter);
        dataInputStream.close();
        byteArrayInputStream.close();
        int dataVersion = reconstructedNBTTagCompound.getInt(NYAACORE_ITEMSTACK_DATAVERSION_KEY);
        if (dataVersion > 0) {
            reconstructedNBTTagCompound.remove(NYAACORE_ITEMSTACK_DATAVERSION_KEY);
        }
        net.minecraft.server.v1_12_R1.ItemStack reconstructedNativeItemStack = new net.minecraft.server.v1_12_R1.ItemStack(reconstructedNBTTagCompound);
        return CraftItemStack.asBukkitCopy(reconstructedNativeItemStack);
    }

    private static byte[] compress(byte[] data) {
        byte[] ret;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ByteStreams.copy(new DeflaterInputStream(bis), bos);
            ret = bos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return ret;
    }

    private static byte[] decompress(byte[] data) {
        byte[] ret;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ByteStreams.copy(new InflaterInputStream(bis), bos);
            ret = bos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return ret;
    }

    /* *
     * Structure of binary NBT list:
     * - First byte (n): number of items (currently limit to 0<=n<=127 i.e. MSB=0)
     * - Next 4*n bytes (s1~sn): size of binary nbt for each item
     * - Next sum(s1~sn) bytes: actual data nbt
     */

    /**
     * Convert a list of items into compressed base64 string
     */
    public static String itemsToBase64(List<ItemStack> items) {
        if (items.isEmpty()) return "";
        if (items.size() > 127) {
            throw new IllegalArgumentException("Too many items");
        }

        List<byte[]> nbts = new ArrayList<>();
        for (ItemStack item : items) {
            try {
                nbts.add(itemToBinary(item));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        byte[] uncompressed_binary;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeByte(items.size());
            for (byte[] nbt : nbts) dos.writeInt(nbt.length);
            for (byte[] nbt : nbts) dos.write(nbt);
            uncompressed_binary = bos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return BaseEncoding.base64().encode(compress(uncompressed_binary));
    }

    /**
     * Convert base64 string back to a list of items
     */
    public static List<ItemStack> itemsFromBase64(String base64) {
        List<ItemStack> stack = itemDeserializerCache.getIfPresent(base64);
        if (stack != null) return stack.stream().map(ItemStack::clone).collect(Collectors.toList());
        if (base64.length() <= 0) return new ArrayList<>();

        byte[] uncompressedBinary = decompress(BaseEncoding.base64().decode(base64));
        List<ItemStack> ret = new ArrayList<>();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(uncompressedBinary);
             DataInputStream dis = new DataInputStream(bis)) {
            int n = dis.readByte();
            int[] nbtLength = new int[n];
            for (int i = 0; i < n; i++) nbtLength[i] = dis.readInt();
            for (int i = 0; i < n; i++) {
                byte[] tmp = new byte[nbtLength[i]];
                dis.readFully(tmp);
                ret.add(itemFromBinary(tmp));
            }
        } catch (IOException | ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        itemDeserializerCache.put(base64, ret.stream().map(ItemStack::clone).collect(Collectors.toList()));
        return ret;
    }

    public static String itemToBase64(ItemStack item) {
        if (item == null) throw new IllegalArgumentException();
        return itemsToBase64(Collections.singletonList(item));
    }

    public static ItemStack itemFromBase64(String base64) {
        if (base64 == null) throw new IllegalArgumentException();
        List<ItemStack> ret = itemsFromBase64(base64);
        if (ret != null && !ret.isEmpty()) return ret.get(0);
        return null;
    }

    public static Object asNMSCopy(ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }
}
