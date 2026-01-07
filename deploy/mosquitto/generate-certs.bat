@echo off
REM Generate TLS certificates for Mosquitto MQTT broker

set CERT_DIR=%~dp0certs
if not exist "%CERT_DIR%" mkdir "%CERT_DIR%"

echo Generating CA private key...
openssl genrsa -out "%CERT_DIR%\ca.key" 2048

echo Generating CA certificate...
openssl req -new -x509 -days 3650 -key "%CERT_DIR%\ca.key" -out "%CERT_DIR%\ca.crt" ^
    -subj "/C=UA/ST=Kyiv/L=Kyiv/O=SmartHome/CN=SmartHome-CA"

echo Generating server private key...
openssl genrsa -out "%CERT_DIR%\server.key" 2048

echo Generating server certificate signing request...
openssl req -new -key "%CERT_DIR%\server.key" -out "%CERT_DIR%\server.csr" ^
    -subj "/C=UA/ST=Kyiv/L=Kyiv/O=SmartHome/CN=mosquitto"

echo Generating server certificate...
openssl x509 -req -in "%CERT_DIR%\server.csr" -CA "%CERT_DIR%\ca.crt" -CAkey "%CERT_DIR%\ca.key" ^
    -CAcreateserial -out "%CERT_DIR%\server.crt" -days 3650 ^
    -extensions v3_req -extfile <(
        echo [v3_req]
        echo keyUsage = keyEncipherment, dataEncipherment
        echo extendedKeyUsage = serverAuth
        echo subjectAltName = @alt_names
        echo [alt_names]
        echo DNS.1 = mosquitto
        echo DNS.2 = localhost
        echo IP.1 = 127.0.0.1
    )

REM Clean up CSR
del "%CERT_DIR%\server.csr" 2>nul

echo Certificates generated successfully!
echo CA Certificate: %CERT_DIR%\ca.crt
echo Server Certificate: %CERT_DIR%\server.crt
echo Server Key: %CERT_DIR%\server.key
