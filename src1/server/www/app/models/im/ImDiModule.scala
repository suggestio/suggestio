package models.im

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

final class ImDiModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] = {
    // Single-shot side-effecting stuff:
    new MLocalImgsConf( environment, configuration )
      .ensureStorageDirectories()

    Nil
  }

}
