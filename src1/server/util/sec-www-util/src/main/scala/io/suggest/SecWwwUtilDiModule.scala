package io.suggest

import io.suggest.sec.util.{PgpUtil, ScryptUtil, SecInitUtil}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

final class SecWwwUtilDiModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] = {
    // One-shot side-effection stuff:
    (new ScryptUtil).disableNativeCode()
    (new SecInitUtil).ensureBcJce()

    // TODO Only first run initialization needed. eagerly() causes useless singletons in memory here.
    val pgpBind = bind[PgpUtil]
      .toSelf
      .eagerly()

    pgpBind :: Nil
  }

}
