package io.suggest.sc.m

import io.suggest.geo.{GeoLocType, MGeoLoc, PositionException}
import io.suggest.routes.scRoutes
import io.suggest.sc.m.Sc3Pages.MainScreen
import io.suggest.spa.DAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 18:30
  * Description: Корневые экшены sc3.
  */

/** Интерфейс корневых экшенов. */
sealed trait IScRootAction extends DAction

/** Запустить инициализацию js-роутера. */
case object JsRouterInit extends IScRootAction

/** Сигнал основной цепочке о состоянии основного js-роутера. */
case class JsRouterStatus( payload: Try[scRoutes.type] ) extends IScRootAction


/** События экрана. */
case object ScreenReset extends IScRootAction

/** Сработал таймер непосредственного запуска действий при ресайзе. */
case object ScreenRszTimer extends IScRootAction


/** Управление подсистемой */
case class GeoLocOnOff(enabled: Boolean) extends IScRootAction


/** Есть координаты. */
case class GlLocation(glType: GeoLocType, location: MGeoLoc) extends IScRootAction
/** Ошибка получения координат. */
case class GlError(glType: GeoLocType, error: PositionException) extends IScRootAction

/** Сработал таймер подавления нежелательных координат. */
case class GlSuppressTimeout(generation: Long) extends IScRootAction


/** Из js-роутера пришла весточка, что нужно обновить состояние из данных в URL. */
case class RouteTo( mainScreen: MainScreen ) extends IScRootAction

/** Команда к обновлению ссылки в адресе согласно обновившемуся состоянию. */
case object ResetUrlRoute extends IScRootAction

