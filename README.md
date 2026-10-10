# 🟦 SimpleClumps – Lag-free drops, smoother servers!

**SimpleClumps** is a lightweight, fully server-side mod for **Fabric** and **NeoForge** that improves performance and gameplay by merging drops **and** streamlining tree cutting.  

By reducing entity clutter and simplifying repetitive actions, it keeps your world running smoothly while staying true to vanilla feel.  

Perfect for survival servers, SMPs, and performance-focused hosts.

---

## ✨ Features

### 📦 Smart Drop Clumping
- 🟦 Nearby XP orbs automatically merge into as few orbs as possible, without losing any XP  
- 📦 Dropped items within a **configurable radius** (default: **5 blocks** in every direction) merge into full stacks  
- 🔢 Items display as `x30 Spruce Wood` (example) instead of 30 separate drops, and the label updates when part of a stack is picked up  
- 🧭 Follows vanilla's merging rules: items only one player may pick up stay theirs, fresh drops wait until they can be picked up, and merged stacks keep the newer despawn timer  
- ⚡ Significantly reduces entity count for better server performance  

### 🧹 Scheduled Cleanup
- 🗑️ Every few minutes (default: **5**), all dropped items and XP orbs in loaded chunks are removed  
- 📢 Players get a warning 30 seconds before, and a countdown for the last 5 seconds  
- 🔧 The interval can be changed, or the cleanup turned off, in the config  

### 🌲 Tree Cutting Helper
- 🪓 Break one log with an axe to fell the entire tree  
- 🧍 Sneak while breaking to cut a single log  
- 🌿 Automatically detects real trees: only natural leaves count, not leaves placed by players (prevents accidental structure breaking)  
- 🛡 Uses up axe durability, gives no drops in creative, and respects spawn protection and protection mods  
- ⚡ Fast and efficient — no need to manually chop every block  
- 🎮 Designed to feel natural and balanced with vanilla gameplay  
- 🔧 Can be enabled or disabled via config  

### ⚙️ Configurable
SimpleClumps now includes a config file so you can tweak behavior to fit your server.  
It is created on the first start at:  
`/config/SimpleClumps_Seed_<world seed>/SimpleClumps.json`

- 📏 **Clumping Radius** (`SimpleClumps_ClumpRadius`, default `5`) — how far (in blocks) items and XP merge  
- ⏱️ **Cleanup Interval** (`SimpleClumps_CleanupMinutes`, default `5`) — minutes between cleanups of all dropped items and XP orbs (`0` turns the cleanup off)  
- 🌲 **Tree Cutting Toggle** (`SimpleClumps_EnableTreeCutter`, default `true`) — enable or disable the tree helper feature  

Changes take effect after a server restart.

---

## 🧩 Requirements

**Fabric**
- [Fabric Loader](https://fabricmc.net/use/)  
- [Fabric API](https://modrinth.com/mod/fabric-api)  

**NeoForge**
- [NeoForge](https://neoforged.net/) (nothing else needed)  

---

## 🛠️ Building

The project builds both loaders from one shared codebase:

- `common/` – the mod itself (clumping, cleanup, tree cutting, config), plain Minecraft code  
- `fabric/` – the Fabric entrypoint and `fabric.mod.json`  
- `neoforge/` – the NeoForge entrypoint and `neoforge.mods.toml`  

Run `gradlew build` (Java 25). The jars are created in:

- `fabric/build/libs/simpleclumps-fabric-<version>.jar`  
- `neoforge/build/libs/simpleclumps-neoforge-<version>.jar`  

Versions (Minecraft, Fabric, NeoForge) are all set in `gradle.properties`.

---

## 🌟 Why SimpleClumps?

Because **your server deserves smooth performance!**  

SimpleClumps reduces lag from excessive entities **and** removes tedious actions like chopping trees block-by-block — all while giving you full control through configuration.  

Fewer entities. Less grind. More control. Happier players.

---

## 📜 License

This mod is licensed under the GNU AGPLv3 Licence.  

---

## 💬 Feedback

Found a bug or have a feature suggestion?  
Open an issue or PR on GitHub!
