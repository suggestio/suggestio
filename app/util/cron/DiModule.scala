package util.cron

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.16 13:54
  * Description: Активация кронтаба при запуске через DI.
  */
class DiModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[Crontab]
        .toSelf
        .eagerly()
    )
  }

}
