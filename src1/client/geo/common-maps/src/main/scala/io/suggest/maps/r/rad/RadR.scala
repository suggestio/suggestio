package io.suggest.maps.r.rad

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.maps.m._
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.react.ReactCommonUtil.cbFun1TojsCallback
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import io.suggest.sjs.leaflet.map.LatLng
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.circle.{CirclePropsR, CircleR}
import react.leaflet.layer.LayerGroupR
import react.leaflet.marker.{MarkerPropsR, MarkerR}
import react.leaflet.popup.PopupR

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

  private val _radiusIcon = MapIcons.radiusMarkerIcon()

  case class State(
                    centerLatLngOptConn : ReactConnectProxy[Option[LatLng]],
                    centerPopupConn     : ReactConnectProxy[Option[Boolean]],
                    radEnabledPropsConn : ReactConnectProxy[RadEnabledR.PropsVal]
                  )



  protected class Backend($: BackendScope[Props, State]) extends RadBackendHelper($) {

    // Стабильные инстансы функций, чтобы точно избежать их перебиндинга при каждом рендере...
    private val _radAreaClickF    = cbFun1TojsCallback { _: Event => _dispatch( RadAreaClick ) }

    // Стабильные инстансы callback-функций для маркера радиуса.
    private val _radiusDragStartF = cbFun1TojsCallback { _: Event => _dispatch(RadiusDragStart) }
    private val _radiusDraggingF  = cbFun1TojsCallback( _markerDragging(_: Event, RadiusDragging) )
    private val _radiusDragEndF   = cbFun1TojsCallback( _markerDragEnd(_: DragEndEvent, RadiusDragEnd) )


    def render(p: Props, s: State): ReactElement = {
      for ( v <- p() ) yield {
        s.centerLatLngOptConn { cllOptProxy =>
          for (centerLatLng <- cllOptProxy()) yield {

            // Рендер группы слоёв одной пачкой, чтобы можно было всё скопом вернуть наверх.
            LayerGroupR()(

              // Слой с кругом и маркерами управления оными.
              if (v.enabled) {
                LayerGroupR()(
                  // Основной круг для описания слоя:
                  CircleR(
                    new CirclePropsR {
                      override val center = centerLatLng
                      // Таскаемый центр хранится в состоянии отдельно от основного, т.к. нужно для кое-какие рассчётов апосля.
                      override val radius = v.circle.radiusM
                      override val color  = Const.FILL_COLOR

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
                      override val onClick = _radAreaClickF
                      override val clickable = true
                    }
                  )(),

                  // Маркер центра круга.
                  // TODO Скрывать маркер центра, если расстояние в пикселях до радиуса < 5
                  s.centerLatLngOptConn { DraggablePinMarkerR.apply },

                  // Маркер радиуса круга. Сделан в виде circle-marker'а.
                  // Почему-то реализация этого дела через fold()() вызывает NPE.
                  if (v.state.centerDragging.isEmpty) {
                    MarkerR(
                      new MarkerPropsR {
                        override val position    = MapsUtil.geoPoint2LatLng {
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
              } else {
                null
              },

              // Попап управления центром.
              s.centerPopupConn { popupEnabledOpt =>
                for (isEnabled <- popupEnabledOpt() if isEnabled) yield {
                  PopupR(
                    position = centerLatLng
                  )(
                    <.div(
                      s.radEnabledPropsConn( RadEnabledR.apply )
                    )
                  )
                }
              }

            )
          }
        }

      }
    }

  }


  val component = ReactComponentB[Props]("Rad")
    .initialState_P { mradOptProxy =>
      State(
        centerPopupConn = mradOptProxy.connect { mradOpt =>
          mradOpt.map(_.centerPopup)
        },
        radEnabledPropsConn = RadEnabledR.radEnabledPropsConn(mradOptProxy, renderHintAsText = true),
        centerLatLngOptConn  = mradOptProxy.connect { mradOpt =>
          for (mrad <- mradOpt) yield {
            MapsUtil.geoPoint2LatLng {
              mrad.state.centerDragging
                .getOrElse( mrad.circle.center )
            }
          }
        }
      )
    }
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
