package util.jmx

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.16 16:13
  * Description: Поддержка DI для JMX-утили.
  * [[JmxImpl]] живёт отдельно от всех и над всеми, живёт в фоне, поэтому нужно его делать eagerly.
  */
class DiModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[JmxImpl]
        .toSelf
        .eagerly()
    )
  }
}
