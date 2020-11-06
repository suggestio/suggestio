-- Надо залогинится на pg-master и инициализировать БД peertube_prod эктеншенами,
-- это может сделать только суперюзер.
--
-- kubectl get pods | grep pg-master
-- ...
-- kubectl exec -ti pg-master-deploy-5db498bbf4-22tvd -- /bin/bash
-- psql -U postgres -d peertube_prod
-- CREATE EXTENSION pg_trgm;
-- CREATE EXTENSION unaccent;
-- ^D

CREATE EXTENSION pg_trgm;
CREATE EXTENSION unaccent;
