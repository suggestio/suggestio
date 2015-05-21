package io.suggest.sc.sjs.m

import io.suggest.sc.sjs.m.magent.IAppStateAgent
import io.suggest.sc.sjs.m.mgeo.{MAppStateLocation, IAppStateLocation}
import io.suggest.sc.sjs.m.msrv.{MAppStateSrv, IAppStateSrv}
import io.suggest.sc.sjs.v.render.IRenderer
import io.suggest.sjs.common.view.SafeDocument

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 10:34
 * Description: Модель глобального состояния приложения.
 * Формируется и поддерживается где-то на верхнем уровне системы и передается в нижележащие компоненты.
 * Здесь интерфейс модели и её реализация.
 */
trait IAppState {

  /** Доступ к динамическим сущностям, относящимся к backend-серверу suggest.io. */
  def srv: IAppStateSrv

  /** API для доступа к системам и данным локации. */
  def location: IAppStateLocation

  /** API для доступа к данным/фунциям user-agent'а, т.е. рантаймовые данные по браузеру/приложению и т.д. */
  def agent: IAppStateAgent

  /** Враппер для более безопасного доступа к document. */
  def safeDoc: SafeDocument

  /** Используемый рендерер. */
  def renderer: IRenderer

}

case class MAppState(
  override val srv       : MAppStateSrv,
  override val location  : MAppStateLocation,
  override val agent     : IAppStateAgent,
  override val safeDoc   : SafeDocument,
  override val renderer  : IRenderer
)
  extends IAppState
