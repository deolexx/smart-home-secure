# Smart Home Secure - Diploma-Level Prototype

## Зміст

1. [Загальний огляд](#загальний-огляд)
2. [Архітектура](#архітектура)
3. [Функціонал](#функціонал)
4. [Швидкий старт](#швидкий-старт)
5. [Keycloak Setup](#keycloak-setup)
6. [Docker Setup](#docker-setup)
7. [Device Simulator](#device-simulator)
8. [API Документація](#api-документація)
9. [Безпека](#безпека)
10. [Технологічний стек](#технологічний-стек)

---

## Загальний огляд

**Smart Home Secure** - це система управління розумним домом з централізованим Hub (шлюзом), який керує IoT пристроями через MQTT протокол. Система забезпечує безпеку через JWT автентифікацію, RBAC (Role-Based Access Control) та шифрування трафіку.

### Компоненти системи:

- **Hub** (Spring Boot додаток) - центральний шлюз з REST API
- **Keycloak** - Identity and Access Management (IAM) для автентифікації та авторизації
- **Device Simulator** - симулятор IoT пристроїв
- **Mosquitto MQTT Broker** - брокер для обміну повідомленнями
- **PostgreSQL** - база даних для зберігання інформації

---

## Архітектура

### Модулі

- **hub**: Spring Boot application (REST API, Security, MQTT, JPA)
- **device-simulator**: Standalone Java app publishing telemetry and receiving commands

### Структура проєкту

```
hub/
  - domain: Device, DeviceTelemetry
  - repository: JPA repositories
  - service: DeviceService, TelemetryService
  - security: SecurityConfig, JwtTokenService, filters and handlers
  - mqtt: MqttConfig, MqttGateway
  - controller: AuthController, DeviceController, TelemetryController
  - resources: application.yml (dev + prod sections)
device-simulator/
  - DeviceSimulatorApplication (telemetry + command listener)
deploy/mosquitto: TLS-enabled broker config
```

### Clean Layered Architecture

- **Controller**: REST endpoints with `@PreAuthorize`
- **Service**: business logic, transaction boundaries
- **Repository**: JPA repositories
- **Security**: JWT issuance/validation, RBAC, filters
- **MQTT**: TLS client, telemetry ingestion, commands publishing
- **Persistence**: PostgreSQL (production-ready)

---

## Функціонал

### Hub (Центральний шлюз)

#### 1. Аутентифікація та авторизація

**Аутентифікація через Keycloak**

Реєстрація та вхід обробляються через Keycloak:

- **Keycloak Admin Console**: http://localhost:8090
- **Token Endpoint**: `POST http://localhost:8090/realms/smarthome/protocol/openid-connect/token`
- **User Info Endpoint**: `GET http://localhost:8090/realms/smarthome/protocol/openid-connect/userinfo`

**Функціонал:**
- Реєстрація користувачів через Keycloak Admin API (через `/api/auth/register`) або Admin Console
- Отримання JWT токенів від Keycloak
- Автоматична валідація токенів Hub'ом
- Ролі з Keycloak автоматично мапляться в Spring Security authorities

Детальні інструкції: [KEYCLOAK-SETUP.md](KEYCLOAK-SETUP.md)

**Ролі користувачів:**
- **ADMIN** - повний доступ до всіх функцій
- **USER** - перегляд пристроїв та телеметрії
- **DEVICE** - для майбутнього використання

#### 2. Управління пристроями

- **GET /api/devices** - Список пристроїв (ADMIN, USER)
- **POST /api/devices** - Створення пристрою (тільки ADMIN)
- **GET /api/devices/{id}** - Отримання пристрою (ADMIN, USER) (`id` = UUID)
- **PUT /api/devices/{id}** - Оновлення пристрою (тільки ADMIN) (`id` = UUID)
- **DELETE /api/devices/{id}** - Видалення пристрою (тільки ADMIN) (`id` = UUID)
- **POST /api/devices/{id}/commands** - Надсилання команди пристрою (тільки ADMIN) (`id` = UUID)
- **POST /api/devices/{id}/claim** - Прив'язати вільний пристрій до користувача (ADMIN, USER) (`id` = UUID)
- **POST /api/devices/by-client/{clientId}/temperature-unit** - Зміна одиниць температури (тільки ADMIN) (`clientId` = UUID)

#### 3. Телеметрія

- **GET /api/telemetry/devices/{deviceId}/latest** - Останні дані телеметрії (ADMIN, USER) (`deviceId` = UUID)

#### 4. MQTT Gateway

**Автоматична реєстрація пристроїв:**
- Hub підписується на топик: `devices/+/telemetry`
- Коли пристрій публікує телеметрію, Hub:
  1. Витягує `clientId` з топику
  2. Шукає пристрій за `mqttClientId` в базі даних
  3. Якщо пристрій знайдено - оновлює статус на ONLINE
  4. Якщо пристрій не знайдено - автоматично створює новий пристрій (без власника)
  5. Зберігає телеметрію в базу даних

**Надсилання команд:**
- Hub публікує команди на топик: `devices/{mqttClientId}/cmd`
- Пристрої підписуються на цей топик та отримують команди

### Device Simulator

**Функціонал:**

1. **Підключення до MQTT брокера**
   - Підтримка TLS та non-TLS підключень
   - Автоматичне переподключення при втраті з'єднання

2. **Публікація телеметрії**
   - Кожні 5 секунд публікує дані на топик: `devices/{clientId}/telemetry`
   - Формат даних (JSON):
     ```json
     {
       "temperature": 22.5,
       "humidity": 45.2,
       "status": "OK",
       "ts": "2026-01-07T20:30:00Z"
     }
     ```

3. **Отримання команд**
   - Підписується на топик: `devices/{clientId}/cmd`
   - Виводить отримані команди в лог

### Потік даних

**Реєстрація нового пристрою:**
1. Device Simulator запускається з унікальним `clientId`
2. Підключається до MQTT брокера
3. Починає публікувати телеметрію на `devices/{clientId}/telemetry`
4. Hub отримує повідомлення та автоматично створює пристрій в БД

**Надсилання команди пристрою:**
1. Адміністратор викликає `POST /api/devices/{id}/commands`
2. Hub знаходить пристрій за ID
3. Публікує команду на MQTT топик: `devices/{mqttClientId}/cmd`
4. Device Simulator отримує команду та виводить в лог

---

## Швидкий старт

### Передумови

- **Java 21+**
- **Maven 3.6+**
- **Docker Desktop** (або Docker Engine + Docker Compose) - для Docker запуску
- **PostgreSQL 16** (якщо запускаєте без Docker)

### Запуск через Docker (рекомендовано)

```bash
# Запуск всього стеку
docker-compose up -d

# Перегляд логів
docker-compose logs -f

# Зупинка
docker-compose down
```

### Аудит логів

Hub зберігає аудит логів запитів у таблиці `audit_logs`. Логуються: користувач, ролі, метод, шлях, статус, тривалість, IP та User-Agent.

Це запустить:
- **Keycloak** на порту 8090 (IAM сервер)
- **PostgreSQL Database** на порту 5432
- **Mosquitto MQTT Broker** на портах 1883 (без TLS) та 8883 (з TLS)
- **Hub** (Spring Boot API) на порту 8080
- **Device Simulator** - автоматично підключається до MQTT

## Keycloak Setup

### Швидкий старт

Keycloak запускається разом зі стеком. Після запуску виконайте bootstrap-скрипт:

**Windows (PowerShell):**
```powershell
.\deploy\keycloak\bootstrap.ps1
```

**Linux/Mac (bash):**
```bash
./deploy/keycloak/bootstrap.sh
```

Скрипт створить realm `smarthome`, client `smart-home-hub`, ролі `ADMIN/USER/DEVICE`
та admin-користувача в realm.

Деталі див. [KEYCLOAK-SETUP.md](KEYCLOAK-SETUP.md)

### Отримання токену

```bash
curl -X POST http://localhost:8090/realms/smarthome/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin123!" \
  -d "grant_type=password" \
  -d "client_id=smart-home-hub" \
  -d "client_secret=YOUR_CLIENT_SECRET"
```

### Використання токену

```bash
curl -X GET http://localhost:8080/api/devices \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Docker Setup

### Швидкий старт

```bash
docker-compose up -d
```

### Перегляд логів

```bash
# Всі сервіси
docker-compose logs -f

# Конкретний сервіс
docker-compose logs -f hub
docker-compose logs -f device-simulator
docker-compose logs -f mosquitto
docker-compose logs -f postgres
```

### Зупинка

```bash
docker-compose down
```

### Перебудова після змін коду

```bash
docker-compose up -d --build
```

### Окремі сервіси

**Запуск тільки Hub:**
```bash
docker-compose up -d postgres mosquitto hub
```

**Запуск тільки Device Simulator:**
```bash
docker-compose up -d mosquitto device-simulator
```

### Доступ до сервісів

- **Hub API**: http://localhost:8080
- **Hub Health Check**: http://localhost:8080/actuator/health
- **Keycloak Admin Console**: http://localhost:8090 (admin/admin123!)
- **PostgreSQL**: localhost:5432 (user: smarthome, password: smarthome123, database: smarthome)
- **Mosquitto MQTT**: tcp://localhost:1883 (dev) або ssl://localhost:8883 (prod)

### Змінні середовища

**Hub:**
```yaml
hub:
  environment:
    - SPRING_PROFILES_ACTIVE=prod
    - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/smarthome
    - SPRING_DATASOURCE_USERNAME=smarthome
    - SPRING_DATASOURCE_PASSWORD=smarthome123
    - MQTT_BROKER_URL=ssl://mosquitto:8883
    - MQTT_TLS_ENABLED=true
```

**Device Simulator:**
```yaml
device-simulator:
  environment:
    - SIM_BROKER_URL=tcp://mosquitto:1883
    - SIM_CLIENT_ID=my-device-id
```

### Розробка

**Перебудова одного сервісу:**
```bash
# Тільки hub
docker-compose build hub
docker-compose up -d hub

# Тільки device-simulator
docker-compose build device-simulator
docker-compose up -d device-simulator
```

**Запуск без кешування:**
```bash
docker-compose build --no-cache
docker-compose up -d
```

### Структура Docker файлів

- `docker-compose.yml` - головний файл для запуску всього стеку
- `hub/Dockerfile` - збірка Hub модуля
- `device-simulator/Dockerfile` - збірка Device Simulator модуля
- `.dockerignore` - файли, які ігноруються при збірці

### Примітки

- Hub чекає на готовність PostgreSQL та Mosquitto перед запуском
- Device Simulator чекає на готовність Hub перед запуском
- Всі сервіси автоматично перезапускаються при падінні (`restart: unless-stopped`)
- Дані PostgreSQL зберігаються в Docker volume `postgres-data` і не втрачаються при перезапуску контейнерів
- Для видалення всіх даних PostgreSQL: `docker-compose down -v`

---

## Device Simulator

### Швидкий старт

**Запуск через Docker (рекомендовано):**
```bash
docker-compose up -d device-simulator
```

### Конфігурація

Device Simulator налаштовується через змінні середовища:

**Обов'язкові параметри:**
- **`SIM_BROKER_URL`** - URL MQTT брокера
  - Для розробки (без TLS): `tcp://localhost:1883` або `tcp://mosquitto:1883` (в Docker)
  - Для production (з TLS): `ssl://localhost:8883` або `ssl://mosquitto:8883` (в Docker)

**Опціональні параметри:**
- **`SIM_CLIENT_ID`** - Унікальний ID пристрою (за замовчуванням: `device-{UUID}`)
- **`SIM_KEYSTORE_PATH`** - Шлях до keystore для mutual TLS (PKCS12)
- **`SIM_KEYSTORE_PASSWORD`** - Пароль keystore
- **`SIM_KEY_PASSWORD`** - Пароль приватного ключа (за замовчуванням = keystore password)
- **`SIM_TRUSTSTORE_PATH`** - Шлях до truststore з CA сертифікатом (JKS)
- **`SIM_TRUSTSTORE_PASSWORD`** - Пароль truststore

### Приклади використання

**1. Запуск локально без TLS:**
```bash
export SIM_BROKER_URL=tcp://localhost:1883
export SIM_CLIENT_ID=my-device-001
java -jar target/device-simulator-1.0.0-jar-with-dependencies.jar
```

**2. Запуск з TLS сертифікатами:**
```bash
export SIM_BROKER_URL=ssl://localhost:8883
export SIM_CLIENT_ID=my-device-001
export SIM_KEYSTORE_PATH=/path/to/device-keystore.p12
export SIM_KEYSTORE_PASSWORD=changeit
export SIM_KEY_PASSWORD=changeit
export SIM_TRUSTSTORE_PATH=/path/to/ca-truststore.jks
export SIM_TRUSTSTORE_PASSWORD=changeit
java -jar target/device-simulator-1.0.0-jar-with-dependencies.jar
```

**3. Запуск кількох пристроїв одночасно:**
```bash
# Пристрій 1
SIM_BROKER_URL=tcp://localhost:1883 SIM_CLIENT_ID=device-001 java -jar app.jar

# Пристрій 2
SIM_BROKER_URL=tcp://localhost:1883 SIM_CLIENT_ID=device-002 java -jar app.jar
```

### Перевірка роботи

**Перевірка логів:**
```bash
docker-compose logs -f device-simulator
```

**Перевірка через Hub API:**
```bash
# Отримати токен від Keycloak
curl -X POST http://localhost:8090/realms/smarthome/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin123!" \
  -d "grant_type=password" \
  -d "client_id=smart-home-hub" \
  -d "client_secret=YOUR_CLIENT_SECRET"

# Список пристроїв (використайте access_token з попереднього запиту)
curl -X GET http://localhost:8080/api/devices \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Перевірка через MQTT клієнт:**
```bash
# Підписка на телеметрію всіх пристроїв
mosquitto_sub -h localhost -p 1883 -t "devices/+/telemetry"
```

### Надсилання команд до пристрою

**Через Hub API:**
```bash
curl -X POST http://localhost:8080/api/devices/DEVICE_UUID/commands \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":"turn_on","value":true}'
```

### Прив'язка пристрою до користувача

Поки пристрій не прив'язаний, він доступний усім користувачам у списку. Після прив'язки його бачить лише власник (або ADMIN).

```bash
curl -X POST http://localhost:8080/api/devices/DEVICE_UUID/claim \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Зміна одиниць температури (за mqttClientId)

**Через Hub API:**
```bash
curl -X POST http://localhost:8080/api/devices/by-client/CLIENT_UUID/temperature-unit \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"unit":"F"}'
```

**Напряму через MQTT:**
```bash
mosquitto_pub -h localhost -p 1883 \
  -t "devices/device-sim-xxx/cmd" \
  -m '{"action":"turn_on","value":true}'
```

### Troubleshooting

**Device Simulator не підключається:**
1. Перевірте, чи запущений MQTT брокер: `docker-compose ps mosquitto`
2. Перевірте URL брокера (в Docker: `tcp://mosquitto:1883`)
3. Перевірте логі: `docker-compose logs device-simulator`

**Телеметрія не надходить до Hub:**
1. Перевірте, чи Hub підключений до MQTT: `docker-compose logs hub | grep -i mqtt`
2. Перевірте, чи Hub підписаний на правильний топик: `devices/+/telemetry`
3. Перевірте, чи пристрій зареєстрований в Hub

---

## API Документація

### Аутентифікація

Аутентифікація тепер обробляється через Keycloak. Реєстрація та вхід виконуються через Keycloak endpoints.

#### Отримання токену від Keycloak
```bash
POST http://localhost:8090/realms/smarthome/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

username=admin&password=admin123!&grant_type=password&client_id=smart-home-hub&client_secret=YOUR_CLIENT_SECRET

Response: {
  "access_token": "eyJhbGc...",
  "expires_in": 3600,
  "refresh_token": "eyJhbGc...",
  "token_type": "Bearer"
}
```

#### Реєстрація користувачів

Є два варіанти:
- **Hub API**: `POST /api/auth/register` (створює користувача в Keycloak через Admin API)
- **Admin Console**: http://localhost:8090 → Users → Create new user

**Використання токену:**
```
Authorization: Bearer <access_token>
```

### Пристрої

#### Список пристроїв
```bash
GET /api/devices
Authorization: Bearer <token>
```

#### Створення пристрою (тільки ADMIN)
```bash
POST /api/devices
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Living Room Thermostat",
  "type": "THERMOSTAT"
}
```

#### Отримання пристрою
```bash
GET /api/devices/{id}  # id = UUID
Authorization: Bearer <token>
```

#### Оновлення пристрою (тільки ADMIN)
```bash
PUT /api/devices/{id}  # id = UUID
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Thermo 1",
  "type": "THERMOSTAT"
}
```

#### Видалення пристрою (тільки ADMIN)
```bash
DELETE /api/devices/{id}  # id = UUID
Authorization: Bearer <token>
```

#### Надсилання команди пристрою (тільки ADMIN)
```bash
POST /api/devices/{id}/commands  # id = UUID
Authorization: Bearer <token>
Content-Type: application/json

{
  "action": "set_temperature",
  "value": 22
}
```

#### Прив'язати пристрій до користувача
```bash
POST /api/devices/{id}/claim  # id = UUID
Authorization: Bearer <token>
```

#### Зміна одиниць температури (тільки ADMIN)
```bash
POST /api/devices/by-client/{clientId}/temperature-unit  # clientId = UUID
Authorization: Bearer <token>
Content-Type: application/json

{
  "unit": "C"
}
```

### Телеметрія

#### Останні дані телеметрії
```bash
GET /api/telemetry/devices/{deviceId}/latest  # deviceId = UUID
Authorization: Bearer <token>
```

---

## Безпека

### Keycloak OAuth2 автентифікація

- Токени видаються Keycloak сервером
- JWT токени з підписом від Keycloak
- Термін дії налаштовується в Keycloak (за замовчуванням: 1 година)
- Автоматична валідація через Spring Security OAuth2 Resource Server
- Ролі з Keycloak автоматично конвертуються в Spring Security authorities

### RBAC (Role-Based Access Control)

- Метод-рівнева авторизація через `@PreAuthorize`
- ADMIN: повний доступ
- USER: тільки читання

### Шифрування

- REST API: HTTP (dev) / HTTPS (prod)
- MQTT: TCP (dev) / TLS (prod)
- Паролі: BCrypt хешування

### Security Rationale (STRIDE)

**API Security:**
- JWT-based authentication with HMAC-256 secret, short-lived tokens, issuer claim
- RBAC via Spring Security with roles: ADMIN, USER, DEVICE
- Method-level authorization using `@PreAuthorize`

**Transport Security:**
- REST over TLS (prod profile enables HTTPS)
- MQTT over TLS (broker requires TLS)
- Mutual TLS for devices (broker validates client certs; identity as username)

**OWASP IoT Top 10 Mitigations:**
- Weak/default credentials: BCrypt password hashing, mandatory registration
- Insecure ecosystem interfaces: enforce TLS, JWT auth, RBAC
- Insecure network services: MQTT over TLS, mutual TLS required
- Lack of transport encryption: TLS everywhere (REST + MQTT)
- Privacy concerns: scoped telemetry, least privilege, logging controls

### Threat Model (STRIDE)

- **Spoofing**: JWT with signature verification; mutual TLS device identity; broker enforces client certs
- **Tampering**: TLS prevents in-transit tampering; database writes are validated
- **Repudiation**: Authenticated actions with roles; application logs
- **Information Disclosure**: TLS; method-level access control; minimal telemetry exposure
- **Denial of Service**: MQTT QoS control; timeouts; reconnection handling
- **Elevation of Privilege**: RBAC via `@PreAuthorize`, least-privilege roles

---

## Технологічний стек

- **Backend**: Spring Boot 3.3.5, Java 21
- **Security**: Spring Security OAuth2 Resource Server, Keycloak 25.0
- **Database**: PostgreSQL 16
- **MQTT**: Eclipse Paho MQTT Client 1.2.5, Mosquitto Broker 2
- **Build**: Maven 3.9
- **Containerization**: Docker, Docker Compose
- **Mapping**: MapStruct 1.6.2
- **Logging**: SLF4J, Logback

---

## База даних

### Таблиці

1. **devices** - IoT пристрої
   - id, name, type, status (ONLINE/OFFLINE), mqtt_client_id, created_at, updated_at

2. **device_telemetry** - телеметрія пристроїв
   - id, device_id, payload (JSON), created_at

---

## Автоматичні процеси

### Налаштування Keycloak

Після першого запуску виконайте bootstrap-скрипт:

```bash
./deploy/keycloak/bootstrap.sh
```

або на Windows:

```powershell
.\deploy\keycloak\bootstrap.ps1
```

Детальні інструкції: [KEYCLOAK-SETUP.md](KEYCLOAK-SETUP.md)

**⚠️ УВАГА**: Змініть пароль адміністратора Keycloak перед production!

### Автоматична реєстрація пристроїв

Коли пристрій публікує телеметрію вперше, Hub автоматично створює запис в базі даних з `mqttClientId = clientId`.

### Оновлення статусу пристроїв

При отриманні телеметрії статус оновлюється на ONLINE.

---

## MQTT Flow

### Підключення пристроїв

- Devices connect to broker using MQTTS with mutual TLS (client certs)
- Devices publish telemetry to `devices/{clientId}/telemetry`
- Hub subscribes to telemetry filter `devices/+/telemetry` and ingests messages into DB
- Hub publishes commands to `devices/{clientId}/cmd`

### Authentication Flow

1. User registers via `POST /api/auth/register` (Hub creates user in Keycloak)
2. User logs in via Keycloak token endpoint
3. Client calls secured endpoints with `Authorization: Bearer <token>`
4. Hub validates JWT via Keycloak issuer

---

## Production Checklist

- ✅ Replace JWT secret with long, random base64 value (>= 256-bit)
- ✅ Use PostgreSQL with restricted credentials and TLS
- ✅ Harden TLS (ECDHE, modern ciphers), rotate certs
- ✅ Enable HTTPS (prod profile) with real certificates
- ⚠️ Centralize secrets via Vault/KMS
- ⚠️ Add rate limiting and audit logging
- ⚠️ Change default admin password
- ⚠️ Configure proper firewall rules
- ⚠️ Set up monitoring and alerting

---

## Можливості для розширення

- WebSocket для real-time оновлень
- Web UI для управління
- Графіки телеметрії
- Правила автоматизації (if-then)
- Підтримка різних типів пристроїв
- Історія команд
- Сповіщення та алерти
- Експорт даних
- API версіонування
- Webhooks для інтеграцій

---

## Troubleshooting

### Hub не запускається

1. Перевірте логи: `docker-compose logs hub`
2. Перевірте, чи PostgreSQL доступний
3. Перевірте, чи MQTT брокер доступний
4. Перевірте змінні середовища

### Device Simulator не підключається

1. Перевірте, чи запущений MQTT брокер
2. Перевірте URL брокера
3. Перевірте логі: `docker-compose logs device-simulator`

### Телеметрія не надходить

1. Перевірте, чи Hub підключений до MQTT
2. Перевірте, чи Hub підписаний на правильний топик
3. Перевірте, чи пристрій зареєстрований в Hub

---

## Ліцензія

Diploma-level Prototype

---

## Автор

Smart Home Secure - Secure IoT Management System
