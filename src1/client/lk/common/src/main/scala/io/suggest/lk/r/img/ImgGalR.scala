package io.suggest.lk.r.img

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.react.ReactCommonUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 12:25
  * Description: Вёрстка галлереи картинок.
  */
object ImgGalR {

  final case class PropsVal(
                             imgUrls      : Seq[String],
                             imgClass     : String,
                             outerClass   : Option[String] = None
                           )

  implicit object ImgGalPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.imgUrls ===* b.imgUrls) &&
        (a.imgClass ===* b.imgClass) &&
        (a.outerClass ===* b.outerClass)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]


  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      p()
        .filter(_.imgUrls.nonEmpty)
        .fold[VdomElement]( ReactCommonUtil.VdomNullElement ) { v =>
          val inner = <.div(
            ^.`class` := Css.Lk.BxSlider.JS_PHOTO_SLIDER,

            v.imgUrls.iterator.zipWithIndex.toVdomArray { case (imgUrl, i) =>
              <.img(
                ^.key     := i.toString,
                ^.`class` := v.imgClass,
                ^.src     := imgUrl
              )
            }
          )

          v.outerClass.fold(inner) { outerClass =>
            <.div(
              ^.`class` := outerClass,
              inner
            )
          }
        }
    }

  }

  val component = ScalaComponent.builder[Props]("ImgGal")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component(p)

}
