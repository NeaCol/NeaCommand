<div align="center">

# ⚡ NeaCommand

**Modern Command Whitelist & Tab-Completion Blocker powered by LuckPerms.**

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0)
[![Java: 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square)](https://www.oracle.com/java/)
[![Platform: Paper / Purpur / Folia](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Folia-red.svg?style=flat-square)](https://papermc.io/)
[![bStats](https://img.shields.io/badge/bStats-Metrics-brightgreen.svg?style=flat-square)](https://bstats.org/)
[![Build Status](https://img.shields.io/github/actions/workflow/status/NeaCol/NeaCommand/build.yml?branch=main&style=flat-square)](https://github.com/NeaCol/NeaCommand/actions)

<p align="center">
  <a href="#features">Features</a> •
  <a href="#commands--permissions">Commands</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#installation">Installation</a> •
  <a href="#building">Building</a> •
  <a href="#license">License</a>
</p>

</div>

---

## 🌟 Features

* **⚡ Native LuckPerms Inheritance:** Groups automatically inherit command whitelists according to your LuckPerms hierarchy. No duplicate configurations needed.
* **🛡️ Clean Tab-Completion (No Namespace Pollution):** Eliminates messy suggestions such as `/minecraft:`, `/bukkit:`, or `/purpur:` from player autocompletion while preserving whitelisted commands.
* **🔔 Interactive Staff Notifications:** When a player runs an unauthorized command, Staff members receive an alert in chat with interactive buttons:
  * **`[ALLOW]`**: Automatically adds the command to the player's group with a single click.
  * **`[ADD PERMISSION]`**: Automatically prepares the `/lp group <group> permission set` command if the underlying plugin requires permissions.
* **🎯 Granular Subcommand Support:** Whitelist entire commands (`/warp`) or isolate specific subcommands (`/warp spawn`), giving you complete control over your player experience.
* **🕵️ `:hidden` Commands:** Allow players to run a command without cluttering their Tab autocompletion suggestions (e.g. `/fly:hidden`).
* **🌐 Native Multi-Language (i18n):** Shipped with **English** (`en`) and **Spanish** (`es`) translations out-of-the-box, powered by [Kyori Adventure MiniMessage](https://docs.advntr.dev/minimessage/format.html) (gradients, RGB, hover/click events).
* **🚀 Folia & Paper Native:** Fully compliant with Paper 1.20-1.21+ and multi-threaded regional servers (Folia).

---

## 📋 Commands & Permissions

### Commands (`/neacommand`, `/nc`, `/nea`)

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/nc add <group> <cmd> [hidden]` | Adds a command to a group's whitelist. | `neacommand.admin` |
| `/nc remove <group> <cmd>` | Removes a command from a group's whitelist. | `neacommand.admin` |
| `/nc list <group>` | Lists all commands whitelisted for a group. | `neacommand.admin` |
| `/nc check <player>` | Inspects a player's primary group, inherited groups, and whitelist stats. | `neacommand.admin` |
| `/nc sync` | Synchronizes configured groups with loaded LuckPerms groups. | `neacommand.admin` |
| `/nc reload` | Reloads configurations, groups, and language files. | `neacommand.admin` |
| `/nc help` | Displays the help menu. | `neacommand.admin` |

### Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `neacommand.admin` | Access to all administrative commands and reloading. | `op` |
| `neacommand.admin.notify` | Receive interactive alerts when players run blocked commands. | `op` |
| `neacommand.bypass` | Bypasses all command restrictions and whitelist checks. | `op` |

---

## ⚙️ Configuration

### `config.yml`
```yaml
# Selected language file in 'lang/' ('en' or 'es')
language: "en"

# Enable debug mode in console
debug-mode: false

# Server operators (OP) automatically bypass all checks
bypass-op: true

# Anonymous telemetry via bStats
metrics: true

# Commands accessible to ALL players unconditionally
global-whitelist:
  - "login"
  - "register"
  - "l"
  - "reg"
  - "neacommand"
  - "nc"
  - "nea"
```

### `groups.yml`
```yaml
groups:
  default:
    commands:
      - "/help"
      - "/spawn"
      - "/msg"
      - "/tpa"
      - "/tpaccept"
      - "/home"
      - "/sethome"
      - "/menu"

  vip:
    commands:
      - "/hat"
      - "/craft"
      - "/feed"
      - "/fly:hidden" # Allowed, but hidden from Tab autocompletion

  admin:
    commands:
      - "*" # Access to all commands
```

---

## 📥 Installation

1. Make sure your server is running **Paper**, **Purpur**, or **Folia** (1.20 - 1.21+).
2. Download and install **[LuckPerms](https://luckperms.net/)**.
3. Download the latest `NeaCommand-x.x.x.jar` from the [Releases](https://github.com/NeaCol/NeaCommand/releases) page.
4. Place the `.jar` inside your server's `plugins/` folder and restart your server.
5. Customize your whitelists in `plugins/NeaCommand/groups.yml` or use `/nc add <group> <command>`.

---

## 🛠️ Building from Source

To compile NeaCommand yourself, ensure you have **Java 21 JDK** and **Maven** installed:

```bash
git clone https://github.com/NeaCol/NeaCommand.git
cd NeaCommand
mvn clean package
```

The compiled JAR will be located inside the `target/` directory.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** (GPL-3.0). See the [LICENSE](LICENSE) file for more information.
