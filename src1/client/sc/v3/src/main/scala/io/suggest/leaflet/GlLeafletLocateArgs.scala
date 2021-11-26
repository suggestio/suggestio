package io.suggest.leaflet

import io.suggest.sjs.leaflet.map.LocateOptions
import japgolly.univeq._
import monocle.macros.GenLens
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.10.2020 20:10
  * Description: Контейнер данных запроса гео-локации из Leaflet.Map().locate() .
  */
object GlLeafletLocateArgs {
  @inline implicit def univEq: UnivEq[GlLeafletLocateArgs] = UnivEq.force
}
case class GlLeafletLocateArgs(
                                locateOpts: LocateOptions,
                                onLocation: js.Function1[dom.Position, Unit],
                                onLocError: js.Function1[dom.PositionError, Unit],
                              )


object MGlSourceS {
  @inline implicit def univEq: UnivEq[MGlSourceS] = UnivEq.force
  def args = GenLens[MGlSourceS]( _.args )
  def timeoutId = GenLens[MGlSourceS]( _.timeoutId )
}

/** @param args Аргументы, запрошенные из Leaflet.
  * @param timeoutId id таймера для наступления таймаута геолокации.
  */
case class MGlSourceS(
                       args           : GlLeafletLocateArgs,
                       timeoutId      : Option[() => SetTimeoutHandle] = None,
                     )
  extends IGlSourceS
