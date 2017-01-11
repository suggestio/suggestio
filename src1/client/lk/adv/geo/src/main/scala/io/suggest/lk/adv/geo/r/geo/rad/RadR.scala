package io.suggest.lk.adv.geo.r.geo.rad

import diode.react.ModelProxy
import io.suggest.geo.MGeoPoint
import io.suggest.lk.adv.geo.a._
import io.suggest.lk.adv.geo.m.MRad
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.react.ReactCommonUtil.callBackFun2jsCallback
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.marker.Marker
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import react.leaflet.circle.{CirclePropsR, CircleR}
import react.leaflet.layer.LayerGroupR
import react.leaflet.marker.{MarkerPropsR, MarkerR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.01.17 21:55
  * Description: React-компонент для центра круга, описывающего радиус размещения.
  */
object RadR {

  private object Const {

    val OPACITY0              = 0.2
    val DRAG_OPACITY          = OPACITY0 / 2

    val PATH_OPACITY0         = 0.5
    val DRAG_PATH_OPACITY     = 0.4
    val RESIZE_PATH_OPACITY   = 0.9

    val FILL_COLOR            = "red"

  }

  type Props = ModelProxy[Option[MRad]]

  private val _pinIcon = LkAdvGeoFormUtil._pinMarkerIcon()
  private val _radiusIcon = LkAdvGeoFormUtil.radiusMarkerIcon()

  protected class Backend($: BackendScope[Props, _]) {

    /** Событие начала перетаскивания маркера. */
    private def _dragStart(msg: IAdvGeoFormAction): Callback = {
      $.props >>= { p =>
        p.dispatchCB( msg )
      }
    }

    /** События таскания какого-то маркера. */
    private def _dragging(e: Event, msg: MGeoPoint => IAdvGeoFormAction): Callback = {
      val latLng = e.target
        .asInstanceOf[Marker]
        .getLatLng()
      val mgp = LkAdvGeoFormUtil.latLng2geoPoint( latLng )
      $.props >>= { p =>
        p.dispatchCB( msg(mgp) )
      }
    }

    /** Событие завершения перетаскивания маркера. */
    private def _dragEnd(e: DragEndEvent, msg: MGeoPoint => IAdvGeoFormAction): Callback = {
      _dragging(e, msg)
    }

    // Стабильные инстансы функций, чтобы точно избежать их перебиндинга при каждом рендере...
    // Функции-коллбеки для маркера центра круга.
    private val _centerDragStartF = callBackFun2jsCallback { _: Event => _dragStart(RadCenterDragStart) }
    private val _centerDraggingF  = callBackFun2jsCallback( _dragging(_: Event, RadCenterDragging) )
    private val _centerDragEndF   = callBackFun2jsCallback( _dragEnd(_: DragEndEvent, RadCenterDragEnd) )

    // Стабильные инстансы callback-функций для маркера радиуса.
    private val _radiusDragStartF = callBackFun2jsCallback { _: Event => _dragStart(RadiusDragStart) }
    private val _radiusDraggingF  = callBackFun2jsCallback( _dragging(_: Event, RadiusDragging) )
    private val _radiusDragEndF   = callBackFun2jsCallback( _dragEnd(_: DragEndEvent, RadiusDragEnd) )


    def render(p: Props): ReactElement = {
      for ( v <- p() ) yield {

        val centerLatLng = LkAdvGeoFormUtil.geoPoint2LatLng {
          v.state.centerDragging
            .getOrElse( v.circle.center )
        }

        // Рендер группы слоёв одной пачкой, чтобы можно было всё скопом вернуть наверх.
        LayerGroupR()(
          // Основной круг для описания слоя:
          CircleR(
            new CirclePropsR {
              override val center  = centerLatLng
                // Таскаемый центр хранится в состоянии отдельно от основного, т.к. нужно для кое-какие рассчётов апосля.
              override val radius  = v.circle.radiusM
              override val color   = Const.FILL_COLOR

              // Прозрачность меняется на время перетаскивания.
              override val fillOpacity = {
                if (v.state.centerDragging.nonEmpty)
                  Const.DRAG_OPACITY
                else
                  Const.OPACITY0
              }

              // Прозрачность абриса зависит от текущей деятельности юзера.
              override val opacity = {
                if (v.state.radiusDragging)
                  Const.RESIZE_PATH_OPACITY
                else if (v.state.centerDragging.nonEmpty)
                  Const.DRAG_PATH_OPACITY
                else
                  Const.PATH_OPACITY0
              }
            }
          )(),

          // Маркер центра круга.
          // TODO Скрывать маркер центра, если расстояние в пикселях до радиуса < 5
          MarkerR(
            new MarkerPropsR {
              // Параметры рендера:
              override val position    = centerLatLng
              override val draggable   = true
              override val icon        = _pinIcon

              // Привязка событий:
              override val onDragStart = _centerDragStartF
              override val onDrag      = _centerDraggingF
              override val onDragEnd   = _centerDragEndF
            }
          )(),

          // Маркер радиуса круга. Сделан в виде circle-marker'а.
          if (v.state.centerDragging.isEmpty) {
            MarkerR(
              new MarkerPropsR {
                override val position    = LkAdvGeoFormUtil.geoPoint2LatLng {
                  v.state.radiusMarkerCoords
                }
                override val draggable   = true
                override val icon        = _radiusIcon

                // Привязка событий:
                override val onDragStart = _radiusDragStartF
                override val onDrag      = _radiusDraggingF
                override val onDragEnd   = _radiusDragEndF
              }
            )()
          } else {
            null
          }

        )
      }
    }

  }


  val component = ReactComponentB[Props]("Rad")
    .stateless
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
