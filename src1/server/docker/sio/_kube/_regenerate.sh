#!/bin/sh

## Скрипт для перегенерации kubernetes-файлов на основе вышележащего docker-compose-файла.

rm ./*.yaml
kompose convert -f ../docker-compose.yml

