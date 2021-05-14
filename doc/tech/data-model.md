# Data model.

## Elasticsearch data models
ElasticSearch (ES) documents stored as JSON documents. ES makes all needed indexing according to index mapping,
stores and shards Lucene index, shards are distributed accross cluster.

Since ES-6.x, `_type` field inside index has been removed, so virtual separation between document's types inside index
(looking like SQL tables in database) also gone away. (Now, if `_type`-like separation needed, it can be implemented explicitly).

Suggest.io primary model is a graph-like structure of [MNode](#mnode-model) nodes.
Nodes have one data type: the MNode, but internally separated in field-level: ads, person, ADN-node, picture, video, etc.

Each node is a JSON object, contains one or more fields.
One of the most interesting field of each node is `edges` field, containing object with array-field `out`.
Nodes between each other connected via "edges", implemented as array of [MEdge](#medge-model) sub-documents.
In ES, edges mapped to [nested documents](https://www.elastic.co/guide/en/elasticsearch/reference/current/nested.html).
Talking in SQL terms, node like a table row, and edges is something like sub-table (sub-rows array) inside `edges.out` column.


### MNode model
Every node stored using `MNode`.



### MEdge model

Edge can point to none, one or many nodes.
Edges are typed.
Edges are "too abstract": edges are not only for connect graph node-to-node.
Edge can point to many or zero nodes.
Edge can point to geo.point or geo.shape on the map with zero nodes.
Edge can point to node(s) in context of geo.shape.
Edge can have edge-flags.
Edge may store indexed file metadata.
And others.

