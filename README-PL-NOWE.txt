POLISHSURVIVALBOTS 0.2.0 – ŹRÓDŁA PLUGINA

To jest projekt źródłowy do zbudowania JAR przez GitHub Actions. Nie jest to gotowy,
lokalnie przetestowany plik JAR.

Wymagania:
- Paper 26.3, Java 25
- Citizens kompatybilny z Paper 26.3
- uprawnienie aiplayers.admin
- właściciel ustawiony na MilosheQ (musi być OP)

Zmiany:
- nazwa pluginu PolishSurvivalBots
- właściciel MilosheQ
- automatyczne krótkie spacery NPC co 30 sekund, w promieniu 12 bloków
- komenda /aiplayers wander <nick>
- zachowane dotychczasowe create/remove/list/follow/stop/reload
- konfiguracja rozmów OpenAI-compatible zachowana z poprzedniego projektu

Ograniczenia:
- To nadal NPC Citizens, a nie prawdziwe konta-klienci Minecraft.
- Spacerowanie jest prostą nawigacją, nie autonomicznym survivalem.
- Brak prawdziwego kopania, craftingu i walki.
- NPC utworzone przez plugin nie są odtwarzane automatycznie po restarcie.
- Rozmowy AI wymagają poprawnego endpointu i modelu w config.yml.
