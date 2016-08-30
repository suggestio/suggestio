package util.es

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.16 17:26
  * Description: Конфигуратор поддержки DI для нужд util.es.
  */
class DiModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[SiowebEsModel]
        .toSelf
        .eagerly()
    )
  }
}
