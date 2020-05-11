package io.suggest.lk.adv.geo.r.pop

import diode.react.ModelProxy
import io.suggest.lk.adv.geo.m.MNodeInfoPopupS
import io.suggest.lk.r.adv.NodeAdvInfoPopR
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 16:35
  * Description: React-компонент инфы по узлу.
  */
object AdvGeoNodeInfoPopR {

  type Props = ModelProxy[Option[MNodeInfoPopupS]]


  class Backend($: BackendScope[Props, Unit]) {

    def render(props: Props): VdomElement = {
      props().whenDefinedEl { ps =>
        props.wrap(_ => ps.req.toOption)( NodeAdvInfoPopR.apply )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(infoPopupOptProxy: Props) = component( infoPopupOptProxy )

}
