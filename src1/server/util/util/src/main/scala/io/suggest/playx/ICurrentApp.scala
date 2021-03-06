package io.suggest.playx

import play.api.{Application, Mode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 11:56
 * Description: Трейт для DI-
 */
trait ICurrentApp {

  /** Экземпляр Application, вброшенный в класс через DI. */
  def current: Application

}


trait IsAppModes {

  protected def appMode: Mode

  def isDev   = appMode == Mode.Dev
  def isProd  = appMode == Mode.Prod
  def isTest  = appMode == Mode.Test
}

trait ICurrentAppHelpers extends ICurrentApp with IsAppModes {
  override protected def appMode = current.mode
}
