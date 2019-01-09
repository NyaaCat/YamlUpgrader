package me.recursiveg.yamlupgrader;

import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Method;

public class Accessor {
    static Method write;
    static Method load;

    static {
        try {
            write = NBTTagCompound.class.getDeclaredMethod("write", DataOutput.class);
            write.setAccessible(true);
            load = NBTTagCompound.class.getDeclaredMethod("load", DataInput.class, int.class, NBTReadLimiter.class);
            load.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void nbtTagCompoundWrite(NBTTagCompound nbt, DataOutput output) {
        try {
            write.invoke(nbt, output);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void nbtTagCompoundLoad(NBTTagCompound nbt, DataInput var1, int var2, NBTReadLimiter var3) {
        try {
            load.invoke(nbt, var1, var2, var3);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
