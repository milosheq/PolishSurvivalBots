AIPlayersPaper 0.1.0 — projekt startowy
Paper 26.3 / Java 25

CO JUŻ ZAWIERA:
- Limit 5 NPC.
- Tworzenie NPC Citizens: /aiplayers create <nick>
- Usuwanie/listowanie: /aiplayers remove <nick>, /aiplayers list
- Podążanie i zatrzymywanie: /aiplayers follow <nick> <gracz>, /aiplayers stop <nick>
- Rozmowa z AI po polsku po wpisaniu na czacie: NickBota, wiadomość
- OpenAI-compatible endpoint; domyślnie Ollama lokalnie.

WAŻNE OGRANICZENIA TEJ WERSJI:
To jest wersja startowa/prototyp rozmów i NPC, a nie pełny port moda AI Players.
Boty są NPC Citizens, nie prawdziwymi kontami graczy. Ta wersja NIE implementuje jeszcze
samodzielnego kopania, craftingu, budowania, farmienia ani walki. Nie należy jej
przedstawiać jako gotowego survivalowego AI. Te moduły wymagają osobnej implementacji
i testów w Paper.

WYMAGANIA:
- Paper 26.3, Java 25
- Citizens 2.0.43+
- Ollama lokalnie albo inny endpoint zgodny z OpenAI Chat Completions.

KONFIGURACJA:
1. Zbuduj projekt poleceniem: gradle build
2. Skopiuj build/libs/AIPlayersPaper-0.1.0.jar do plugins/
3. Zainstaluj Citizens 2.0.43+ w plugins/
4. Uruchom serwer, edytuj plugins/AIPlayersPaper/config.yml
5. Dla lokalnego AI zainstaluj Ollama, uruchom: ollama pull mistral
   Domyślny endpoint: http://127.0.0.1:11434/v1/chat/completions
6. Zrestartuj serwer.

KOMENDY:
- /aiplayers create Kuba
- /aiplayers create Mati
- /aiplayers list
- /aiplayers follow Kuba TwójNick
- /aiplayers stop Kuba
- /aiplayers remove Kuba
- /aiplayers reload

ROZMOWA:
Kuba, cześć! Co dziś robisz?

BEZPIECZEŃSTWO:
Nie wpisuj klucza API do publicznych plików ani nie udostępniaj config.yml.
