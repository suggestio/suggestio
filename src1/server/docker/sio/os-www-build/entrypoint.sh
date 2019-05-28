#!/bin/bash

## Подготовка к запуску контейнера сборки s.io.
## Нужно заполнить конфиги ivy2 и sbt, чтобы всё хорошо собиралось.

set -e

cd $HOME

SBT_VERSION_ABI=1.0

mkdir -p $HOME/.ivy2 $HOME/.sbt/${SBT_VERSION_ABI}

## Заполнить конфиг для локального кэширования артефактов из artifactory:
echo ".ivy2/.credentials..."
cat > $HOME/.ivy2/.credentials <<EOF
realm=$K8S_SECRET_IVY2_REALM
host=$K8S_SECRET_IVY2_HOST
user=$K8S_SECRET_IVY2_USER
password=$K8S_SECRET_IVY2_PASSWORD
EOF

## Подготовить конфиг sbt, чтобы активнее работал с локальной artifactory:
echo "global.sbt..."
cat > $HOME/.sbt/${SBT_VERSION_ABI}/global.sbt <<EOF
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

## Выставить кол-во RAM в sbtopts.
SBT_OPTS="/etc/sbt/sbtopts"
if [ -z "$K8S_SECRET_SBT_MEM" ]; then
  K8S_SECRET_SBT_MEM=3072
fi
echo "-mem $K8S_SECRET_SBT_MEM" > "$SBT_OPTS"

## Продолжить выполнение исходных операций:
exec $@ || exit 0

