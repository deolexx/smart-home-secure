# Keycloak Setup Guide

## Швидкий старт

Keycloak автоматично запускається з усім стеком:

```bash
docker-compose up -d
```

## Доступ до Keycloak

- **Admin Console**: http://localhost:8090
- **Username**: `admin`
- **Password**: `admin123!`

## Налаштування Realm

### 1. Створення Realm

1. Увійдіть в Admin Console
2. Натисніть на "Create Realm"
3. Введіть назву: `smarthome`
4. Натисніть "Create"

### 2. Налаштування Client

1. В меню зліва виберіть "Clients"
2. Натисніть "Create client"
3. **Client ID**: `smart-home-hub`
4. **Client authentication**: Enabled
5. **Authorization**: Enabled (опціонально)
6. Натисніть "Next"
7. **Valid redirect URIs**: `http://localhost:8080/*`
8. **Web origins**: `http://localhost:8080`
9. **Access token settings**:
   - Access token lifespan: 1 hour
   - Client authentication: On
10. Натисніть "Save"

### 3. Налаштування Client Credentials

Після створення client:

1. Перейдіть на вкладку "Credentials"
2. Скопіюйте **Client Secret** - він потрібен для налаштування Hub

### 4. Створення Roles

1. В меню зліва виберіть "Realm roles"
2. Створіть ролі:
   - `ADMIN`
   - `USER`
   - `DEVICE`

### 5. Створення користувачів

1. В меню зліва виберіть "Users"
2. Натисніть "Create new user"
3. Заповніть:
   - **Username**: `admin`
   - **Email**: `admin@local`
   - **Email verified**: Enabled
4. Натисніть "Create"
5. Перейдіть на вкладку "Credentials"
6. Встановіть пароль:
   - **Password**: `admin123!`
   - **Temporary**: Off
7. Натисніть "Set password"
8. Перейдіть на вкладку "Role mapping"
9. Додайте роль `ADMIN` до "Assigned roles"

### 6. Налаштування Client Roles (опціонально)

Для більш детального контролю можна створити client roles:

1. Перейдіть до Client `smart-home-hub`
2. Вкладка "Roles"
3. Створіть ролі: `ADMIN`, `USER`, `DEVICE`
4. Призначте ролі користувачам через "Users" → "Role mapping" → "Filter by clients"

## Отримання токену

### Через curl

```bash
# Отримати токен
curl -X POST http://localhost:8090/realms/smarthome/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin123!" \
  -d "grant_type=password" \
  -d "client_id=smart-home-hub" \
  -d "client_secret=YOUR_CLIENT_SECRET"

# Response:
# {
#   "access_token": "eyJhbGc...",
#   "expires_in": 3600,
#   "refresh_expires_in": 1800,
#   "refresh_token": "eyJhbGc...",
#   "token_type": "Bearer"
# }
```

### Використання токену

```bash
curl -X GET http://localhost:8080/api/devices \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Налаштування Hub

Hub автоматично налаштований для роботи з Keycloak через змінні середовища:

```yaml
hub:
  environment:
    - KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/smarthome
    - KEYCLOAK_CLIENT_ID=smart-home-hub
```

## Mapper для ролей

Keycloak автоматично додає ролі в токен через claim `realm_access.roles`. 
Hub автоматично конвертує їх в `ROLE_ADMIN`, `ROLE_USER`, тощо.

Якщо потрібно використовувати client roles:

1. Перейдіть до Client → "Mappers"
2. Створіть mapper:
   - **Name**: `client-roles`
   - **Mapper Type**: `User Client Role`
   - **Client ID**: `smart-home-hub`
   - **Token Claim Name**: `resource_access.smart-home-hub.roles`
   - **Add to access token**: On

## Production налаштування

1. **Змініть пароль адміністратора**
2. **Використовуйте HTTPS** для Keycloak
3. **Налаштуйте database** для Keycloak (вже налаштовано в docker-compose)
4. **Обмежте доступ** до Admin Console
5. **Налаштуйте email** для реєстрації та відновлення паролів
6. **Увімкніть 2FA** для адміністраторів
7. **Налаштуйте token lifespan** відповідно до вимог безпеки

## Troubleshooting

### Hub не може підключитися до Keycloak

1. Перевірте, чи Keycloak запущений: `docker-compose ps keycloak`
2. Перевірте логи: `docker-compose logs keycloak`
3. Перевірте `KEYCLOAK_ISSUER_URI` в docker-compose.yml
4. Перевірте, чи realm `smarthome` створений

### Токен не валідується

1. Перевірте, чи issuer URI правильний
2. Перевірте, чи client ID правильний
3. Перевірте, чи токен не прострочений
4. Перевірте логи Hub: `docker-compose logs hub`

### Ролі не працюють

1. Перевірте, чи ролі призначені користувачу в Keycloak
2. Перевірте, чи ролі додані в access token (перевірте токен на jwt.io)
3. Перевірте, чи mapper налаштований правильно
