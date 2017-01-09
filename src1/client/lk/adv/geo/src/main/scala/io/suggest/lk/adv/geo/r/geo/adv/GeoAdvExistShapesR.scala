package io.suggest.lk.adv.geo.r.geo.adv

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.adv.geo.AdvGeoConstants.CurrShapes.{OK_COLOR, OPACITY, REQ_COLOR}
import io.suggest.geo.MGeoPoint
import io.suggest.lk.adv.geo.a.OpenAdvGeoExistPopup
import io.suggest.lk.adv.geo.m.MGeoAdvExistGjFtProps.fromAny
import io.suggest.sjs.common.geo.json.{GjFeature, GjFeatureCollection, GjGeometry}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.event.{Events, LayerEvent}
import io.suggest.sjs.leaflet.map.{LatLng, Layer}
import io.suggest.sjs.leaflet.path.PathOptions
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import react.leaflet.gj.{GeoJsonPropsR, GeoJsonR}

import scala.scalajs.js
import scala.scalajs.js.{Function1, Function2, JSON, UndefOr}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 10:38
  * Description: React-компонент рендера на карте фигурок существующих гео-размещений карточки.
  */
object GeoAdvExistShapesR extends Log {

  type Props = ModelProxy[Pot[js.Array[GjFeature]]]

  protected class Backend($: BackendScope[Props, _]) {

    def existShapeClick(itemId: Double, at: MGeoPoint): Callback = {
      $.props >>= { props =>
        props.dispatchCB( OpenAdvGeoExistPopup(itemId, at) )
      }
    }

    def render(p: Props): ReactElement = {
      p().toOption.fold[ReactElement](null) { gjFeatures =>
        // Собрать GeoJSON-слой для кружочков, но рендером управлять через callback'и.
        GeoJsonR( new GeoJsonPropsR {

          override val data = GjFeatureCollection(gjFeatures)

          // Нужно рендерить круги при получение точек.
          override val pointToLayer: UndefOr[Function2[GjFeature, LatLng, Layer]] = js.defined { (gjFeature, latLng) =>
            Leaflet.circle(latLng, gjFeature.properties.get.radiusM)
          }

          // Стилизация перед рендером...
          override val style: UndefOr[Function1[GjFeature, PathOptions]] = js.defined { gjFeature =>
            // Десериализовать пропертисы.
            if (gjFeature.properties.isEmpty)
              LOG.warn( WarnMsgs.GJ_PROPS_EMPTY_OR_MISS, msg = JSON.stringify(gjFeature) )

            new PathOptions {
              override val clickable: UndefOr[Boolean]  = true
              override val stroke: UndefOr[Boolean]     = false
              override val fillColor: UndefOr[String]   = {
                if (gjFeature.properties.exists(_.hasApproved)) OK_COLOR else REQ_COLOR
              }
              override val fillOpacity: UndefOr[Double] = OPACITY
            }
          }

          // Повесить слушалки событий
          override val onEachFeature: UndefOr[Function2[GjFeature, Layer, Unit]] = js.defined { (gjFeature, layer) =>
            for {
              props <- gjFeature.properties
            } {

              val itemId = props.itemId
              val gp = GjGeometry.firstPoint(gjFeature.geometry)

              layer.on3( Events.CLICK, { e: LayerEvent =>
                existShapeClick(itemId, gp).runNow()
              })
            }
          }

        })()
      }
    }

  }

  val component = ReactComponentB[Props]("GeoAdvExistShapes")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(gjResp: Props) = component(gjResp)

}

