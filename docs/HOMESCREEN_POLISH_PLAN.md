# HomeScreen Modernization & Aesthetic Polish Plan

This document outlines the detailed UI polish refinements for `HomeScreen.kt` to elevate the visual aesthetic of the main dashboard screen.

---

## 🎨 Proposed HomeScreen Refinements

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ HOMESCREEN POLISH FEATURES                                                  │
│   1. Live Mesh Status Pill (Pulsing connection indicator badge)             │
│   2. Sleek Segmented Tab Selector (Pill tabs for Chats / Peers)             │
│   3. Modern Avatar Ring Badges (Peer initials + online indicator)           │
│   4. Polished Empty States (Illustrative icons + styled action buttons)     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 1. 🌐 Live Mesh Status Header Badge
- **Current:** Text line in `TopAppBar` title (`3 direct links · 8 peers known`).
- **Polished:** High-contrast status pill badge with a live status dot:
  - 🟢 **Green (`#2ECC71`)**: Mesh active & connected over BLE.
  - 🟠 **Amber (`#F39C12`)**: Mesh initializing / scanning.
  - 🔴 **Red (`#E74C3C`)**: Bluetooth is disabled.

---

### 2. 🎛️ Segmented Pill Tab Bar
- **Current:** Standard `TabRow` with flat bottom underline indicator.
- **Polished:** Rounded segmented container (`Surface(shape = RoundedCornerShape(16.dp))`) where active tabs highlight with a pill shape and clear typography.

---

### 3. 🖼️ Avatar System & Status Indicators
- **Current:** Square with rounded corners (`12.dp`).
- **Polished:**
  - Initial letters of peer's display name or Node ID.
  - Glowing status dot on the bottom-right corner of peer avatars indicating online/recent state (< 5 mins).

---

### 4. 📭 Polished Empty States
- **Current:** Plain text message when no peers are discovered.
- **Polished:** Elevated card container featuring a `BluetoothSearching` icon, clear guidance text, and a styled call-to-action button.

---

Shall we apply these **HomeScreen UI polish refinements**? Let me know and we will get started!
