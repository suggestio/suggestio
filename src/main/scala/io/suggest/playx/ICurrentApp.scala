package io.suggest.playx

import play.api.{Application, Configuration, Mode}

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


trait ICurrentAppHelpers extends ICurrentApp {

  def isDev   = current.mode == Mode.Dev
  def isProd  = current.mode == Mode.Prod
  def isTest  = current.mode == Mode.Test

}


trait IConfiguration {
  def configuration: Configuration
}


/** Трейт для быстрого доступа к play config через DI Application. */
trait ICurrentConf extends ICurrentApp with IConfiguration {

  /** Доступ к play config через экземпляр Application, вброшенный через DI. */
  def configuration = current.configuration

}
