# Data storages

Suggest.io project uses several storages:

## ElasticSearch (ES)
Primary NoSQL database. All nodes (including ads, users, pictures metadata, etc), gathered statistics,
ipgeobase index -- all stored here.
Clustered multi-master by design, so ES allows to horizontally scale Suggest.io nodes cluster.

ES models [MNode](node.md) and `MStat` designed to use ES for underlying storage.


## PostgreSQL (PG)
Billing transactions processed using SQL RDBMS.
Billing data contains billed items (e.g. nodes advertisings), offers (item grouping),
contracts (offers grouping and linking with nodes), transactions.
ES can't process things atomically, so more traditional way is used here.
Currently, PG distribution abilities is limited to master-slave clustering.
In future PG may be replaced with blockchain solution.

Models of `mbill2` sub-project (at `src1/server/bill/mbill2/`) uses PG as storage backend
via [Slick](https://slick.lightbend.com/) framework.


## SeaWeedFS (SWFS)
Multi-master object (file) storage. Metadata of objects stored in elasticsearch.
`IMediaStorage` model used as abstraction over SWFS and possibly other storages.
`controllers.Upload` and `MImg3` uses SWFS (via `IMediaStorage`) as underlying file storage.

  
## Local filesystem (FS)
For pictures manipulations, uploading and other cases, local files/directories are used.
Plain-old MLocalImg model used for Filesystem interactions.


# Conclusions  

As you can see, Suggest.io project can scale cluster near-horizontally, even between data-centers.
PostgreSQL-related limitations are very low-bandwidth, and not a big pain.
