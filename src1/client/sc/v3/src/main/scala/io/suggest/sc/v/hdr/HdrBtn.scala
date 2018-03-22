package io.suggest.sc.v.hdr

import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.color.MColorData
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.hdr.IScHdrAction
import io.suggest.svg.SvgConst
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^.{< => html, ^ => htmlAttrs}
import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import scalacss.ScalaCssReact._
import scalacss.internal.StyleA

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 15:35
  * Description: Для сборки компонентов кнопок заголовка можно заюзать этот трейт,
  * дедублицирующий пачку кода.
  */
object HdrBtn {

  val W = 36
  val H = W

  val WIDTH  = ^.width := W.px
  val HEIGHT = ^.height := H.px

  val VIEWPORT = {
    ^.viewBox := "0 0 " + W + " " + H
  }

  def fillHex(fgColorHex: String) = {
    ^.fill := "#" + fgColorHex
  }

  type Props_t = Option[MColorData]
  type Props = ModelProxy[Props_t]

}

trait HdrBtn {

  import HdrBtn.{Props_t, Props}

  protected[this] def cssStyle: StyleA

  protected[this] def svgPath: String

  protected[this] def _btnClickAction: IScHdrAction

  protected class Backend($: BackendScope[Props, _]) {

    private def _onClick: Callback = {
      dispatchOnProxyScopeCB( $, _btnClickAction )
    }

    def render(p: Props): VdomElement = {
      val fgColorOpt = for {
        fgColorData <- p()
      } yield {
        fgColorData.code
      }

      fgColorOpt
        .whenDefinedEl { fgColorHex =>
          html.div(
            cssStyle,
            htmlAttrs.onClick --> _onClick,

            <.svg(
              ^.xmlns := SvgConst.SVG_NAMESPACE_URI,
              HdrBtn.WIDTH,
              HdrBtn.HEIGHT,
              HdrBtn.VIEWPORT,

              <.path(
                HdrBtn.fillHex( fgColorHex ),
                ^.d := svgPath
              )
            )
          )
        }
    }

  }

  protected[this] def _compName: String

  val component = ScalaComponent.builder[Props]( _compName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(fgColorDataOptProxy: Props) = component.withKey(_compName)( fgColorDataOptProxy )
  lazy val applyReusable: ReactConnectProps[Props_t] = apply

}
