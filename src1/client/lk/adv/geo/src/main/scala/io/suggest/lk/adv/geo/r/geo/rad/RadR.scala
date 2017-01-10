package io.suggest.lk.adv.geo.r.geo.rad

import diode.react.ModelProxy
import io.suggest.lk.adv.geo.a.{RadCenterDragEnd, RadCenterDragStart, RadCenterDragging}
import io.suggest.lk.adv.geo.m.MRad
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.marker.Marker
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import react.leaflet.circle.{CirclePropsR, CircleR}
import react.leaflet.layer.LayerGroupR
import react.leaflet.marker.{MarkerPropsR, MarkerR}

import scala.scalajs.js
import scala.scalajs.js.{Function1, UndefOr}

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

  protected class Backend($: BackendScope[Props, _]) {

    /** Событие начала перетаскивания маркера центра круга. */
    def centerDragStart(e: Event): Callback = {
      $.props >>= { p =>
        p.dispatchCB( RadCenterDragStart )
      }
    }

    /** События таскания центра круга за маркер центра. */
    def centerDragging(e: Event): Callback = {
      val latLng = e.target
        .asInstanceOf[Marker]
        .getLatLng()
      val mgp = LkAdvGeoFormUtil.latLng2geoPoint( latLng )
      $.props >>= { p =>
        p.dispatchCB( RadCenterDragging(mgp) )
      }
    }

    /** Событие завершение перетаскивания круга за маркер центра. */
    def centerDragEnd(e: DragEndEvent): Callback = {
      val latLng = e.target
        .asInstanceOf[Marker]
        .getLatLng()
      val mgp = LkAdvGeoFormUtil.latLng2geoPoint( latLng )
      $.props >>= { p =>
        p.dispatchCB( RadCenterDragEnd(mgp) )
      }
    }


    def render(p: Props): ReactElement = {
      val cOpt = for ( v <- p() ) yield {
        // Рендер группы слоёв одной пачкой, чтобы можно было всё скопом вернуть наверх.
        LayerGroupR()(
          // Основной круг для описания слоя:
          CircleR(
            new CirclePropsR {
              override val center  = LkAdvGeoFormUtil.geoPoint2LatLng( v.circle.center )
              override val radius  = v.circle.radiusM
              override val color   = Const.FILL_COLOR

              // Прозрачность меняется на время перетаскивания.
              override val fillOpacity = {
                if (v.state.centerDragging)
                  Const.DRAG_OPACITY
                else
                  Const.OPACITY0
              }

              // Прозрачность абриса зависит от текущей деятельности юзера.
              override val opacity = {
                if (v.state.radiusDragging)
                  Const.RESIZE_PATH_OPACITY
                else if (v.state.centerDragging)
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
              override val position   = LkAdvGeoFormUtil.geoPoint2LatLng( v.circle.center )
              override val draggable  = true
              override val icon       = _pinIcon

              // Привязка событий:
              override val onDragStart: js.UndefOr[js.Function1[Event,_]] = js.defined {
                centerDragStart _
              }
              override val onDrag: UndefOr[Function1[Event, _]] = js.defined {
                centerDragging _
              }
              override val onDragEnd: UndefOr[Function1[DragEndEvent, _]] = js.defined {
                centerDragEnd _
              }
            }
          )(),

          // Маркер радиуса круга. Сделан в виде circle-marker'а.
          MarkerR(
            new MarkerPropsR {
              override val position  = LkAdvGeoFormUtil.geoPoint2LatLng( v.state.radiusMarkerCoords )
              override val draggable = true
            }
          )()

        )
      }
      cOpt.orNull
    }

  }


  val component = ReactComponentB[Props]("Rad")
    .stateless
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
