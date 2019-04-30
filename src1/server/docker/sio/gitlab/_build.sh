#docker build -t docker-registry.suggest.io/sio/gitlab:latest .
## Т.к. есть симлинк на moduli, необходимо проводить derefernce симв.ссылок через tar -h:
tar -czh . | docker build -t docker-registry.suggest.io/sio/gitlab:latest -
