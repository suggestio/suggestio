package io.suggest.lk.adv.geo.r.pop

import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.lk.adv.geo.m.MNodeInfoPopupS
import io.suggest.lk.r.adv.NodeAdvInfoPopR
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 16:35
  * Description: React-компонент инфы по узлу.
  */
object AdvGeoNodeInfoPopR {

  type Props = ModelProxy[Option[MNodeInfoPopupS]]


  class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props): ReactElement = {
      for (ps <- props()) yield {
        ps.req.renderReady { iHtml =>
          props.wrap(_ => Option(iHtml)) { iHtmlOptProxy =>
            NodeAdvInfoPopR(iHtmlOptProxy)
          }
        }.asInstanceOf[ReactElement]
      }
    }

  }


  val component = ReactComponentB[Props]("AdvGeoNodeInfoPop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(infoPopupOptProxy: Props) = component( infoPopupOptProxy )

}
