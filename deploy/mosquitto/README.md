# Mosquitto MQTT Broker Configuration

## Development Mode (Current)

Поточна конфігурація дозволяє підключення без сертифікатів для розробки:
- `allow_anonymous true` - дозволяє анонімні підключення
- TLS сертифікати закоментовані
- Порт 8883 доступний для підключень

## Production Mode

Для production потрібно:

1. **Підготувати сертифікати** (CA, server cert/key) у папці `certs`

2. **Оновити конфігурацію** в `config/mosquitto.conf`:
   - Розкоментувати рядки з сертифікатами
   - Встановити `allow_anonymous false`
   - Встановити `require_certificate true`

## Примітки

- Сертифікати валідні 10 років
- Для production використовуйте сертифікати від довіреного CA
- Приватні ключі мають права доступу 600
