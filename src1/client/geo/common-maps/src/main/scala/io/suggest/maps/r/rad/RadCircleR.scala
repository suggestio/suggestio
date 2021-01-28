package io.suggest.maps.r.rad

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.maps.u.MapsUtil
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import io.suggest.maps.m.RadAreaClick
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.react.ReactDiodeUtil
import io.suggest.sjs.leaflet.event.{LeafletEventHandlerFnMap, MouseEvent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import org.js.react.leaflet.{Circle, CircleProps}

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
      (a.centerGeoPoint ===* b.centerGeoPoint) &&
        (a.radiusM ==* b.radiusM) &&
        (a.centerDragging ==* b.centerDragging) &&
        (a.radiusDragging ==* b.radiusDragging)
    }
  }


  type Props = ModelProxy[Option[PropsVal]]

  private object Const {

    final def OPACITY0              = 0.2
    final def DRAG_OPACITY          = OPACITY0 / 2

    final def PATH_OPACITY0         = 0.5
    final def DRAG_PATH_OPACITY     = 0.4
    final def RESIZE_PATH_OPACITY   = 0.9

    final val FILL_COLOR            = "red"

  }


  class Backend($: BackendScope[Props, Unit]) {

    private val _onClickCB = cbFun1ToJsCb { _: MouseEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, RadAreaClick )
    }

    private val _eventHandlers = new LeafletEventHandlerFnMap {
      override val click = _onClickCB
    }

    def render(propsProxy: Props): VdomElement = {
      propsProxy().whenDefinedEl { p =>
        Circle.component(
          new CircleProps {
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

            override val eventHandlers = _eventHandlers
            override val clickable = true
          }
        )()
      }
    }

  }


  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
