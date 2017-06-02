package io.suggest.lk.adv.geo.r.rcvr

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.MGeoPoint
import io.suggest.lk.adv.geo.m.{MRcvrsGeo, ReqRcvrPopup}
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.sjs.leaflet.marker.MarkerEvent
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.react.ReactCommonUtil.Implicits._
import react.leaflet.marker.cluster.{MarkerClusterGroupPropsR, MarkerClusterGroupR}
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.maps.m.MonkeyNodeId
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.common.geo.json.GjTypes
import io.suggest.sjs.common.model.loc.MGeoPointJs
import io.suggest.sjs.leaflet.PolygonLatLngs_t
import io.suggest.sjs.leaflet.event.MouseEvent
import io.suggest.sjs.leaflet.geojson.GeoJson
import io.suggest.sjs.leaflet.map.LatLng
import react.leaflet.circle.{CirclePropsR, CircleR}
import react.leaflet.layer.LayerGroupR
import react.leaflet.poly.{PolygonPropsR, PolygonR}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 12:50
  * Description: Поддержка простенького react-компонента для кластера ресиверов на карте.
  */
object RcvrMarkersR {

  type Props = ModelProxy[Pot[MRcvrsGeo]]

  val FILL_OPACITY    = 0.15
  val STROKE_OPACITY  = 0.7
  val STROKE_WEIGHT   = 1

  protected class Backend($: BackendScope[Props, Unit]) {

    private def onMarkerClicked(e: MarkerEvent): Callback = {
      val marker = e.layer
      // TODO Почему-то тут не срабатывает implicit convertion... Приходится явно заворачивать
      val nodeId = MonkeyNodeId(marker).nodeId.get
      val latLng = marker.getLatLng()
      val gp = MapsUtil.latLng2geoPoint(latLng)
      _clickEvent(nodeId, gp)
    }

    private def _clickEvent(nodeId: String, gp: MGeoPoint): Callback = {
      val msg = ReqRcvrPopup(nodeId, gp)
      dispatchOnProxyScopeCB($, msg)
    }

    private def _toColorOpt(mcdOpt: Option[MColorData]): UndefOr[String] = {
      val colorOpt = for (mcd <- mcdOpt) yield {
        HtmlConstants.DIEZ + mcd.code
      }
      JsOptionUtil.opt2undef( colorOpt )
    }

    private def _clickSomewhereEvent(nodeId: String)(e: MouseEvent): Callback = {
      val gp = MapsUtil.latLng2geoPoint( e.latlng )
      val msg = ReqRcvrPopup(nodeId, gp)
      dispatchOnProxyScopeCB($, msg)
    }

    private val _onMarkerClickedF = cbFun1ToJsCb( onMarkerClicked )

    def render(rcvrsGeoPotProxy: Props): ReactElement = {
      for {
        mRcvrsGeo <- rcvrsGeoPotProxy().toOption
      } yield {
        val features = mRcvrsGeo.features

        // Собрать маркеры.
        val _markers = MapIcons.geoJsonToClusterMarkers( features )

        // Собрать круги.
        val _circles = {

          val iter2 = for {
            feature <- features.toIterator

            // Интересуют только круги. В GeoJSON они задаются точкой-центром + props.radiusM.
            if feature.geometry.`type` == GjTypes.Geom.POINT
            radiusM <- feature.props.circleRadiusM

          } yield {
            val c = feature.props.colors
            val nodeId = feature.props.nodeId
            val gjCoordArr = feature.geometry
              .coordinates
              .asInstanceOf[js.Array[Double]]
            val gp = MGeoPointJs.fromGjArray( gjCoordArr )
            val opts = new CirclePropsR {
              override val radius: Double = radiusM
              override val center: LatLng = {
                GeoJson.coordsToLatLng( gjCoordArr )
              }
              override val fillColor: UndefOr[String] = {
                _toColorOpt( c.bg )
              }
              override val fillOpacity: UndefOr[Double] = {
                FILL_OPACITY
              }
              override val stroke: UndefOr[Boolean] = {
                c.fg.nonEmpty
              }
              override val weight: UndefOr[Int] = {
                STROKE_WEIGHT
              }
              override val color: UndefOr[String] = {
                _toColorOpt( c.fg )
              }
              override val opacity: UndefOr[Double] = {
                STROKE_OPACITY
              }
              override val onClick: UndefOr[js.Function1[MouseEvent, Unit]] = {
                cbFun1ToJsCb { _: MouseEvent =>
                  _clickEvent(nodeId, gp)
                }
              }
            }
            CircleR( opts )()
          }

          iter2.toSeq
        }

        // Собираем полигоны.
        val _polygons = {
          // Типы геометрий, пригодный для рендера через PolygonR.
          val types = GjTypes.Geom.POLYGON ::
            GjTypes.Geom.MULTI_POLYGON ::
            Nil
          val iter2 = for {
            feature <- features.iterator
            if types.contains( feature.geometry.`type` )
          } yield {
            val c = feature.props.colors
            val nodeId = feature.props.nodeId

            val opts = new PolygonPropsR {
              override val positions: PolygonLatLngs_t = {
                GeoJson.coordsToLatLngs(
                  coords     = feature.geometry.coordinates,
                  levelsDeep = if (feature.geometry.`type`.contains( GjTypes.Geom._MULTI )) 2 else 1
                )
              }
              override val fillColor: UndefOr[String] = {
                _toColorOpt( c.bg )
              }
              override val fillOpacity: UndefOr[Double] = {
                FILL_OPACITY
              }
              override val stroke: UndefOr[Boolean] = {
                c.fg.nonEmpty
              }
              override val weight: UndefOr[Int] = {
                STROKE_WEIGHT
              }
              override val color: UndefOr[String] = {
                _toColorOpt( c.fg )
              }
              override val opacity: UndefOr[Double] = {
                STROKE_OPACITY
              }
              override val onClick: UndefOr[js.Function1[MouseEvent, Unit]] = {
                cbFun1ToJsCb { _clickSomewhereEvent(nodeId) }
              }
            }
            PolygonR(opts)()
          }

          iter2.toSeq
        }

        LayerGroupR()(

          // Полигоны, мультиполигоны.
          for (_ <- _polygons.headOption) yield {
            LayerGroupR()(
              _polygons: _*
            )
          },

          // Гео-шейпы узлов.
          // Круги -- выше всех остальных шейпов, но ниже маркеров.
          for (_ <- _circles.headOption) yield {
            LayerGroupR()(
              _circles: _*
            )
          },

          // Точки-маркеры поверх вообще всех кружочков
          for (_ <- _markers.headOption) yield {
            MarkerClusterGroupR(
              new MarkerClusterGroupPropsR {
                override val markers      = _markers
                override val markerClick  = _onMarkerClickedF
              }
            )()
          }

        )

      }
    }

  }


  val component = ReactComponentB[Props]("RcvrMarkers")
    .stateless
    .renderBackend[Backend]
    .build


  def apply(rcvrsGeoPotProxy: Props) = component(rcvrsGeoPotProxy)

}
