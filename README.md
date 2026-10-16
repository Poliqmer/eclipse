# Eclipse Client (Fabric 1.21.11)

Autorski klient modów (Mojang mappings, Loom `fabric-loom` 1.14, Java 21). Kod napisany od zera – mod referencyjny
(AutoRynek, bez licencji) i JAR NiceWurst (GPL-3.0) posłużyły wyłącznie do analizy; nic z nich nie skopiowano.

## Status (szczerze)
- **Skompilowane i uruchomione:** tylko logika bez Minecrafta (ustawienia, walidacja, flaga zmian) – OK.
- **NIE skompilowane z Minecraftem:** środowisko, w którym powstał kod, blokuje maven.fabricmc.net i Maven Central.
  Kompilację wykona GitHub Actions (`.github/workflows/build.yml`). Przy pierwszym buildzie mogą wyskoczyć błędy
  sygnatur API – patrz "Miejsca podwyższonego ryzyka".
- **Nie testowane w grze.**

## Uruchomienie
`./gradlew build` → `build/libs/eclipse-client-0.1.0.jar`. GUI: **Right Shift** lub `/eclipse`. Panic: **End**.
Komendy: `/eclipse profile list|load|new <n>`, `/eclipse export|import <n>`, `/eclipse wp add|remove|list`, `/eclipse panic`.
Konfiguracja: `config/eclipse/profiles/<profil>.json` (+ `.bak`).

## Moduły
Render: Fullbright, Tracers, ESP (2D/3D), FullBox ESP, Item ESP, Zoom · World: Chunk Borders, Waypoints ·
HUD: FPS/ping, koordynaty, kierunek, biom, zegar, aktywne moduły, CPS, Keystrokes, Armor, Potion, Custom Crosshair ·
Player: Toggle Sprint, Auto Tool, Inventory HUD · Movement: prędkość, kierunek ruchu, trajektoria ·
Combat: Combat HUD, KillAura (test, exp), Surround/Cobweb (test, exp) · Misc: Panic, Sound Notifications, Debug overlay.

Moduły automatyzujące (KillAura, Surround) działają **tylko w lokalnym świecie bez LAN** (`util/Safety`), nie celują w graczy,
wymagają linii widoczności i nie są wznawiane z konfiguracji. Brak omijania antycheatów.

## Nie zaimplementowane (celowo, bez pustych klas)
Storage ESP, Nametags, trening celowania, Screenshot Utilities, Chat Customizer, edycja pól tekstowych w GUI,
selektor koloru HSV (jest paleta), eksport/import w GUI (są komendy).

## Miejsca podwyższonego ryzyka kompilacji (API 1.21.9–1.21.11, nie zweryfikowane)
`Window.handle()`, `HudElementRegistry`/`VanillaHudElements`, `Identifier.fromNamespaceAndPath`, `MouseButtonEvent`/`KeyEvent`/`CharacterEvent`
w `Screen`, `g.pose().pushMatrix()/scale()`, `Inventory.getSelectedSlot()/setSelectedSlot()`, `Level.isUnobstructed(...)`,
wersje `fabric_api_version`/`loader_version`/`loom_version` w `gradle.properties`.
Ograniczenia: rzutowanie ESP/Tracers używa pozycji oczu i FOV z opcji (bez macierzy renderera) – poprawne w widoku 1. osoby;
Fullbright w trybie Gamma ogranicza się do maksimum suwaka vanilla (tryb "Efekt" daje pełną jasność lokalnie).
