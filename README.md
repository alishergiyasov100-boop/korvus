# Корвус

Компаньон по устройству. Чат + agentic tool-use, на Android.

**v0.1.0 — MVP**

## Что умеет

- Чат с роутингом на 7 моделей:
  - **HF Inference Providers** (твой токен): Qwen3-Coder 480B, DeepSeek V3.2, Llama 4 Maverick, Kimi K2
  - **Pollinations** (анонимно, без лимитов): Claude Opus 4.8, GPT-5.2, o5-reasoning
- Tools: `read_file`, `write_file`, `list_dir`, `run_shell` — через Termux RUN_COMMAND broadcast + локальный HTTP bridge на 127.0.0.1
- 2×2 home widget — последние 3 ответа Корвуса + оранжевая полоса
- Авто-failover между моделями (если выбранная вернула ошибку)
- Onboarding: спрашивает имя, потом обращается по имени

## Стек

Kotlin, Compose, DataStore, OkHttp, kotlinx.serialization, NanoHTTPD.
Без NDK, без Hilt, без Room — минимальный MVP.

## Сборка

CI собирает debug + release APK при пуше в `main` — скачивай из Actions → artifacts.

Локально:
```
./gradlew assembleDebug
```

## Зависимости устройства

- Termux установлен и `termux-setup-storage` выполнен
- Termux:RUN_COMMAND permission разрешён в Korvus (Settings → Apps → Korvus → permissions)
- `curl` в Termux: `pkg install curl`
