package io.suggest.maps.r

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.color.MColorData
import io.suggest.common.coll.Lists
import io.suggest.common.html.HtmlConstants
import io.suggest.geo._
import io.suggest.log.Log
import io.suggest.maps.OpenMapRcvr
import io.suggest.proto.http.client.HttpClient
import io.suggest.maps.m.MonkeyNodeId.forJsObject
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.event.{LeafletEventHandlerFnMap, LeafletMouseEventHandlerFn, MouseEvent}
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.icon.IconOptions
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent, MarkerOptions}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.scalajs.react.{BackendScope, Callback, PropsChildren, ScalaComponent}
import io.suggest.math.SimpleArithmetics._
import io.suggest.msg.ErrorMsgs
import org.js.react.leaflet.{Circle, CircleProps, LayerGroup, MarkerClusterGroup, MarkerClusterGroupProps, Polygon, PolygonProps}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 12:50
  * Description: Поддержка простенького react-компонента для кластера ресиверов на карте.
  */
object RcvrMarkersR extends Log {

  type Props_t = Pot[MGeoNodesResp]
  type Props = ModelProxy[Props_t]

  def FILL_OPACITY    = 0.15
  def STROKE_OPACITY  = 0.7
  def STROKE_WEIGHT   = 1

  protected class Backend($: BackendScope[Props, Unit]) {

    private val _onMarkerClicked = ReactCommonUtil.cbFun1ToJsCb { e: MarkerEvent =>
      val marker = e.layer
      val nodeId = marker.nodeId.get
      val latLng = marker.getLatLng()
      val gp = MapsUtil.latLng2geoPoint(latLng)
      _clickEvent(nodeId, gp)
    }

    private def _clickEvent(nodeId: String, gp: MGeoPoint): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCB($, OpenMapRcvr(nodeId, gp))
    }

    private def _toColorOpt(mcdOpt: Option[MColorData]): js.UndefOr[String] = {
      val colorOpt = for (mcd <- mcdOpt) yield {
        HtmlConstants.DIEZ + mcd.code
      }
      colorOpt.toUndef
    }


    private val _mcgEventTypes = new LeafletEventHandlerFnMap {
      @JSName( "click" )
      override val clusterMarkerClick = _onMarkerClicked
    }

    // Внутренний класс вместо кортежа, т.к. у scalac крышу срывает от кортежей с RComp_t внутри.
    private case class ResTuple( latLng: LatLng, jsComp: List[VdomElement] )


