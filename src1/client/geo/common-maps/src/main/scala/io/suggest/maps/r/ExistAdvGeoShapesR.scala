package io.suggest.maps.r

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.adv.AdvConstants.CurrShapes
import io.suggest.geo.{GeoConstants, MGeoPoint, MGeoPointJs}
import io.suggest.maps.m.MGeoAdvExistGjFtProps.fromAny
import io.suggest.sjs.common.geo.json.{GjFeature, GjFeatureCollection, GjGeometry}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.event.{Events, LayerEvent}
import io.suggest.sjs.leaflet.map.{LatLng, Layer}
import io.suggest.sjs.leaflet.path.PathOptions
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.Implicits._
import react.leaflet.gj.{GeoJsonPropsR, GeoJsonR}
import io.suggest.sjs.leaflet.path.circle.{CircleMarkerOptions, CircleOptions}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits.vdomElOptionExt
import io.suggest.maps.m.OpenAdvGeoExistPopup
import japgolly.scalajs.react.vdom.VdomElement

import scala.scalajs.js
import scala.scalajs.js.{JSON, UndefOr}


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.17 18:36
  * Description: React-компонент для рендера шейпов текущих размещений на leaflet-карте.
  */
object ExistAdvGeoShapesR extends Log {

  type Props = ModelProxy[Pot[js.Array[GjFeature]]]


  protected class Backend($: BackendScope[Props, Unit]) {

    private def onShapeClick(itemId: Double, at: MGeoPoint): Callback = {
      dispatchOnProxyScopeCB( $, OpenAdvGeoExistPopup(itemId, at) )
    }

    def render(p: Props): VdomElement = {
      p().toOption.whenDefinedEl { gjFeatures =>
        lazy val _circleMarkerOptions = new CircleMarkerOptions {
          override val radius = GeoConstants.CircleMarkers.RADIUS_PX
        }

        // Собрать GeoJSON-слой для кружочков, но рендером управлять через callback'и.
        GeoJsonR( new GeoJsonPropsR {

          override val data = GjFeatureCollection(gjFeatures)

          // Нужно рендерить круги при получение точек.
          override val pointToLayer: UndefOr[js.Function2[GjFeature, LatLng, Layer]] = js.defined { (gjFeature, latLng) =>
            // Если радиус не задан, то рендерить circle-marker. Это используется в lk-adn-map, где есть размещение в точке.
            gjFeature.properties
              .flatMap(_.radiusM)
              .fold[Layer] {
                Leaflet.circleMarker(latLng, _circleMarkerOptions)
              } { radiusM =>
                val circleOpts = new CircleOptions {
                  override val radius = radiusM
                }
                Leaflet.circle(latLng, circleOpts)
              }
          }

          // Стилизация перед рендером...
          override val style: UndefOr[js.Function1[GjFeature, PathOptions]] = js.defined { gjFeature =>
            // Десериализовать пропертисы.
            if (gjFeature.properties.isEmpty)
              LOG.warn( WarnMsgs.GJ_PROPS_EMPTY_OR_MISS, msg = JSON.stringify(gjFeature) )

            new PathOptions {
              override val clickable: UndefOr[Boolean]  = true
              override val stroke: UndefOr[Boolean]     = false
              override val fillColor: UndefOr[String]   = {
                if (gjFeature.properties.exists(_.hasApproved))
                  CurrShapes.OK_COLOR
                else
                  CurrShapes.REQ_COLOR
              }
              override val fillOpacity: UndefOr[Double] = {
                CurrShapes.OPACITY
              }
            }
          }

          // Повесить слушалки событий
          override val onEachFeature: UndefOr[js.Function2[GjFeature, Layer, Unit]] = js.defined { (gjFeature, layer) =>
            for {
              props <- gjFeature.properties
            } {

              val itemId = props.itemId
              val gp = MGeoPointJs.fromGjArray(
                GjGeometry.firstPoint(gjFeature.geometry)
              )

              layer.on3( Events.CLICK, { _: LayerEvent =>
                onShapeClick(itemId, gp).runNow()
              })
            }
          }

        })
      }
    }

  }

  val component = ScalaComponent.builder[Props]("ExistAdvGeoShapes")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(gjResp: Props) = component(gjResp)

}
