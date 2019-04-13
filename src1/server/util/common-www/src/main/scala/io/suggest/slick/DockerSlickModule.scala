package io.suggest.slick

import java.net.URI

import io.suggest.env.DockerEnv
import io.suggest.util.logs.MacroLogsDyn
import play.api.db.slick.SlickModule
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.19 11:51
  * Description: Вместо SlickModule используем эту надстройку, которая переопределяет конфиг
  * для slick на основе возможных переменных окружения docker'а.
  */
final class DockerSlickModule extends Module with MacroLogsDyn {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    // Нужно понять, произошёл ли запуск в docker-окружении?
    // Если да, то подменить db url в конфиге.
    val ck = "slick.dbs.default.db.url"

    val conf2Opt = for {
      // Настроена ли БД в исходном конфиге?
      dbUrlStr0 <- configuration.getOptional[String](ck)

      // Поискать признаки docker-окружения:
      dockerHostPorts = ("PG_MASTER" :: "PG_SLAVE" :: Nil)
        .iterator
        .flatMap { serviceName =>
          DockerEnv
            .getLinkHostPort( serviceName, 5432, proto = "TCP" )
        }
        .toStream
      if dockerHostPorts.nonEmpty

    } yield {
      // Это docker-окружение, и заданы какие-то порты в env, и база в конфиге. Надо переопределить db URL:
      val dbUri0 = new URI( dbUrlStr0 )
      val hostPart2 = dockerHostPorts
        .iterator
        .map { case (host, port) =>
          s"$host:$port"
        }
        .mkString(",")

      val dbUriStr2 = dbUri0.getScheme + "://" +
        hostPart2 +
        Option(dbUri0.getRawPath).getOrElse("") +
        Option(dbUri0.getRawQuery).getOrElse("")

      LOGGER.info(s"Docker env detected: pg_hosts = ${hostPart2}")

      configuration ++ Configuration(ck -> dbUriStr2)
    }

    val conf2 = conf2Opt getOrElse configuration

    new SlickModule().bindings(environment, conf2)
  }

}
