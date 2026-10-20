# ⚡ NeaCommand

<div align="center">

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Folia-1e87f0)](https://papermc.io/)
[![bStats](https://img.shields.io/badge/bStats-Plugin%2034681-brightgreen)](https://bstats.org/plugin/bukkit/NeaCommand/34681)
[![Modrinth](https://img.shields.io/badge/Modrinth-neacommand-00AF5C?logo=modrinth&logoColor=white)](https://modrinth.com/plugin/neacommand)
[![GitHub](https://img.shields.io/badge/GitHub-NeaCol%2FNeaCommand-black?logo=github)](https://github.com/NeaCol/NeaCommand)

**Modern, ultra-fast command whitelist and clean tab-completion system powered by LuckPerms.**

</div>

---

### 📖 Overview

**NeaCommand** is an advanced, high-performance command protection and tab-completion sanitization plugin engineered for modern Minecraft servers. Built from the ground up to integrate seamlessly with **LuckPerms**, it eliminates the headache of maintaining redundant permissions and massive command lists.

Traditional command blockers either fail to hide sensitive commands from tab-completion, break plugin subcommands, or lag your main server thread. **NeaCommand** intercepts commands at the packet level, dynamically resolving LuckPerms inheritance while keeping autocompletion spotless.

---

### ✨ Key Features

* 🛡️ **Total Command Protection**: Restrict unauthorized commands across all player ranks with zero bypass loopholes.
* 🧬 **Native LuckPerms Inheritance**: Define allowed commands per LuckPerms group. If a rank inherits another rank, it automatically inherits all allowed commands—no duplicate lists needed!
* 🧹 **Clean Tab-Completion Filtering**: Completely eliminates annoying autocompletions such as `minecraft:`, plugin namespace prefixes (`essentials:`, `worldguard:`), and syntax snooping (`/version`, `/plugins`, `/about`).
* 🔔 **Interactive Staff Alerts**: When a player attempts an unauthorized command, staff members receive real-time MiniMessage notifications with clickable `[ALLOW]` buttons to instantly add the command to the player's group.
* 💡 **Intelligent Permission Resolution**: NeaCommand identifies the underlying plugin permission required for any command and provides clickable suggestions (`[ADD PERMISSION]`) directly to LuckPerms!
* 🚀 **Full Folia & Paper Optimization**: Built with thread-safe scheduling (`SchedulerUtils`) for 100% native compatibility with Paper, Purpur, and Folia multi-threaded tick regions.
* 🌐 **Multi-Language Support**: Complete internationalization out of the box with English (`en`) and Spanish (`es`) included. Fully customizable with MiniMessage formatting and gradients.
* 📊 **bStats Telemetry**: Anonymous usage metrics (Plugin ID: `34681`).

---

### 📋 Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/nc add <group> <command> [hidden]` | `neacommand.admin` | Adds an allowed command to a group. Set `hidden` to `true` to allow usage while hiding it from tab-completion. |
| `/nc remove <group> <command>` | `neacommand.admin` | Removes an allowed command from a group. |
| `/nc list <group>` | `neacommand.admin` | Displays all currently whitelisted commands for the specified group. |
| `/nc check <player>` | `neacommand.admin` | Inspects a player's primary group, inherited groups, allowed command count, and bypass state. |
| `/nc sync` | `neacommand.admin` | Synchronizes configured groups with active LuckPerms groups. |
| `/nc reload` | `neacommand.admin` | Reloads `config.yml`, `groups.yml`, and language files without restarting. |
| `/nc help` | `neacommand.admin` | Displays the command guide and subcommands list. |

*Aliases:* `/neacommand`, `/nc`, `/nea`

---

### 🔑 Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `neacommand.admin` | `op` | Grants access to administrative commands (`/nc reload`, `/nc add`, `/nc check`, etc.). |
| `neacommand.admin.notify` | `op` | Receives clickable alerts when players try to execute blocked commands. |
| `neacommand.bypass` | `op` | Completely bypasses the command whitelist and tab-completion filtering. |

---

### ⚙️ Configuration Files

#### `config.yml`
```yaml
# ----------------------------------------------------------------------
# NeaCommand - Main Configuration
# Advanced Command Whitelist & Tab-Completion Blocker powered by LuckPerms
# ----------------------------------------------------------------------

# Selected language file located inside the 'lang/' directory.
# Available out of the box: 'en' (English), 'es' (Spanish).
language: "en"

# Enable debug mode to log command verification and group lookups in console.
debug-mode: false

# If true, server operators (OP) automatically bypass all whitelist rules.
bypass-op: true

# Enable anonymous metrics collection via bStats.
metrics: true

# Commands that are ALWAYS accessible to all players, regardless of their group.
# Essential for authentication plugins, basic lobby commands, or aliases.
# Do NOT include the leading '/' character.
global-whitelist:
  - "login"
  - "register"
  - "l"
  - "reg"
  - "neacommand"
  - "nc"
  - "nea"
```

#### `groups.yml`
```yaml
# ----------------------------------------------------------------------
# NeaCommand - Groups Configuration
# Command whitelist mapped directly to LuckPerms group names.
#
# Syntax Guide:
#   - "/command"         : Player can run this command and it appears in Tab-Complete.
#   - "/command:hidden"  : Player can run this command, but it remains HIDDEN from Tab-Complete.
#   - "/warp spawn"      : Allows execution of that specific subcommand.
#   - "*"                : Grants access to all commands (bypass).
#
# Note: LuckPerms inheritance is automatically applied!
# For example, if 'vip' inherits from 'default' in LuckPerms,
# VIP players automatically gain access to all commands in 'default'.
# ----------------------------------------------------------------------

groups:
  default:
    commands:
      - "/help"
      - "/rules"
      - "/spawn"
      - "/msg"
      - "/r"
      - "/tpa"
      - "/tpaccept"
      - "/tpdeny"
      - "/pay"
      - "/balance"
      - "/baltop"
  vip:
    commands:
      - "/fly"
      - "/feed"
      - "/hat"
      - "/workbench"
  staff:
    commands:
      - "/kick"
      - "/mute"
      - "/ban"
      - "/warn"
      - "/vanish:hidden"
```

---

### 📦 Requirements

* **Platform:** Paper, Purpur, or Folia (`1.20` - `1.21.4+`)
* **Java:** `Java 21` or newer
* **Dependencies:** [LuckPerms](https://luckperms.net/) (`v5.4+`)

---

### 🛠️ Compiling from Source

Requires Java 21 and Maven:
```bash
git clone https://github.com/NeaCol/NeaCommand.git
cd NeaCommand
mvn clean package
```
The resulting JAR will be located in the `target/` directory.

---

### 👨‍💻 Credits & Author

Developed with ❤️ by **NeaCol**.
* Modrinth: [neacommand](https://modrinth.com/plugin/neacommand)
* GitHub: [NeaCol/NeaCommand](https://github.com/NeaCol/NeaCommand)
* Issues & Support: [Bug Tracker](https://github.com/NeaCol/NeaCommand/issues)
