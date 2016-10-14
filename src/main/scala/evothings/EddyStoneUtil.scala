package evothings

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 22:18
  * Description: Scala.js APIs for evothings/eddystone.js.
  */
@JSName("evothings.eddystone")
@js.native
object EddyStoneUtil extends js.Object {

  def calculateAccuracy(txPower: Int, rssi: Int): Double = js.native

  def createLowPassFilter(cutOff: Double, state: Double = js.native): LowPassFilter = js.native

}


@js.native
sealed trait LowPassFilter extends js.Object {

  var filter: js.Function1[Double, Double] = js.native

  var value: js.Function0[Double] = js.native

}
