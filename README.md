Smart Home Secure - Diploma-Level Prototype
===========================================

Overview
--------
A secure Smart Home management system consisting of:
- Central Java hub (Spring Boot) with REST API, JWT auth, RBAC, MQTT ingestion
- Simulated IoT devices (Java) using MQTTS with mutual TLS
- TLS for REST (prod profile) and MQTTS for device communication

Modules
-------
- hub: Spring Boot application (REST API, Security, MQTT, JPA)
- device-simulator: Standalone Java app publishing telemetry and receiving commands

Architecture
------------
- Clean layered architecture:
  - Controller: REST endpoints with `@PreAuthorize`
  - Service: business logic, transaction boundaries
  - Repository: JPA repositories
  - Security: JWT issuance/validation, RBAC, filters
  - MQTT: TLS client, telemetry ingestion, commands publishing
  - Persistence: H2 (dev) or PostgreSQL (prod-ready)

Security Rationale
------------------
- API Security (STRIDE: Spoofing, Elevation of Privilege)
  - JWT-based authentication with HMAC-256 secret, short-lived tokens, issuer claim
  - RBAC via Spring Security with roles: ADMIN, USER, DEVICE
  - Method-level authorization using `@PreAuthorize`
- Transport Security (STRIDE: Information Disclosure, Tampering)
  - REST over TLS (prod profile enables HTTPS)
  - MQTT over TLS (broker requires TLS)
  - Mutual TLS for devices (broker validates client certs; identity as username)
- OWASP IoT Top 10 Mitigations
  - Weak/default credentials: BCrypt password hashing, mandatory registration
  - Insecure ecosystem interfaces: enforce TLS, JWT auth, RBAC
  - Insecure network services: MQTT over TLS, mutual TLS required
  - Lack of transport encryption: TLS everywhere (REST + MQTT)
  - Privacy concerns: scoped telemetry, least privilege, logging controls
  - Insecure updates: not implemented in prototype; recommend code signing for firmware

Authentication Flow (Sequence)
-----------------------------
1) User registers via `POST /api/auth/register` (ADMIN seeded separately).
2) User logs in via `POST /api/auth/login` with credentials.
3) Hub issues JWT with roles claim and expiry.
4) Client calls secured endpoints with `Authorization: Bearer <token>`.
5) `JwtAuthenticationFilter` validates token and sets `SecurityContext`.

MQTT Flow
---------
- Devices connect to broker using MQTTS with mutual TLS (client certs)
- Devices publish telemetry to `devices/{clientId}/telemetry`
- Hub subscribes to telemetry filter and ingests messages into DB
- Hub publishes commands to `devices/{clientId}/cmd`

Project Layout
--------------
- hub/
  - domain: `User`, `Role`, `Device`, `DeviceTelemetry`
  - repository: JPA repositories
  - service: `UserService`, `DeviceService`, `TelemetryService`
  - security: `SecurityConfig`, `JwtTokenService`, filters and handlers
  - mqtt: `MqttConfig`, `MqttGateway`
  - controller: `AuthController`, `DeviceController`, `TelemetryController`
  - resources: `application.yml` (dev + prod sections)
- device-simulator/
  - `DeviceSimulatorApplication` (telemetry + command listener)
- deploy/mosquitto: TLS-enabled broker config

Run (Dev)
---------
Prerequisites: Java 17+, Maven 3.6+

Build all:
```
mvn -q -DskipTests package
```

Run hub (dev/H2):
```
cd hub
mvn spring-boot:run
```

Run device simulator (requires broker):
```
cd device-simulator
mvn -q -DskipTests package
java -jar target/device-simulator-1.0.0-jar-with-dependencies.jar
```

Run Mosquitto with TLS
----------------------
1) Generate CA and certs (example; replace password/path as needed):
```
# CA
openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -out ca.crt -subj "/CN=smarthome-ca"

# Server cert
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr -subj "/CN=mosquitto"
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 1825 -sha256

# Device cert (client)
openssl genrsa -out device.key 2048
openssl req -new -key device.key -out device.csr -subj "/CN=device-123"
openssl x509 -req -in device.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out device.crt -days 1825 -sha256
```
2) Place files into `deploy/mosquitto/certs/`: `ca.crt`, `server.crt`, `server.key`
3) Start broker:
```
cd deploy/mosquitto
docker compose up -d
```

Mutual TLS Client Material
--------------------------
Create PKCS#12 for hub and device:
```
# Hub keystore
openssl pkcs12 -export -in server.crt -inkey server.key -out hub-keystore.p12 -name hub -passout pass:changeit

# Device PKCS#12
openssl pkcs12 -export -in device.crt -inkey device.key -out device-keystore.p12 -name device-123 -passout pass:changeit

# Truststore (JKS) containing CA
keytool -importcert -noprompt -alias smarthome-ca -file ca.crt -keystore truststore.jks -storepass changeit
```
Configure paths in:
- `hub/src/main/resources/application.yml` (mqtt.keystore-path/truststore-path)
- Device simulator via environment variables: `SIM_KEYSTORE_PATH`, `SIM_TRUSTSTORE_PATH`, etc.

Example REST Requests
---------------------
Register:
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json
{"username":"alice","email":"alice@example.com","password":"Secret123!"}
```
Login:
```
POST http://localhost:8080/api/auth/login
{"username":"alice","password":"Secret123!"}
```
Auth header example: `Authorization: Bearer <token>`

Devices (ADMIN for write):
```
GET /api/devices
POST /api/devices {"name":"Living Thermostat","type":"THERMOSTAT"}
PUT /api/devices/{id} {"name":"Thermo 1","type":"THERMOSTAT"}
DELETE /api/devices/{id}
POST /api/devices/{id}/commands {"command":"SET_MODE","mode":"ECO"}
```
Telemetry:
```
GET /api/telemetry/devices/{id}/latest
```

Threat Model (STRIDE) Notes
---------------------------
- Spoofing: JWT with signature verification; mutual TLS device identity; broker enforces client certs
- Tampering: TLS prevents in-transit tampering; database writes are validated
- Repudiation: Authenticated actions with roles; application logs
- Information Disclosure: TLS; method-level access control; minimal telemetry exposure
- Denial of Service: MQTT QoS control; timeouts; reconnection handling
- Elevation of Privilege: RBAC via `@PreAuthorize`, least-privilege roles

OWASP IoT Top 10 Mapping
------------------------
- Weak/guessable passwords: BCrypt, strong password recommendation
- Insecure ecosystem interfaces: TLS, JWT, authorization checks
- Insecure network services: broker configured for TLS only
- Lack of transport encryption: TLS across REST and MQTT
- Privacy concerns: scope limited telemetry; secure storage
- Insecure updates: out of scope in prototype; advise signed updates

Production Checklist
--------------------
- Replace JWT secret with long, random base64 value (>= 256-bit)
- Use PostgreSQL with restricted credentials and TLS
- Harden TLS (ECDHE, modern ciphers), rotate certs
- Enable HTTPS (prod profile) with real certificates
- Centralize secrets via Vault/KMS
- Add rate limiting and audit logging
