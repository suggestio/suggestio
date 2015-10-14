package io.suggest.playx

import play.api.{Configuration, Application}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 11:56
 * Description: Трейт для DI-
 */
trait ICurrentApp {

  /** Экземпляр Application, вброшенный в класс через DI. */
  implicit def current: Application

}


trait IConfiguration {
  def configuration: Configuration
}


/** Трейт для быстрого доступа к play config через DI Application. */
trait ICurrentConf extends ICurrentApp with IConfiguration {

  /** Доступ к play config через экземпляр Application, вброшенный через DI. */
  def configuration = current.configuration

}
