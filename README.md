# Suggest.io -- Design, radio-signals, nodes graph.
Suggest.io is concept solution to simplify create rich eye-candy content, attached to regions on the geo-map or
Bluetooth (and possibly others) physical radio-beacons.

## Contents

- [About](#about)
- [Requirements](#requirements)
  - [Runtime](#runtime)
  - [Development](#development)
- [Getting started](#getting-started)


## About

Suggest.io is an CMS-like + Web + Hybrid Mobile App solution over ElasticSearch storage, used to create design-rich
screen-fit single-page Web, composed into unified nodes graphs and contexted into bluetooth-beacon signals,
geolocation regions, parent nodes, etc. Content placements can be monetized using build-in billing system.

All server, client and shared code written on Scala.
Client-side UI developed using [scalajs-react](https://github.com/japgolly/scalajs-react/)
and translated into JavaScript using [Scala.js](https://www.scala-js.org/) compiler.
Server-side code based on [Play! framework](https://playframework.com/).


## Requirements

### Runtime
TODO Pre-build images/binaries not-yet ready, so see [Development](#development).
- Java 8+
- sbt 1.3+
- ElasticSearch 5.x (Distibuted primary storage. Upgrading to 7.x in progress)
- PostgreSQL (Used for billing)
- SeaWeedFS (Distributed storage for pictures and other files)
- ImageMagick

### Development
- sbt 1.3+
- scala 2.13+
- Node.js

## Getting started
0. Install needed system packages:
  - `pikaur -Sy jdk11-openjdk imagemagick sbt elasticsearch postgresql seaweedfs`
1. Ensure elasticsearch, postgresql, seaweedfs master and volumes started.
2. Go to main server sub-directory:
  - `cd src1/server/www`
2. Install postgresql schema:
  - `cat evolutions/default/schema.sql | sudo -U postgres psql`
3. Create server config:
  - `cp conf/application.conf.example conf/application.conf`
4. Edit `appliction.conf` file according to your needs. Type your admin email into `superusers.emails`.
5. Start the server:
  - `sbt`
  - `project www`
  - `run`
    Command `run` used for dev-mode.
    `runProd` for production,
    `stage` to compile production tarball into `server/www/target/universal/` directory.
6. See console log for created superusers with email typed at step 4.
7. Open browser, go to [login page](http://localhost:9000/id). Wait for compilation finishes. Type login/password from step 6.
8. Superuser also have access to special [/sys/ pages](http://localhost:9000/sys).


