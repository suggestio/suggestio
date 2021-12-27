# Suggest.io: Your offer may be here

![Suggest.io showcase](doc/images/showcase-demo-moscow-2021.gif)

Suggest.io is conceptual solution to simply create rich eye-candy cross-screen content and
simply attach it to physical locations via:
- [NFC tags](doc/tech/nfc.md)
- [Wi-Fi zones](doc/tech/wifi.md)
- [Bluetooth radio-beacons (EddyStone technology)](doc/tech/bluetooth-beacons.md)
- [Geographical regions on the map](doc/tech/cabinet/adv-geo.md#geo-area-advertising)
- [Internal nodes hierarchy](doc/tech/cabinet/adv-geo.md#node-level-advertising)
- [In tags advertising](doc/tech/cabinet/adv-geo.md#tags-advertising)
- etc.

## Contents

- [About](#about)
- [Goals](#goals)
- [Requirements](#requirements)
  - [Runtime](#runtime)
  - [Development](#development)
- [Getting started](#getting-started)


## About

Suggest.io is an CMS-like + Web + Hybrid Mobile App solution over ElasticSearch storage, used to create design-rich
screen-fit single-page Web, composed into unified nodes graphs and contexted into radio-beacon signals,
geolocation regions, nodes graph, etc. Content placements can be monetized using build-in billing system.

Server, client and shared code written on Scala.
Client-side UI developed using [scalajs-react](https://github.com/japgolly/scalajs-react/) over [React.js](https://reactjs.org/),
and translated into JavaScript using [Scala.js](https://www.scala-js.org/) compiler.
Server-side code based on [Play! framework](https://playframework.com/).

Some small parts contains non-scala code due to historical reasons.

## Goals
- More content placement dimensions.
  Current usual Internet structured into sites, and pages mapped to URLs.
  Content may be also transparently attached to geographical regions, radio-beacon signals, NFC-tags, geo/node-tags, etc.
  Abstract out from virtual and physical dimensions.
- Create a simple representation into the internet for end-users.
  Currently, sites+domains+certificates+programmers+html+designers+etc+etc have
  too high cost for small business.
- Extended navigation dimensions: usual site page-to-page navigation may be extended via extending current content view
  with new content.
- Page can contain apps, app can show pages. Single-page view can become app. Abstract over browsers and apps.
- Many screens -- one content. Abstract over mobiles, tables and PCs screens. [Be responsible](doc/tech/showcase/showcase.md#responsive).
- Become distributed. Current implementation designed with horizontal cluster scaling in mind.
  In future become more federated/distributed using blockchains/git/activitypub/etc as underlying storage.

## Parts
There are three main parts of Suggest.io:
- [Showcase](doc/tech/showcase/showcase.md) - Suggest.io start page (JS WebApp & Hybrid mobile app).
- Private cabinet: registered user area. Create, edit and [advertise](doc/tech/cabinet/adv-geo.md) nodes/content/etc.
- System: restricted technical area for super-users and developers.

Also, there are shared client-server Scala-code, placed into [src/shared](src1/shared) directory.
Such code includes common models and utilities, protocols, etc.

## Requirements

### Runtime
- GraalVM jdk 11-15
- sbt 1.3+
- ElasticSearch 7 - Distibuted primary storage.
- PostgreSQL (Used for billing)
- SeaWeedFS (Distributed storage for pictures and other files)
- ImageMagick
- openjdk-8 for building cordova android app.
 
TODO Pre-build images/binaries not-yet ready, so see [Development](#development).

### Development
- sbt 1.3+
- scala 2.13+
- Node.js (for prepare assets and compile client-side code)

## Getting started
0. Install needed system packages:
  - `pikaur -Sy jdk11-graalvm-bin imagemagick sbt elasticsearch-xpack postgresql seaweedfs`
1. Ensure elasticsearch, postgresql, seaweedfs master and volumes started.
2. Go to main server sub-directory:
  - `cd src1/server/www`
2. Install postgresql schema:
  - `cat evolutions/default/schema.sql | sudo -U postgres psql`
3. Create server config:
  - `cp conf/application.conf.example conf/application.conf`
4. Edit `application.conf` file according to your needs. Type your admin email into `superusers.emails`.
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


## Sponsors & partners
Development of Suggest.io has been invested by "Clever Boys Communication Agency" LLC ("CBCA"),
formerly "Aversimage" LLC.
Exclusive thanks to **Alexandr Shumeyko**, the founder and owner of CBCA/Aversimage companies.

Inbetween 2011-2021 years total investments from **Alexandr Shumeyko** are **&euro;350'000**.


## License
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License.
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.

