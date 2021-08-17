package io.suggest

import io.suggest.sec.util.{ScryptUtil, SecInitUtil}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

final class SecWwwUtilDiModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] = {
    // One-shot side-effection stuff:
    (new ScryptUtil).disableNativeCode()
    (new SecInitUtil).ensureBcJce()

    // Nothing to bind, everything already initialized impure.
    Nil
  }

}
