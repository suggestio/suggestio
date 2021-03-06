package io.suggest.lk.r.img

import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.CropOpen
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCBf
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.18 11:52
  * Description: Кнопка "Кадрировать...".
  */
class CropBtnR {

  type Props_t = Option[CropOpen]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Клик по кнопке кадрирования картинки. */
    private def _onCropClick: Callback = {
      dispatchOnProxyScopeCBf($) { props: Props =>
        props.value.get
      }
    }

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { _ =>
        val B = Css.Buttons
          <.div(
            <.br,
            <.a(
              ^.`class` := Css.flat( B.BTN, B.BTN_W, B.MINOR, Css.Size.M ),
              ^.onClick --> _onCropClick,
              Messages( MsgCodes.`Crop` ), HtmlConstants.ELLIPSIS
            )
          )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

  def apply(resKeyOptProxy: Props) = component( resKeyOptProxy )

}
