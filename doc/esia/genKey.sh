#!/bin/bash

## Генерация корректного сертификата для подписи.
## https://ru.stackoverflow.com/a/469364

SECRET_KEY="secret.key"
KEYSTORE="siocerts.p12"
CSR="request.csr"
CERTIFICATE="cert.crt"

openssl req -nodes -sha256 -newkey rsa:2048 -keyout $SECRET_KEY -out $CSR -subj /C=RU/ST=Sankt-Peterburg/L=city/O=CBCA\/emailAddress=admin@suggest.io/

openssl x509 -req -sha256 -days 3650 -in $CSR -signkey $SECRET_KEY -out $CERTIFICATE -extfile ./ossl-ext.cnf -extensions esia_ca

#openssl x509 -in $CERTIFICATE -text

## Объединение ключа и сертификата в единый keystore, пригодный для работы системы:
openssl pkcs12 -export -inkey $SECRET_KEY -in $CERTIFICATE -name esia.mykey -out $KEYSTORE

## Заливка сертификатов ЕСИА в keystore:
keytool -import -alias esia.test -keystore $KEYSTORE -file certs/esia.test.cer
keytool -import -alias esia.prod -keystore $KEYSTORE -file certs/esia.prod.cer
