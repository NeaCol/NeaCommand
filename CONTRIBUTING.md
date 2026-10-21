# Contributing to NeaCommand

Guidelines and standards for contributing to **NeaCommand**.

NeaCommand is a command whitelist and tab-completion filtering plugin integrated with LuckPerms for Paper, Purpur, and Folia servers.

---

## Development Setup

1. **Prerequisites:**
   - Java 21 JDK
   - Maven 3.8+
   - Git

2. **Compiling:**
   ```bash
   mvn clean package
   ```
   The compiled shaded plugin JAR is output to `target/`.

---

## Code Guidelines

- **Folia Compatibility**: All tasks must be scheduled through `SchedulerUtils` to guarantee thread safety across Folia regional ticks.
- **Packet Handling**: Keep packet inspection lightweight and asynchronous where possible to avoid latency on network threads.
- **Conventional Commits**: Format commit messages using the standard format:
  - `feat: ...`
  - `fix: ...`
  - `docs: ...`
  - `refactor: ...`
