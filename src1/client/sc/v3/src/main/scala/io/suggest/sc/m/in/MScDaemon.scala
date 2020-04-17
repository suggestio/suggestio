package io.suggest.sc.m.in

import io.suggest.daemon.cordova.MCbgmDaemonS
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.2020 22:07
  * Description: Состояние демонизаторов
  */
object MScDaemon {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MScDaemon] = UnivEq.derive

  lazy val cordova = GenLens[MScDaemon]( _.cordova )
  lazy val sc = GenLens[MScDaemon]( _.sc )

}


case class MScDaemon(
                       cordova        : MCbgmDaemonS        = MCbgmDaemonS.empty,
                       sc             : MScDaemonInfo       = MScDaemonInfo.empty,
                     )
