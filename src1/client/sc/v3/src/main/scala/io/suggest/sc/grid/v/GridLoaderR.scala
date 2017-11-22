package io.suggest.sc.grid.v

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.color.MColorData
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.sc.styl.GetScCssF
import io.suggest.svg.SvgConst
import io.suggest.ueq.UnivEqUtil._
import org.scalajs.dom.ext.Color

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.17 17:24
  * Description: Компонент preloader'а в плитке.
  * Он реализован через анимированный SVG, потому что так надо.
  */
class GridLoaderR(
                   getScCssF                  : GetScCssF
                 ) {

  case class PropsVal(
                       fgColor    : MColorData,
                       widthPx    : Option[Int]
                     )
  implicit object GridLoaderPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.fgColor ===* b.fgColor) &&
        (b.widthPx ===* b.widthPx)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsOptProxy: Props): VdomElement = {
      import japgolly.scalajs.react.vdom.html_<^._

      val Css = getScCssF().Grid.Loaders

      propsOptProxy.value.whenDefinedEl { props =>
        <.div(
          Css.outer,
          props.widthPx.whenDefined { widthPx =>
            ^.width := widthPx.px
          },

          // TODO Width по фактической ширине внешнего контейнера плитки.
          <.div(
            Css.spinnerOuter,

            <.div(
              Css.spinnerInner,
              _spinnerSvg( props.fgColor )
            )
          )
        )
      }
    }

  }


  import japgolly.scalajs.react.vdom.svg_<^._


  private def _spinnerSvg(fgColor: MColorData): VdomElement = {
    val szPx = 32
    val sz = szPx.px
    val zero = 0
    val whiteHex = Color.White.toHex
    val fillWhite = {
      ^.fill := whiteHex
    }
    <.svg(
      ^.xmlns := SvgConst.SVG_NAMESPACE_URI,
      ^.width := sz,
      ^.height := sz,
      ^.viewBox := (zero + SPACE + zero + SPACE + szPx + SPACE + szPx),
      fillWhite,

      <.path(
        ^.opacity := 0.25,
        fillWhite,
        ^.d := "M16 0 A16 16 0 0 0 16 32 A16 16 0 0 0 16 0 M16 4 A12 12 0 0 1 16 28 A12 12 0 0 1 16 4"
      ),

      <.path(
        ^.fill := fgColor.hexCode,
        ^.d := "M16 0 A16 16 0 0 1 32 16 L28 16 A12 12 0 0 0 16 4z",

        <.animateTransform(
          ^.attributeName := "transform",
          ^.attributeType := "rotate",
          ^.from := "0 16 16",
          ^.to := "360 16 16",
          ^.dur := "0.8s",
          ^.repeatCount := "indefinite"
        )
      )
    )
  }


  val component = ScalaComponent.builder[Props]("GLdr")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component(propsOptProxy)

}
