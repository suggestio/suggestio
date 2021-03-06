package io.suggest.sjs.dom2

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.12.18 12:45
  * Description: ScalaJSDefined-трейты вместо нативных var-костылей.
  * Это модифицированные интерфейсы, т.к. нативные не очень сподручны или имеют ошибки.
  */

/** Перепев стандартных PositionOptions, т.к. неудобно их просто использовать. */
trait PositionOptions extends js.Object {
  val enableHighAccuracy: js.UndefOr[Boolean] = js.undefined
  // Double для возможности задания +Inf-значений, которые тоже допустимы.
  val timeout: js.UndefOr[Double] = js.undefined
  val maximumAge: js.UndefOr[Double] = js.undefined
}


@js.native
trait Geolocation extends js.Object {

  /** Cordova на андройде (cordova-plugin-geolocation) возвращает UUID, а браузер по стандарту -- Int.
    * Возможно, есть ещё какие-то девиации, поэтому возвращать надо js.Any.
    */
  @JSName("watchPosition")
  def watchPosition2(successCallback: js.Function1[dom.Position, Unit],
                     errorCallback: js.Function1[dom.PositionError, Unit] = js.native,
                     options: dom.PositionOptions = js.native): GeoLocWatchId_t = js.native

  /** Очистка watch'а, возвращённого из watchPosition2(), который js.Any */
  @JSName("clearWatch")
  def clearWatch2(watchId: GeoLocWatchId_t): Unit = js.native

}


/** Замена для dom.Position */
trait Position extends js.Object {
  def timestamp: Double
  val coords: Coordinates
}


/** Координаты. Замена для dom.Coordinates. */
trait Coordinates extends js.Object {
  val altitudeAccuracy,
      speed,
      heading,
      altitude: js.UndefOr[Double] = js.undefined

  val longitude,
      latitude,
      accuracy: Double
}


object PositionError {
  final val PERMISSION_DENIED = 1
  final val POSITION_UNAVAILABLE = 2
  final val TIMEOUT = 3
}
trait PositionError extends js.Object {
  val code: Int
  val message: String
}
