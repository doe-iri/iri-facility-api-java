#!/bin/sh

# === Configurable values ===
COMMON_NAME="localhost"
CERT_DAYS_VALID=365
KEY_NAME="mykey"
CERT_NAME="mycert"
P12_NAME="keystore.p12"
P12_ALIAS="myalias"
P12_PASSWORD="changeit"

# === Generate private key ===
echo "Generating private key..."
openssl genrsa -out "$KEY_NAME.key" 2048

# === Generate self-signed certificate ===
echo "Generating self-signed certificate..."
openssl req -new -x509 -key "$KEY_NAME.key" -out "$CERT_NAME.crt" -days "$CERT_DAYS_VALID" -subj "/CN=$COMMON_NAME"

# === Create PKCS#12 keystore ===
echo "Creating PKCS#12 keystore..."
openssl pkcs12 -export \
  -in "$CERT_NAME.crt" \
  -inkey "$KEY_NAME.key" \
  -out "$P12_NAME" \
  -name "$P12_ALIAS" \
  -password pass:"$P12_PASSWORD"

echo "Done!"
echo "Generated files:"
echo "  - Private Key: $KEY_NAME.key"
echo "  - Certificate: $CERT_NAME.crt"
echo "  - PKCS#12 Keystore: $P12_NAME (alias: $P12_ALIAS, password: $P12_PASSWORD)"
