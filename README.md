# How to build

    git clone <URL_HERE>
    cd YamlUpgrader
    ./build.sh

You can also copy spigot binaries to the `spigot_bin` folder manually.
It's required to have `gradle` installed on your system.

# How it works

In order to upgrade a yaml config, say `save.yml`, you need to setup both 1.12.2 and 1.13.2 spigot server.
The corresponding plugin jar should be used. All `yml` files should be put in plugin's config folder (i.e. `plugins/YamlUpgrader`).

We first convert `ItemStack` into native NBT byte streams using 1.12.2 server:

    /yu tonbt save.yml
    
then the file `save.yml-nbt112R1.yml` will be generated.
Copy the file to 1.13.2 server's data folder and we now convert the nbt back to spigot-readable config.

    /yu toyaml save.yml-nbt112R1.yml

Now `save.yml-nbt112R1.yml-yaml113R2.yml` is generated and can be used.

When the old 1.12.2 nbt format is loaded using 1.13.2 server,
Minecraft's native data upgrade routine will be invoked
and upgrade it to 1.13.2 nbt format.
The new nbt will then be assembled into `ItemStack` and
saved back to the YAML file.

# Bug report

Open an issue and be sure to attach a minimal YAML config (usually a single item)
which can produce the error.

# Special thanks

- [NyaaCore](https://github.com/NyaaCat/NyaaCore)