# 🛒 LeykaShopBot

Telegram-бот для продажи Telegram Premium и Stars с автоматической обработкой платежей.

## ✨ Возможности

- 💫 Продажа **Telegram Stars** по актуальному курсу
- 👑 Продажа **Telegram Premium** подписок
- 💳 Несколько способов оплаты: карта, СБП QR, криптовалюта
- 💎 Интеграция с **TON** кошельком для крипто-платежей
- 🛒 Интеграция с **Platega** (платёжный агрегатор)
- 📦 Интеграция с **Fragment** для покупки Stars
- 📊 Логирование всех транзакций в Telegram канал
- 🔒 Dev-режим с вайтлистом для тестирования
- 📢 Проверка подписки на канал перед покупкой

## 🚀 Быстрый старт

### Требования

- Java 17+
- Maven
- Аккаунт на [Platega](https://platega.ru)
- TON кошелёк
- Telegram Bot Token ([@BotFather](https://t.me/BotFather))

### Установка

1. Клонируй репозиторий:
```bash
git clone https://github.com/zyr1x/TelegramBot-Premium-Stars-Sale.git
cd TelegramBot-Premium-Stars-Sale
```

2. Скопируй конфиг и заполни:
```bash
cp src/main/resources/config.yml.example src/main/resources/config.yml
```

3. Собери проект:
```bash
mvn clean package
```

4. Запусти:
```bash
java -jar target/LeykaShopBot.jar
```

## ⚙️ Конфигурация

```yaml
telegram:
  channel-check-subscribe: '@your_channel'
  channel-check-subscribe-url: 'https://t.me/your_channel'
  log-channel-id: YOUR_LOG_CHANNEL_ID
  bot:
    token: 'YOUR_BOT_TOKEN'
    name: 'your_bot'

ton:
  wallet:
    mnemonic: "your mnemonic phrase here"
    api-key: "YOUR_TON_API_KEY"
    address: 'YOUR_TON_ADDRESS'

platega:
  api: 'YOUR_PLATEGA_API_KEY'
  merchant-id: 'YOUR_MERCHANT_ID'
```

## 💰 Поддерживаемые валюты и методы оплаты

| Метод | Наценка |
|-------|---------|
| СБП QR | 4% |
| Карта | 5% |
| Крипто | 1% |

## 📁 Структура проекта

```
src/
├── main/
│   ├── java/
│   │   └── ...        # Исходный код
│   └── resources/
│       ├── config.yml  # Основной конфиг
│       └── application.yml
```

## 📄 Лицензия

MIT
