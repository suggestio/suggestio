package io.suggest.lk.adv.geo.r.rcvr

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.geo._
import io.suggest.lk.adv.geo.m.ReqRcvrPopup
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.maps.m.MonkeyNodeId.forJsObject
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.event.MouseEvent
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.icon.IconOptions
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent, MarkerOptions}
import japgolly.scalajs.react.{BackendScope, Callback, JsComponentU, ReactComponentB, ReactElement, TopNode}
import react.leaflet.circle.{CirclePropsR, CircleR}
import react.leaflet.layer.LayerGroupR
import react.leaflet.marker.cluster.{MarkerClusterGroupPropsR, MarkerClusterGroupR}
import react.leaflet.poly.{PolygonPropsR, PolygonR}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 12:50
  * Description: Поддержка простенького react-компонента для кластера ресиверов на карте.
  */
object RcvrMarkersR {

  type Props = ModelProxy[Pot[MGeoNodesResp]]

  val FILL_OPACITY    = 0.15
  val STROKE_OPACITY  = 0.7
  val STROKE_WEIGHT   = 1

  protected class Backend($: BackendScope[Props, Unit]) {

    private def onMarkerClicked(e: MarkerEvent): Callback = {
      val marker = e.layer
      val nodeId = marker.nodeId.get
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


    private val _onMarkerClickedF = cbFun1ToJsCb( onMarkerClicked )

    // Алиас типа, чтобы короче писать. Тип везде скастован принудительно, т.к. у scalac крышу рвёт ниже по коду.
    private type RComp_t = JsComponentU[js.Object, js.Any, TopNode]
    // Внутренний класс вместо кортежа, т.к. у scalac крышу срывает от кортежей с RComp_t внутри.
    private case class ResTuple( latLng: LatLng, jsComp: RComp_t )


    /** Рендер всей гео.карты. */
    def render(rcvrsGeoPotProxy: Props): ReactElement = {
      for {
        mRcvrsGeo <- rcvrsGeoPotProxy().toOption
      } yield {

        // Собираем сложный итератор, который на выходе в элементах выдаёт два аккамулятора: маркеры и шейпы.
        val iter = for {
          mnode <- mRcvrsGeo.nodes.iterator

          // Собираем параметры отображения маркеров текущего узла над его шейпами.
          nodeId = mnode.props.nodeId

          _onClickCbF = { _mgp: MGeoPoint =>
            cbFun1ToJsCb { _: MouseEvent =>
              _clickEvent(nodeId, _mgp)
            }
          }

          // Маркер можно рендерить, когда есть иконка узла. Иначе нужен CircleMarker вместо маркера.
          markerOptions = new MarkerOptions {
            override val draggable = false
            override val clickable = true //nodeIdOpt.isDefined
            // Иконка обязательна, иначе отображать будет нечего. Собрать иконку из присланных сервером данных.
            override val icon = js.defined {
              mnode.props.icon.fold ( MapIcons.pinMarkerIcon() ) { iconInfo =>
                val o = IconOptions.empty
                o.iconUrl = iconInfo.url
                // Описываем размеры иконки по данным сервера.
                o.iconSize = MapsUtil.size2d2LPoint( iconInfo.wh )
                // Для иконки -- якорь прямо в середине.
                o.iconAnchor = MapsUtil.size2d2LPoint( iconInfo.wh / 2 )
                Leaflet.icon(o)
              }
            }
            override val title = JsOptionUtil.opt2undef( mnode.props.hint )
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
              // Рендерим react-круг.
              case circleGs: CircleGs =>
                val _center = circleGs.center
                val _centerLatLng = MapsUtil.geoPoint2LatLng( _center )
                val opts = new CirclePropsR {
                  override val radius       = circleGs.radiusM
                  override val center       = _centerLatLng
                  override val fillColor    = _fillColor
                  override val fillOpacity  = FILL_OPACITY
                  override val stroke       = _strokeColor.nonEmpty
                  override val weight       = STROKE_WEIGHT
                  override val color        = _strokeColor
                  override val opacity      = STROKE_OPACITY
                  override val onClick: UndefOr[js.Function1[MouseEvent, Unit]] = {
                    _onClickCbF( _center )
                  }
                }
                val rc = CircleR( opts )()
                  .asInstanceOf[RComp_t]
                ResTuple(_centerLatLng, rc)

              // Рендерить полигон или мультиполигон.
              case lPolygon: ILPolygonGs =>
                val _positions = MapsUtil.lPolygon2leafletCoords( lPolygon )
                // Вычислить гео-центр этого полигона
                val _centerLL = Leaflet.polygon( _positions )
                  .getBounds()
                  .getCenter()
                val _center = MapsUtil.latLng2geoPoint( _centerLL )
                val opts = new PolygonPropsR {
                  override val positions    = _positions
                  override val fillColor    = _fillColor
                  override val fillOpacity  = FILL_OPACITY
                  override val stroke       = _strokeColor.nonEmpty
                  override val weight       = STROKE_WEIGHT
                  override val color        = _strokeColor
                  override val opacity      = STROKE_OPACITY
                  override val onClick: UndefOr[js.Function1[MouseEvent, Unit]] = {
                    _onClickCbF( _center )
                  }
                }
                val rc = PolygonR(opts)()
                  .asInstanceOf[RComp_t]
                ResTuple(_centerLL, rc)

              // TODO Реализовать рендер остальных шейпов. Сейчас они пока не нужны.
              case other =>
                println("Unsupported geo-shape: " + other)
                ???
            }
          }

        } yield {
          val shapeComponents0 = resTuple.jsComp :: Nil
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
            val shapeComponents2 = cm :: shapeComponents0
            (Nil, shapeComponents2)

          } { markerOptions =>*/
            // Есть данные для рендера маркера. Собираем маркер:
            val marker = Leaflet.marker( resTuple.latLng, markerOptions )
            marker.nodeId = nodeId
            val markers = marker :: Nil
            (markers, shapeComponents0)
          //}
        }

        // Превратить итератор аккамуляторов в два стабильных аккамулятора.
        val (markers9, shapeComponents9) = iter
          .foldLeft( (List.empty[List[Marker]], List.empty[List[JsComponentU[_,_,_]]]) ) {
            case ((markersAcc, shapeComponentsAcc), (markers, shapeComponents)) =>
              (markers :: markersAcc,
                shapeComponents :: shapeComponentsAcc)
          }


        // Вернуть итоговый react-компонент:
        LayerGroupR()(

          // Полигоны, мультиполигоны, круги.
          for (_ <- shapeComponents9.headOption) yield {
            LayerGroupR()(
              shapeComponents9.flatten: _*
            )
          },

          // Точки-маркеры поверх вообще всех svg-шейпов
          for (_ <- markers9.headOption) yield {
            MarkerClusterGroupR(
              new MarkerClusterGroupPropsR {
                override val markers      = markers9.iterator.flatten.toJSArray
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
