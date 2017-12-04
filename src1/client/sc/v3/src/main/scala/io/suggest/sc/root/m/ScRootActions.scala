package io.suggest.sc.root.m

import io.suggest.routes.scRoutes
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

/** Сигнал основной цепочке о состоянии основного js-роутера. */
case class JsRouterStatus( payload: Try[scRoutes.type] ) extends IScRootAction


/** События экрана. */
case object ScreenReset extends IScRootAction

/** Сработал таймер непосредственного запуска действий при ресайзе. */
case object ScreenRszTimer extends IScRootAction


/** Управление подсистемой */
case class GeoOnOff(enabled: Boolean) extends IScRootAction
