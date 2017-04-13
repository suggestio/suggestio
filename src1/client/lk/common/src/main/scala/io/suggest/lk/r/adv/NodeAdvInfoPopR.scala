package io.suggest.lk.r.adv

import diode.react.ModelProxy
import io.suggest.lk.m.NodeInfoPopupClose
import io.suggest.lk.pop.PopupR
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import PopupR.PopupPropsValFastEq
import io.suggest.sjs.common.controller.bxslider.BxSliderCtl
import io.suggest.sjs.common.controller.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 13:17
  * Description: React-компонент для рендера ответа сервера на тему инфы по размещению на узле.
  */
object NodeAdvInfoPopR {

  type Props = ModelProxy[Option[String]]


  class Backend($: BackendScope[Props, Unit]) {

    private def closeClick: Callback = {
      dispatchOnProxyScopeCB( $, NodeInfoPopupClose )
    }

    def render(proxy: Props): ReactElement = {
      val ihOpt = proxy()
      for (innerHtml <- ihOpt) yield {
        proxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some( closeClick ),
            topPc     = 10
          )
        } { popPropsProxy =>
          // Костыль: didMount до фактического рендера. Поэтому тут отложенный запуск bxSlider
          // TODO Надо попап переверстать на react.js и использовать react-image-gallery.
          _componentDidMount()

          PopupR(popPropsProxy)(

            <.div(
              ^.dangerouslySetInnerHtml( innerHtml )
            ): ReactElement

          )
        }
      }
    }

    private def _componentDidMount(): Unit = {
      // Исполняем в фоне с задержкой, т.к. иногда оно почему-то не срабатывает.
      DomQuick.setTimeout(50) {
        BxSliderCtl.initAll
      }
    }

  }


  val component = ReactComponentB[Props]("NodeAdvInfoPop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(innerHtmlOptProxy: Props) = component( innerHtmlOptProxy )

}
