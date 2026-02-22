# Claudette for Android

The SSH terminal built for Claude Code. Control AI coding sessions from your Android phone or tablet.

**Website:** [claudettemobile.com](https://claudettemobile.com)

---

## What is Claudette?

Claudette is a purpose-built SSH terminal client that connects to your Mac and lets you drive [Claude Code](https://docs.anthropic.com/en/docs/claude-code) sessions from anywhere. Full terminal emulation, an extended keyboard row, one-tap commands, prompt snippets, and session persistence -- all designed for the developer who ships with AI.

This is the **Android** app. The iOS app and companion CLI live in [claudette](https://github.com/Olorin-ai-git/claudette).

## Features

- **Full Terminal** -- SSH terminal with an extended keyboard row: Esc, Tab, Ctrl+C, pipe, brackets, and programming symbols
- **Multi-Tab Sessions** -- Multiple terminal tabs, each wrapped in its own tmux session
- **Commands, Skills & Agents** -- Browse and trigger your entire Claude Code toolkit with one tap
- **Prompt Snippets** -- Quick-access drawer organized by workflow: refactoring, debugging, Git, and more
- **CLAUDE.md Viewer** -- Review project instructions, token estimates, and character counts at a glance
- **Voice Input** -- Dictate prompts to Claude Code hands-free via the microphone overlay
- **File Browser & Editor** -- Browse and edit remote files over SFTP without leaving the app
- **Session Persistence** -- tmux-wrapped sessions survive app backgrounding and reconnect automatically
- **Host Key Verification** -- TOFU (Trust On First Use) fingerprint pinning for secure connections
- **Wake-on-LAN** -- Wake your Mac remotely before connecting
- **mDNS Discovery** -- Automatically discover Macs on your local network

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM + Hilt dependency injection
- **SSH:** Apache MINA SSHD
- **Crypto:** Bouncy Castle (Ed25519)
- **Serialization:** Kotlinx Serialization
- **Logging:** Timber
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35

---

## Getting Started

### Prerequisites

Before connecting Claudette to your Mac, ensure you have:

| Requirement                 | Why                                           | How to check                       |
| --------------------------- | --------------------------------------------- | ---------------------------------- |
| **macOS** with Remote Login | Claudette connects via SSH                    | `sudo systemsetup -getremotelogin` |
| **tmux**                    | Keeps sessions alive when the app backgrounds | `which tmux`                       |
| **Network access**          | Your phone must reach your Mac                | Same WiFi, VPN, or Tailscale       |
| **Claude Code** (optional)  | The AI coding tool Claudette is built around  | `which claude`                     |

### Setup Options

There are two ways to set up your Mac. Choose the one that fits:

|                 | Manual Checklist            | CLI (`npx claudette-setup`) |
| --------------- | --------------------------- | --------------------------- |
| **Best for**    | Understanding each step     | Getting started fast        |
| **Time**        | ~5 minutes                  | ~1 minute                   |
| **Interactive** | No                          | Yes (prompts to fix issues) |
| **QR pairing**  | No (enter details manually) | Yes (scan to pair)          |

---

### Option A: Manual Checklist

#### Step 1: Enable Remote Login (SSH)

```bash
sudo systemsetup -setremotelogin on
```

Or: **System Settings > General > Sharing > Remote Login > On**

#### Step 2: Install tmux

```bash
brew install tmux
```

#### Step 3: Install Claude Code (optional)

```bash
npm install -g @anthropic-ai/claude-code
```

#### Step 4: Find your Mac's IP address

**With Tailscale (recommended):**

```bash
tailscale ip -4
# e.g. 100.64.0.1
```

**Without Tailscale (WiFi only):**

```bash
ipconfig getifaddr en0
# e.g. 192.168.1.42
```

#### Step 5: Note your host key fingerprint

```bash
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

#### Step 6: Configure the Claudette app

1. Open Claudette on your Android device
2. Tap **+** to add a new server profile
3. Enter your Mac's IP address, username, and SSH port (default: 22)
4. Choose an authentication method:
   - **Generate Key** -- creates an Ed25519 key pair on-device (recommended)
   - **Import Key** -- import an existing PEM private key
   - **Password** -- use your macOS password
5. If you generated a key, copy the public key and append it to `~/.ssh/authorized_keys` on your Mac:
   ```bash
   echo "ssh-ed25519 AAAA..." >> ~/.ssh/authorized_keys
   chmod 600 ~/.ssh/authorized_keys
   ```
6. Tap the server to connect
7. On first connection, verify the host key fingerprint matches Step 5
8. Tap **Trust** -- you're in

---

### Option B: Quick Setup with CLI

The `claudette-setup` CLI (from the [iOS repo](https://github.com/Olorin-ai-git/claudette/tree/main/cli)) automates the entire checklist:

```bash
npx claudette-setup
```

The CLI is purely a convenience tool. Everything it does can be done manually using the checklist above.

---

## Network Configuration

| Method        | Works from               | Stability                           | Setup                           |
| ------------- | ------------------------ | ----------------------------------- | ------------------------------- |
| **Tailscale** | Anywhere                 | Stable IP, survives network changes | Install on Mac + phone, sign in |
| **Same WiFi** | Home only                | IP may change on DHCP renewal       | None                            |
| **VPN**       | Anywhere the VPN reaches | Depends on VPN provider             | Configure VPN on both devices   |

**Tailscale is strongly recommended.** It's free for personal use, gives each device a stable IP address, and works from any network without port forwarding.

---

## Security

- **Ed25519 keys** generated on-device, stored in Android Keystore
- **TOFU host key verification** pins fingerprints on first connect and warns on changes
- **No cloud servers** -- SSH connections go directly between your device and your Mac
- **No telemetry or analytics** -- zero network requests except to servers you configure
- **Open source** -- audit every line of code

## Privacy

Claudette collects no data. All credentials stay in Android Keystore. The app only connects to servers you explicitly configure.

---

## Building from Source

```bash
# Clone
git clone https://github.com/Olorin-ai-git/claudette-android.git
cd claudette-android

# Open in Android Studio
# Build > Make Project
# Run > Run 'app'
```

Requires Android Studio Hedgehog (2023.1) or later with Kotlin 2.0+.

---

## Related Repositories

- [claudette](https://github.com/Olorin-ai-git/claudette) -- iOS app + companion setup CLI
- [claudettemobile.com](https://claudettemobile.com) -- Website and documentation

## Contributing

Contributions are welcome. Please file issues and pull requests on this repository.

## License

MIT
