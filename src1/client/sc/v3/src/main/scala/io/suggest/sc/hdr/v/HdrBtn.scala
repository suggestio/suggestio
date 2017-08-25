package io.suggest.sc.hdr.v

import diode.react.ModelProxy
import io.suggest.model.n2.node.meta.colors.MColorData
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react.vdom.html_<^.{< => html, ^ => htmlAttrs}

import scalacss.ScalaCssReact._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.hdr.m.IScHdrAction
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

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

}

trait HdrBtn {

  type Props = ModelProxy[Option[MColorData]]

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

}
