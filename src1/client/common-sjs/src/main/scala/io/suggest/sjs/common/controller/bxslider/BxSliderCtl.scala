package io.suggest.sjs.common.controller.bxslider

import io.suggest.css.Css
import org.scalajs.dom.Element
import org.scalajs.jquery.{JQuery, jQuery}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined
import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.17 12:57
  * Description: Поддержка jQuery bxSlider.
  */
object BxSliderCtl {

  import JQueryBxSlider._

  def initAll(): Unit = {
    jQuery( "." + Css.Lk.BxSlider.JS_PHOTO_SLIDER )
      .each { (_: Int, el: Element) =>
        val jqEl = jQuery(el)
        val k = "sliderInit"
        if ( js.isUndefined(jqEl.data(k)) ) {
          jqEl
            .bxSlider(
              new BxSliderArgs {
                override val auto: UndefOr[Boolean] = true
                override val pager: UndefOr[Boolean] = false
                override val infiniteLoop: UndefOr[Boolean] = true
                override val hideControlOnEnd: UndefOr[Boolean] = false
              }
            )
            .data(k, true)
        }
      }
  }

}


@js.native
sealed trait JQueryBxSlider extends js.Object {
  var bxSlider: UndefOr[_] = js.native
  def bxSlider(bxSliderArgs: BxSliderArgs): JQuery = js.native
}
object JQueryBxSlider {
  implicit def apply(jq: JQuery): JQueryBxSlider = {
    jq.asInstanceOf[JQueryBxSlider]
  }
}


@ScalaJSDefined
trait BxSliderArgs extends js.Object {

  val auto: UndefOr[Boolean] = js.undefined // true

  val pager: UndefOr[Boolean] = js.undefined  // false

  val infiniteLoop: UndefOr[Boolean] = js.undefined // false

  val hideControlOnEnd: UndefOr[Boolean] = js.undefined //true

}
