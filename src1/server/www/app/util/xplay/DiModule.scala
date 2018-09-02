package util.xplay

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.18 22:30
  * Description: Конфигуратор для xplay-пакета.
  */
class DiModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[SecretModelsInit]
        .toSelf
        .eagerly()
    )
  }
}
