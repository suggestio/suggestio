package io.suggest.sc.m.in

import diode.data.Pot
import io.suggest.cordova.background.mode.MCBgModeDaemonS
import io.suggest.cordova.background.timer.MCBgTimerS
import io.suggest.daemon.{MDaemonState, MHtmlBgTimerS}
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.2020 22:07
  * Description: Состояние компонентов демонизации выдачи.
  */
object MScDaemon {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MScDaemon] = UnivEq.derive

  lazy val cBgMode = GenLens[MScDaemon]( _.cBgMode )
  lazy val cBgTimer = GenLens[MScDaemon]( _.cBgTimer )
  lazy val htmlBgTimer = GenLens[MScDaemon]( _.htmlBgTimer )
  def fallSleepTimer = GenLens[MScDaemon]( _.fallSleepTimer )
  def state = GenLens[MScDaemon]( _.state )

}


/** Контейнер состояний компонентов подсистемы демонизации выдачи.
  *
  * @param cBgMode Состояние контроллера cordova-bg-mode.
  * @param cBgTimer Состояние контроллера cordova-bg-timer.
  * @param htmlBgTimer Состояние контроллера фонового html-таймера.
  * @param fallSleepTimer Таймер принудительного засыпания для демона на случай ошибок.
  *                       или зависаний процесса сканирования.
  * @param state Текущий режим функционирования подсистемы демонизации.
  */
case class MScDaemon(
                      cBgMode        : MCBgModeDaemonS     = MCBgModeDaemonS.empty,
                      cBgTimer       : MCBgTimerS          = MCBgTimerS.empty,
                      htmlBgTimer    : MHtmlBgTimerS       = MHtmlBgTimerS.empty,
                      fallSleepTimer : Pot[Int]            = Pot.empty,
                      state          : Pot[MDaemonState]   = Pot.empty,
                    )
