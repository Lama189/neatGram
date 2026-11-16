# neatGram Architecture

This directory contains a compact, incrementally maintained map of the project.

It is NOT intended to reproduce the entire Telegram architecture.

## Principles

1. Document only relevant subsystems.
2. Verify implementation details before marking them VERIFIED.
3. Prefer symbols and file paths over copied source.
4. Keep documents short and focused.
5. Update documentation when meaningful architectural knowledge is acquired.
6. Never scan the entire repository solely to fill documentation.

## Confidence levels

- KNOWN: previously verified architectural knowledge.
- DISCOVERED: located through search but not fully verified.
- VERIFIED: confirmed by source inspection.

## Suggested subsystem documents

- modules.md
- networking.md
- storage.md
- messaging.md
- media.md
- ui.md
- telegram-protocol.md

Create documents when actual work requires them.
Do not create empty descriptions of unknown subsystems.
