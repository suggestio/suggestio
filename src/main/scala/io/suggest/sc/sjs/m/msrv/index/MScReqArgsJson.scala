package io.suggest.sc.sjs.m.msrv.index

import io.suggest.sc.sjs.m.IAppState
import io.suggest.sc.sjs.m.mgeo.MGeoModeIp

import scala.scalajs.js
import io.suggest.sc.ScConstants.ReqArgs._

import scala.scalajs.js.{Dictionary, UndefOr, Any}
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 15:24
 * Description: Client-side версия серверной qs-модели m.sc.ScReqArgs.
 */
trait MScReqArgsJson extends js.Object {

  @JSName(GEO)
  val geo: String = js.native

  @JSName(SCREEN)
  val screen: String = js.native

  @JSName(WITH_WELCOME)
  val withWelcome: UndefOr[Boolean] = js.native

  @JSName(VSN)
  val vsn: Int = js.native

}


/** Статическая сторона модели. */
object MScReqArgsJson {

  /**
   * Собрать экземпляр модели на основе имеющихся данных.
   * @param withWelcome Ручное управление отображением карточки приветствия.
   * @param state Состояние приложения.
   * @return Экземпляр [[MScReqArgsJson]].
   */
  def apply(withWelcome: Option[Boolean] = None)(implicit state: IAppState): MScReqArgsJson = {
    val d = Dictionary[Any](
      GEO -> state.location
        .geoLoc
        .getOrElse(MGeoModeIp)
        .toQsStr,
      SCREEN -> state.agent
        .availableScreen
        .toQsValue,
      VSN -> state.srv.apiVsn
    )
    if (withWelcome.isDefined) {
      d.update(WITH_WELCOME, withWelcome.get)
    }
    d.asInstanceOf[MScReqArgsJson]
  }

}
