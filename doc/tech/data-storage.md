## Data storages

Suggest.io project uses several storages:

- *ElasticSearch (ES)*
  Primary NoSQL database. All nodes (including ads, users, pictures metadata, etc), gathered statistics,
  ipgeobase index -- all stored here.
  Clustered multi-master by design, so ES allows to horizontally scale Suggest.io nodes cluster.
  
- *PostgreSQL (PG)*
  Billing transactions processed using SQL RDBMS.
  Billing data contains billed items (e.g. nodes advertisings), offers (item grouping),
  contracts (offers grouping and linking with nodes), transactions.
  ES can't process things atomically, so more traditional way is used here.
  Currently, PG distribution abilities is limited to master-slave clustering.
  In future PG may be replaced with blockchain solution.
  
- *SeaWeedFS (SWFS)*
  Multi-master object (file) storage. Metadata of objects stored in elasticsearch.
  
- *Local filesystem (FS)*
  For pictures manipulations, uploading and other cases, local files/directories are used.
  

As you can see, Suggest.io project can scale cluster near-horizontally, even between data-centers.
PostgreSQL-related limitations are very low-bandwidth, and not a big pain.
