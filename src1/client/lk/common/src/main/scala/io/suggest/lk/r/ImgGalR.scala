package io.suggest.lk.r

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

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
      (a.imgUrls eq b.imgUrls) &&
        (a.imgClass eq b.imgClass) &&
        (a.outerClass eq b.outerClass)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]


  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): ReactElement = {
      p()
        .filter(_.imgUrls.nonEmpty)
        .fold[ReactElement](null) { v =>
          val inner = <.div(
            ^.`class` := Css.Lk.BxSlider.JS_PHOTO_SLIDER,

            for {
              (imgUrl, i) <- v.imgUrls.zipWithIndex
            } yield {
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

  val component = ReactComponentB[Props]("ImgGal")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component(p)

}
