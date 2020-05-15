package io.suggest.cordova.background.fetch

import diode.data.Pot
import io.suggest.daemon.MDaemonSleepTimer
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.05.2020 15:42
  * Description: Состояние контроллера cordova-plugin-background-fetch.
  */
object MCBgFetchS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MCBgFetchS] = UnivEq.derive

  def isEnabled = GenLens[MCBgFetchS]( _.isEnabled )
  def opts = GenLens[MCBgFetchS]( _.opts )
  def curTaskId = GenLens[MCBgFetchS]( _.curTaskId )

}


case class MCBgFetchS(
                       isEnabled      : Pot[Boolean]                  = Pot.empty,
                       opts           : Option[MDaemonSleepTimer]     = None,
                       curTaskId      : Option[String]                = None,
                     )
