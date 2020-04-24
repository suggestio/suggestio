package io.suggest.sc.m.in

import io.suggest.cordova.background.mode.MCBgModeDaemonS
import io.suggest.cordova.background.timer.MCBgTimerS
import io.suggest.daemon.MHtmlBgTimerS
import japgolly.univeq.UnivEq
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

}


/** Контейнер состояний компонентов подсистемы демонизации выдачи.
  *
  * @param cBgMode Состояние контроллера cordova-bg-mode.
  * @param cBgTimer Состояние контроллера cordova-bg-timer.
  * @param htmlBgTimer Состояние контроллера фонового html-таймера.
  */
case class MScDaemon(
                      cBgMode        : MCBgModeDaemonS     = MCBgModeDaemonS.empty,
                      cBgTimer       : MCBgTimerS          = MCBgTimerS.empty,
                      htmlBgTimer    : MHtmlBgTimerS       = MHtmlBgTimerS.empty,
                      fallSleepTimer : Option[Int]         = None,
                    )
