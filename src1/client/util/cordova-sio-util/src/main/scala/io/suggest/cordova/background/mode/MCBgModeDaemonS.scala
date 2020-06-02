package io.suggest.cordova.background.mode

import io.suggest.daemon.MDaemonInitOpts
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

import scala.collection.immutable.HashMap
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.04.2020 14:55
  * Description: Контейнер данных состояния контроллера демонизатора приложения через планин c-p-bg-mode.
  */
object MCBgModeDaemonS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MCBgModeDaemonS] = UnivEq.force

  def initOpts = GenLens[MCBgModeDaemonS]( _.initOpts )
  def listeners = GenLens[MCBgModeDaemonS]( _.listeners )

}


final case class MCBgModeDaemonS(
                                  initOpts       : Option[MDaemonInitOpts]         = None,
                                  listeners      : HashMap[String, js.Function]    = HashMap.empty,
                                ) {

  def isActive: Boolean =
    initOpts.nonEmpty

}
