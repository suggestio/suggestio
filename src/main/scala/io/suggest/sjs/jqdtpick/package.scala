package io.suggest.sjs

import org.scalajs.jquery.JQuery

import scala.scalajs.js
import scala.language.implicitConversions
import scala.scalajs.js.Date

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.02.16 14:45
 * Description: Some common package stuff.
 */
package object jqdtpick {

  type Callback_t = js.Function2[Date, JqDtPicker, _]

  /** Casting from JQuery instance. */
  implicit def fromJq(jQuery: JQuery): JqDtPicker = {
    jQuery.asInstanceOf[JqDtPicker]
  }

}
