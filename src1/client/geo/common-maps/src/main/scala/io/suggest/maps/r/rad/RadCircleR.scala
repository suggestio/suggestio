package io.suggest.maps.r.rad

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.maps.u.MapsUtil
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import react.leaflet.circle.{CirclePropsR, CircleR}
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.maps.m.RadAreaClick
import io.suggest.react.ReactCommonUtil.Implicits.vdomElOptionExt
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.sjs.leaflet.event.MouseEvent
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 10:47
  * Description: React-компонент круга покрытия в составе RadR.
  */
object RadCircleR {

  final case class PropsVal(
                             centerGeoPoint : MGeoPoint,
                             radiusM        : Double,
                             centerDragging : Boolean,
                             radiusDragging : Boolean
                           )

  /** Поддержка FastEq для this.PropsVal. */
  implicit object RadCirclePropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.centerGeoPoint eq b.centerGeoPoint) &&
        (a.radiusM == b.radiusM) &&
        (a.centerDragging == b.centerDragging) &&
        (a.radiusDragging == b.radiusDragging)
    }
  }


  type Props = ModelProxy[Option[PropsVal]]

  private object Const {

    val OPACITY0              = 0.2
    val DRAG_OPACITY          = OPACITY0 / 2

    val PATH_OPACITY0         = 0.5
    val DRAG_PATH_OPACITY     = 0.4
    val RESIZE_PATH_OPACITY   = 0.9

    val FILL_COLOR            = "red"

  }


  class Backend($: BackendScope[Props, Unit]) {

    private def _onClick: Callback = {
      dispatchOnProxyScopeCB($, RadAreaClick)
    }

    private val _onClickCB = cbFun1ToJsCb { _: MouseEvent => _onClick }

    def render(propsProxy: Props): VdomElement = {
      propsProxy().whenDefinedEl { p =>
        CircleR(
          new CirclePropsR {
            override val center = MapsUtil.geoPoint2LatLng( p.centerGeoPoint )
            // Таскаемый центр хранится в состоянии отдельно от основного, т.к. нужно для кое-какие рассчётов апосля.
            override val radius = p.radiusM
            override val color  = Const.FILL_COLOR

            // Прозрачность меняется на время перетаскивания.
            override val fillOpacity = {
              if (p.centerDragging)
                Const.DRAG_OPACITY
              else
                Const.OPACITY0
            }

            // Прозрачность абриса зависит от текущей деятельности юзера.
            override val opacity = {
              if (p.radiusDragging)
                Const.RESIZE_PATH_OPACITY
              else if (p.centerDragging)
                Const.DRAG_PATH_OPACITY
              else
                Const.PATH_OPACITY0
            }

            override val onClick   = _onClickCB
            override val clickable = true
          }
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("RadCircle")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValOptProxy: Props) = component(propsValOptProxy)

}
