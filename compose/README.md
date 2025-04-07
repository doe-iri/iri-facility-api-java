# Docker Compose Environment

This directory contains a Docker Compose script `iri-facility-api.yaml` for running 
the IRI Facility API locally.  The `bin` directory holds scrips for creating a Java
keystore:

1) `build_keystore.sh` will build a Java keystore from X.509 private key and public certificates.
2) `generate_self.sh` will build a self signed certificate for local testing.

The `config` directory holds example `application.yaml` and `logback.xml` files.