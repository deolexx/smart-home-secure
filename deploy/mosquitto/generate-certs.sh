#!/bin/bash
# Generate TLS certificates for Mosquitto MQTT broker

CERT_DIR="$(cd "$(dirname "$0")/certs" && pwd)"
CA_KEY="$CERT_DIR/ca.key"
CA_CRT="$CERT_DIR/ca.crt"
SERVER_KEY="$CERT_DIR/server.key"
SERVER_CSR="$CERT_DIR/server.csr"
SERVER_CRT="$CERT_DIR/server.crt"

# Create certs directory if it doesn't exist
mkdir -p "$CERT_DIR"

echo "Generating CA private key..."
openssl genrsa -out "$CA_KEY" 2048

echo "Generating CA certificate..."
openssl req -new -x509 -days 3650 -key "$CA_KEY" -out "$CA_CRT" \
    -subj "/C=UA/ST=Kyiv/L=Kyiv/O=SmartHome/CN=SmartHome-CA"

echo "Generating server private key..."
openssl genrsa -out "$SERVER_KEY" 2048

echo "Generating server certificate signing request..."
openssl req -new -key "$SERVER_KEY" -out "$SERVER_CSR" \
    -subj "/C=UA/ST=Kyiv/L=Kyiv/O=SmartHome/CN=mosquitto"

echo "Generating server certificate..."
openssl x509 -req -in "$SERVER_CSR" -CA "$CA_CRT" -CAkey "$CA_KEY" \
    -CAcreateserial -out "$SERVER_CRT" -days 3650 \
    -extensions v3_req -extfile <(
        echo "[v3_req]"
        echo "keyUsage = keyEncipherment, dataEncipherment"
        echo "extendedKeyUsage = serverAuth"
        echo "subjectAltName = @alt_names"
        echo "[alt_names]"
        echo "DNS.1 = mosquitto"
        echo "DNS.2 = localhost"
        echo "IP.1 = 127.0.0.1"
    )

# Clean up CSR
rm -f "$SERVER_CSR"

echo "Setting permissions..."
chmod 600 "$CA_KEY" "$SERVER_KEY"
chmod 644 "$CA_CRT" "$SERVER_CRT"

echo "Certificates generated successfully!"
echo "CA Certificate: $CA_CRT"
echo "Server Certificate: $SERVER_CRT"
echo "Server Key: $SERVER_KEY"
