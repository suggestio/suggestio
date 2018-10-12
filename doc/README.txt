SiO2 - suggest.io v2.


Первая сборка:

$ git clone ... sio2
$ cd sio2
$ npm install jsdom   ## TODO не ясно, надо ли это сейчас?
$ sbt

Первый запуск:
$ cd src1/server/www/conf
Инициализировать application.conf, elasticsearch.yml, logger.xml на основе файлов-примеров.
$ cd ../../../..
$ sbt
> project www
> run

