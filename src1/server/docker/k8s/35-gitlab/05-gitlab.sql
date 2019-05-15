-- https://wiki.archlinux.org/index.php/GitLab#PostgreSQL
-- Надо залогинится на pg-master и создать pg-юзер для gitlab.
-- Базу пустую создаём: init-скрипт её увидит и сразу грохнет.
--
-- kubectl get pods | grep pg-master
-- ...
-- kubectl exec -ti pg-master-deploy-5db498bbf4-22tvd -- /bin/bash
-- psql -U postgres -d template1

CREATE USER gitlab WITH PASSWORD 'gitlab';
ALTER USER gitlab SUPERUSER;
CREATE DATABASE gitlab OWNER gitlab;
