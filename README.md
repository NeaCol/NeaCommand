# NeaCommand

Modern command whitelist and tab-completion filtering system powered by LuckPerms.

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0)
[![Java: 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square)](https://www.oracle.com/java/)
[![bStats Servers](https://img.shields.io/bstats/servers/34681?style=flat-square)](https://bstats.org/plugin/bukkit/NeaCommand/34681)
[![bStats Players](https://img.shields.io/bstats/players/34681?style=flat-square)](https://bstats.org/plugin/bukkit/NeaCommand/34681)
[![Build Status](https://img.shields.io/github/actions/workflow/status/NeaCol/NeaCommand/build.yml?branch=main&style=flat-square)](https://github.com/NeaCol/NeaCommand/actions)

---

## Features

- **LuckPerms Inheritance:** Groups automatically inherit command permissions according to your LuckPerms hierarchy without duplicate configurations.
- **Tab-Completion Filtering:** Removes namespace prefixes (`minecraft:`, `bukkit:`, `purpur:`, plugin prefixes) from player autocompletion while retaining whitelisted commands.
- **Interactive Staff Notifications:** Alerts staff when unauthorized commands are attempted, providing interactive click actions:
  - `[ALLOW]`: Adds the command directly to the player's group.
  - `[ADD PERMISSION]`: Prepares the corresponding LuckPerms permission assignment.
- **Granular Subcommand Support:** Allows whitelisting base commands (`/warp`) or specific subcommands (`/warp spawn`).
- **Hidden Command Flag:** Allows command execution while suppressing tab completion suggestions via the `:hidden` suffix (e.g., `/fly:hidden`).
- **Internationalization (i18n):** Shipped with English (`en`) and Spanish (`es`) locales using Adventure MiniMessage.
- **Folia and Paper Support:** Compatible with modern Paper 1.20-1.21+ and multi-threaded regional servers (Folia).

---

## Commands and Permissions

### Commands (`/neacommand`, `/nc`, `/nea`)

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/nc add <group> <cmd> [hidden]` | Adds a command to a group's whitelist. | `neacommand.admin` |
| `/nc remove <group> <cmd>` | Removes a command from a group's whitelist. | `neacommand.admin` |
| `/nc list <group>` | Lists whitelisted commands for a group. | `neacommand.admin` |
| `/nc check <player>` | Inspects player groups and command statistics. | `neacommand.admin` |
| `/nc sync` | Synchronizes groups with loaded LuckPerms groups. | `neacommand.admin` |
| `/nc reload` | Reloads configuration, groups, and translations. | `neacommand.admin` |
| `/nc help` | Displays the command help menu. | `neacommand.admin` |

### Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `neacommand.admin` | Administrative access to commands and configuration reloading. | `op` |
| `neacommand.admin.notify` | Receives interactive notifications when commands are blocked. | `op` |
| `neacommand.bypass` | Bypasses all command whitelist restrictions. | `op` |

---

## Configuration

### `config.yml`

```yaml
# Selected language file located in 'lang/' ('en' or 'es')
language: "en"

# Enable debug output in the server console
debug-mode: false

# Server operators (OP) automatically bypass restrictions
bypass-op: true

# Anonymous metrics collection via bStats
metrics: true

# Commands accessible to all players unconditionally
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
      - "/fly:hidden" # Allowed, but excluded from tab completion

  admin:
    commands:
      - "*" # Access to all commands
```

---

## Installation

1. Ensure your server runs Paper, Purpur, or Folia (1.20 - 1.21+).
2. Install [LuckPerms](https://luckperms.net/).
3. Download the latest `NeaCommand-x.x.x.jar` from the [Releases](https://github.com/NeaCol/NeaCommand/releases) section.
4. Place the JAR file into your server's `plugins/` directory and restart the server.
5. Configure permissions in `plugins/NeaCommand/groups.yml` or using `/nc add <group> <command>`.

---

## Building from Source

Requirements: Java 21 JDK and Maven.

```bash
git clone https://github.com/NeaCol/NeaCommand.git
cd NeaCommand
mvn clean package
```

The compiled artifact will be located in `target/`.

---

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0). See [LICENSE](LICENSE) for details.
