-- https://wiki.archlinux.org/index.php/GitLab#PostgreSQL
-- Надо залогинится на pg-master и создать pg-юзер для peertube.
--
-- kubectl get pods | grep pg-master
-- ...
-- kubectl exec -ti pg-master-deploy-5db498bbf4-22tvd -- /bin/bash
-- psql -U postgres -d template1
-- CREATE USER peertube WITH PASSWORD 'peertube';
-- CREATE DATABASE peertube_prod OWNER peertube TEMPLATE 'template0' ENCODING 'UTF8'; 
-- ^D

CREATE USER peertube WITH PASSWORD 'peertube';

-- для экстеншенов pg_trgm и unaccent при инициализации схемы:
--ALTER USER peertube SUPERUSER;
-- _prod - это суффикс из дефолтового /etc/peertube/production.yaml
-- 
CREATE DATABASE peertube_prod OWNER peertube TEMPLATE 'template0' ENCODING 'UTF8';

