#!/bin/bash

## Подготовка к запуску контейнера сборки s.io.
## Нужно заполнить конфиги ivy2 и sbt, чтобы всё хорошо собиралось.

cd $HOME

SBT_VERSION_ABI=1.0

mkdir -p .ivy2 .sbt/${SBT_VERSION_ABI}

## Заполнить конфиг для локального кэширования артефактов из artifactory:
cat > .ivy2/.credentials <<EOF
realm=$K8S_SECRET_IVY2_REALM
host=$K8S_SECRET_IVY2_HOST
user=$K8S_SECRET_IVY2_USER
password=$K8S_SECRET_IVY2_PASSWORD
EOF

## Подготовить конфиг sbt, чтобы активнее работал с локальной artifactory:
cat > .sbt/${SBT_VERSION_ABI}/global.sbt <<EOF
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

val ARTIFACTORY_URL = "http://${K8S_SECRET_IVY2_HOST}/artifactory"

val corpRepoResolver = "cbca-corp-repo" at s"\$ARTIFACTORY_URL/corp-repo"

publishTo := Some(corpRepoResolver)

// Удалить дефолтовый maven1/repo2, чтобы использовать кеш artifactory для ускорения работы.
externalResolvers ~= { extResolvers =>
  extResolvers.filter (_.name != "public")
}

resolvers ++= Seq(
  "repo1"   at  s"\$ARTIFACTORY_URL/repo1",
  corpRepoResolver
)
EOF


## Продолжить выполнение исходных операций:
exec $@