    /** Рендер всей гео.карты. */
    def render(rcvrsGeoPotProxy: Props, children: PropsChildren): VdomElement = {
      rcvrsGeoPotProxy().toOption.whenDefinedEl { mRcvrsGeo =>
        // Чтобы при проблемах не заваливать логи сотнями ошибок, нужен счётчик-заглушка,
        // который говорит: сколько ещё максимум ошибок/проблем можно залоггировать:
        var _logErrorsMax = 2

        // Собираем сложный итератор, который на выходе в элементах выдаёт два аккамулятора: маркеры и шейпы.
        val markersAndShapeComponents = (for {
          mnode <- mRcvrsGeo.nodes.iterator

          // Собираем параметры отображения маркеров текущего узла над его шейпами.
          nodeId = mnode.props.nodeId

          _onShapeClickEventHandlers = { (center: MGeoPoint) =>
            new LeafletEventHandlerFnMap {
              override val click = ReactCommonUtil.cbFun1ToJsCb { _: MouseEvent =>
                _clickEvent(nodeId.get, center)
              }: LeafletMouseEventHandlerFn
            }
          }

          // Маркер можно рендерить, когда есть иконка узла. Иначе нужен CircleMarker вместо маркера.
          markerOptions = new MarkerOptions {
            override val draggable = false
            override val clickable = true //nodeIdOpt.isDefined
            // Иконка обязательна, иначе отображать будет нечего. Собрать иконку из присланных сервером данных.
            override val icon = js.defined {
              val liconOpt = for {
                iconInfo <- mnode.props.logoOpt
                wh       <- iconInfo.whPx
              } yield {
                val o = IconOptions.empty
                // Для cordova требуются абсолютные ссылки на картинки, иначе она подставит file:// в протокол.
                o.iconUrl = HttpClient.mkAbsUrlIfPreferred( iconInfo.url )
                // Описываем размеры иконки по данным сервера.
                // Следует ограничить ширину и высоту
                o.iconSize = {
                  val wh2 = wh.copy(
                    width = Math.min( wh.width, 80 ),
                    height = Math.min( wh.height, 50 ),
                  )
                  MapsUtil.size2d2LPoint( wh2 )
                }
                // Для иконки -- якорь прямо в середине.
                o.iconAnchor = MapsUtil.size2d2LPoint( wh / 2 )
                Leaflet.icon(o)
              }
              liconOpt getOrElse MapIcons.pinMarkerIcon()
            }
            override val title = mnode.props.name.toUndef
          }


          // Подготовить цвета для гео-шейпов текущего узла.
          c = mnode.props.colors
          _fillColor = _toColorOpt( c.bg )
          _strokeColor = _toColorOpt( c.fg )

          // Пройтись по шейпам узла
          gs <- mnode.shapes

          // Если возможно, отрендерить шейп узла в react-компонент, вычислить центр шейпа, и туда маркер засандалить.
          resTuple: ResTuple = {
            gs match {
              // Просто точка.
              case pointGs: PointGs =>
                val ll = MapsUtil.geoPoint2LatLng( pointGs.coord )
                ResTuple( ll, Nil )

              // Рендерим react-круг.
              case circleGs: CircleGs =>
                val _center = circleGs.center
                val _centerLatLng = MapsUtil.geoPoint2LatLng( _center )
                val opts = new CircleProps {
                  override val radius       = circleGs.radiusM
                  override val center       = _centerLatLng
                  override val fillColor    = _fillColor
                  override val fillOpacity  = FILL_OPACITY
                  override val stroke       = _strokeColor.nonEmpty
                  override val weight       = STROKE_WEIGHT
                  override val color        = _strokeColor
                  override val opacity      = STROKE_OPACITY
                  override val eventHandlers = _onShapeClickEventHandlers( _center )
                }
                val rc = Circle.component( opts )()
                ResTuple(_centerLatLng, rc :: Nil)

              // Рендерить полигон или мультиполигон.
              case lPolygon: ILPolygonGs =>
                val _positions = MapsUtil.lPolygon2leafletCoords( lPolygon )
                // Вычислить гео-центр этого полигона
                val _centerLL = MapsUtil.polyLatLngs2center( _positions )
                val _center = MapsUtil.latLng2geoPoint( _centerLL )
                val opts = new PolygonProps {
                  override val positions    = _positions
                  override val fillColor    = _fillColor
                  override val fillOpacity  = FILL_OPACITY
                  override val stroke       = _strokeColor.nonEmpty
                  override val weight       = STROKE_WEIGHT
                  override val color        = _strokeColor
                  override val opacity      = STROKE_OPACITY
                  override val eventHandlers = _onShapeClickEventHandlers( _center )
                }
                val rc = Polygon.component(opts)()
                ResTuple(_centerLL, rc :: Nil)

              // Неподдерживаемый шейп. Используем обходные пути для рендера, но не допускаем исключений.
              case other =>
                if (_logErrorsMax > 0) {
                  logger.warn( ErrorMsgs.GEO_JSON_GEOM_COORD_UNEXPECTED_ELEMENT, msg = (other.shapeType, _logErrorsMax) )
                  _logErrorsMax -= 1
                }
                val centerGp = other.centerPoint getOrElse other.firstPoint
                val centerLL = MapsUtil.geoPoint2LatLng( centerGp )
                ResTuple( centerLL, Nil )
            }
          }

        } yield {
          // Собрать маркер узла над шейпом:
          /*markerOptionsOpt.fold [(List[Marker], List[RComp_t])] {
            // Маркер выставить нет возможности. Поэтому нужно собрать CircleMarkerR вместо него.
            val cmProps = new CircleMarkerPropsR {
              override val center       = centerLatLng
              override val radius       = GeoConstants.CircleMarkers.RADIUS_PX
              override val fillColor    = _fillColor.orElse(_strokeColor)
              override val fillOpacity  = 1.0
              override val stroke       = false
              override val onClick: UndefOr[js.Function1[MouseEvent, Unit]] = {
                _onClickCbF( centerMgp )
              }
            }
            val cm = CircleMarkerR(cmProps)()
              .asInstanceOf[RComp_t]
            val shapeComponents2 = cm :: resTuple.jsComp
            (Nil, shapeComponents2)

          } { markerOptions =>*/
            // Есть данные для рендера маркера. Собираем маркер:
            val marker = Leaflet.marker( resTuple.latLng, markerOptions )
            marker.nodeId = nodeId.get
            val markers = marker :: Nil
            (markers, resTuple.jsComp)
          //}
        })
          .foldLeft( (List.empty[List[Marker]], List.empty[List[VdomElement]]) ) {
            case ((markersAcc, shapeComponentsAcc), (markers, shapeComponents)) =>
              (markers :: markersAcc,
                shapeComponents :: shapeComponentsAcc)
          }

        // Превратить итератор аккамуляторов в два стабильных аккамулятора.
        val (markers9, shapeComponents9) = markersAndShapeComponents

        // Вернуть итоговый react-компонент:
        LayerGroup()(

          // Полигоны, мультиполигоны, круги.
          ReactCommonUtil.maybeNode( shapeComponents9.nonEmpty ) {
            LayerGroup()(
              // Используем ускоренный flattenRev вместо штатного flatten, т.к. порядок нам не важен.
              Lists.flattenRev( shapeComponents9 ): _*
            )
          },

          // Точки-маркеры поверх вообще всех svg-шейпов
          Option.when[VdomElement]( markers9.nonEmpty ) {
            MarkerClusterGroup.component(
              new MarkerClusterGroupProps {
                override val markers      = markers9.iterator.flatten.toJSArray
                override val eventHandlers = _mcgEventTypes

                // По дефолту 80. Но как-то опаздывает оно с разделением мелких кластеров.
                override val maxClusterRadius = 60

                // На время ранних демонстраций: 15 - это уровень зданий ТЦ в центре СПб.
                // Потом, с ростом мелочи, надо будет увеличить до 16, 17 или вообще закомментить.
                // 2020-06-24 - Закомменчено, т.к. слишком близкие точки некликабельны на карте.
                //override val disableClusteringAtZoom = 15

                // Синий полигон поверх точек - не нужен.
                override val showCoverageOnHover = false
              }
            )
          }
            .whenDefinedEl,

          children
        )

      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

}
