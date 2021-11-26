package io.suggest.sc.model.in

import diode.data.Pot
import io.suggest.cordova.background.fetch.MCBgFetchS
import io.suggest.cordova.background.mode.MCBgModeDaemonS
import io.suggest.cordova.background.timer.MCBgTimerS
import io.suggest.daemon.{MDaemonState, MHtmlBgTimerS}
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

import scala.scalajs.js.timers.SetTimeoutHandle

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.2020 22:07
  * Description: Состояние компонентов демонизации выдачи.
  */
object MScDaemon {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MScDaemon] = UnivEq.derive

  def cdvBgMode = GenLens[MScDaemon]( _.cdvBgMode )
  def cdvBgTimer = GenLens[MScDaemon]( _.cdvBgTimer )
  def cdvBgFetch = GenLens[MScDaemon]( _.cdvBgFetch )
  def htmlBgTimer = GenLens[MScDaemon]( _.htmlBgTimer )
  def fallSleepTimer = GenLens[MScDaemon]( _.fallSleepTimer )
  def state = GenLens[MScDaemon]( _.state )

}


/** Контейнер состояний компонентов подсистемы демонизации выдачи.
  *
  * @param cdvBgMode Состояние контроллера cordova-bg-mode.
  * @param cdvBgTimer Состояние контроллера cordova-bg-timer.
  * @param htmlBgTimer Состояние контроллера фонового html-таймера.
  * @param fallSleepTimer Таймер принудительного засыпания для демона на случай ошибок.
  *                       или зависаний процесса сканирования.
  * @param state Текущий режим функционирования подсистемы демонизации.
  */
case class MScDaemon(
                      cdvBgMode      : MCBgModeDaemonS     = MCBgModeDaemonS.empty,
                      cdvBgTimer     : MCBgTimerS          = MCBgTimerS.empty,
                      cdvBgFetch     : MCBgFetchS          = MCBgFetchS.empty,
                      htmlBgTimer    : MHtmlBgTimerS       = MHtmlBgTimerS.empty,
                      fallSleepTimer : Pot[SetTimeoutHandle] = Pot.empty,
                      state          : Pot[MDaemonState]   = Pot.empty,
                    )
