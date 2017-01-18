package io.suggest.sc.sjs.vm.mapbox

import io.suggest.common.maps.mapbox.MapBoxConstants.{TargetPoint, UserGeoLoc, ILayerConst}
import io.suggest.geo.MGeoPoint
import io.suggest.sc.map.ScMapConstants
import io.suggest.sc.map.ScMapConstants.Nodes.Sources
import io.suggest.sc.sjs.m.mgeo.MGeoPointExt
import io.suggest.sjs.common.model.loc.{MGeoLoc, MGeoPointJs}
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.mapbox.gl.{Color_t, Filter_t}
import io.suggest.sjs.mapbox.gl.event.EventData
import io.suggest.sjs.mapbox.gl.geojson.{GeoJsonSource, GeoJsonSourceDescr}
import io.suggest.sjs.mapbox.gl.layer.circle.CirclePaintProps
import io.suggest.sjs.mapbox.gl.layer.symbol.SymbolLayoutProps
import io.suggest.sjs.mapbox.gl.layer.{Clusters, Filters, Layer, LayerTypes}
import io.suggest.sjs.mapbox.gl.ll.LngLat
import io.suggest.sjs.mapbox.gl.map.{GlMap, GlMapOptions}
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.UndefOr
/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 14:06
  * Description: VM'ка, дающая высокоуровневое API вокруг карты mapbox-gl.
  */
object GlMapVm {

  /** Сборка новой карты в указанном контейнере. */
  def createNew(container: IVm { type T <: Element },
                useLocation: Option[MGeoLoc]): GlMap = {
    // Собираем опции для работы.
    val opts = GlMapOptions.empty
    opts.container  = container._underlying
    opts.style      = "mapbox://styles/konstantin2/cimolhvu600f4cunjtnyk1hs6"

    // Выставить начальное состояние карты.
    for (ugl <- useLocation) {
      opts.center = MGeoPointJs.toArray(ugl.point)
      opts.zoom   = 13
      // TODO Воткнуть сюда дефолтовый zoom на основе ugl.accuracyM
    }

    // Инициализировать карту.
    new GlMap(opts)
  }

}

/** Враппер над картой, дающий доступ к высокоуровневому API. */
case class GlMapVm(glMap: GlMap) {

  // ! Инстанс может кешироваться какое-то время, поэтому в нём не должно быть val'ов кроме тех, что в конструкторе.

  private def _circlePaintProps(ilc: ILayerConst): CirclePaintProps = {
    _circlePaintProps(ilc.CENTER_RADIUS_PX, ilc.CENTER_COLOR)
  }
  private def _circlePaintProps(radius: Int, color: Color_t): CirclePaintProps = {
    new CirclePaintProps {
      override val `circle-radius`  : UndefOr[Int]      = radius
      override val `circle-color`   : UndefOr[Color_t]  = color
    }
  }

  /**
    * Выставить в карту новую геолокацию
    *
    * @param mGeoLoc Данные геолокации во внутреннем формате.
    */
  def setUserGeoLoc(mGeoLoc: MGeoLoc): this.type = {
    // Собрать слой (если ещё не собран), где будет отображена текущая геолокация.
    val srcId = UserGeoLoc.SRC_ID
    val myPoint = MGeoPointJs.toGjPoint(mGeoLoc.point)

    glMap.getSource(srcId).fold[Unit] {
      // Пока нет ни слоя, ни source. Создать их
      // Сборка source
      val src = GeoJsonSourceDescr.empty
      src.data = myPoint
      glMap.addSource(srcId, src)

      // Сборка layer'а.
      val lay = Layer.empty
      lay.id      = UserGeoLoc.LAYER_ID
      lay.`type`  = LayerTypes.CIRCLE
      lay.source  = srcId
      lay.paint   = _circlePaintProps(UserGeoLoc)
      glMap.addLayer(lay)

    } { srcObj =>
      val src = srcObj.asInstanceOf[GeoJsonSource]
      src.setData(myPoint)
    }
    this
  }

  def on[U](eventType: String)(f: EventData => U): this.type = {
    glMap.on(eventType, f)
    this
  }

  def off[U](eventType: String)(f: EventData => U): this.type = {
    glMap.off(eventType, f)
    this
  }

  /** Прочитать центр карты в точку. */
  def center = MGeoPointExt( glMap.getCenter() )

  /** Выставить центр карты из точки. */
  def center_=(mgp: MGeoPoint): Unit = {
    val arr = MGeoPointJs.toJsArray(mgp)
    val latLng = LngLat.convert(arr)
    glMap.setCenter( latLng )
  }

  /**
    * Выставить точку-прицел текущей позиции на карте.
    *
    * @param point Гео-точка текущей позициии.
    */
  def setCurrentPos(point: MGeoPoint): this.type = {
    // Слой и сорс маркера обычно не существует до первого драггинга. И создаются они одновременно.
    val srcId = TargetPoint.SRC_ID
    val gjPoint = MGeoPointJs.toGjPoint(point)

    glMap.getSource(srcId).fold [Unit] {
      // Не создано ещё пока слоя-цели для наведения.
      val src = GeoJsonSourceDescr.empty
      src.data = gjPoint
      glMap.addSource(srcId, src)

      // Сборка слоя с точкой прицеливания.
      val lay = Layer.empty
      lay.id      = TargetPoint.LAYER_ID
      lay.`type`  = LayerTypes.CIRCLE
      lay.source  = srcId
      lay.paint   = _circlePaintProps(TargetPoint)
      glMap.addLayer(lay)

    } { srcRaw =>
      // Уже есть цель для наведения. Обновить модель данных слоя.
      val src = srcRaw.asInstanceOf[GeoJsonSource]
      src.setData(gjPoint)
    }
    this
  }


  /** Инициализировать слой  */
  def initAllNodes(fromUrl: String): Unit = {
    val srcDesc = GeoJsonSourceDescr.empty
    srcDesc.data = fromUrl
    srcDesc.cluster = true
    // Карта в нашем случае обычно игнорит это значение, поэтому его оставляем на дефолте (maxzoom 20 - 1 = 19).
    //srcDesc.clusterMaxZoom = 14
    srcDesc.clusterRadius = 50
    val srcId = ScMapConstants.Nodes.ALL_NODES_SRC_ID
    glMap.addSource(srcId, srcDesc)

    val L = ScMapConstants.Nodes.Layers

    // Собрать слой просто точек. Он будет внизу.
    glMap.addLayer {
      val layP = Layer.empty
      layP.id = L.NON_CLUSTERED_LAYER_ID
      layP.`type` = LayerTypes.CIRCLE
      layP.source = srcId
      layP.paint = _circlePaintProps(Sources.MARKER_RADIUS_PX, Sources.FILL_COLOR)
      layP
    }
    // Собрать слой единичек, т.к. кластеризация пашет криво как-то.
    glMap.addLayer {
      val lay1 = Layer.empty
      lay1.id = L.NON_CLUSTERED_LAYER_LABELS_ID
      lay1.`type` = LayerTypes.SYMBOL
      lay1.source = srcId
      lay1.layout = {
        val slp = SymbolLayoutProps.empty
        slp.textField = "1"
        slp
      }
      lay1
    }

    // Спека для слоёв. Скопирована из https://www.mapbox.com/mapbox-gl-js/example/cluster/
    val layers = Seq(
      40  -> "#f28cb1",
      10  -> "#f1f075",
      0   -> "#51bbd6"
    )

    // Имя поля с кол-вом кластеризованных точек.
    val pcFn = Clusters.POINT_COUNT

    // Проходим спецификацию слоёв, создавая различные слои.
    layers.iterator.zipWithIndex.foldLeft(Option.empty[Int]) {
      case (prevMinCountOpt, ((minCount, color), i)) =>
        glMap.addLayer {
          val layC = Layer.empty
          layC.id = L.clusterLayerId(i)
          layC.`type` = LayerTypes.CIRCLE
          layC.source = srcId
          layC.paint = _circlePaintProps(Sources.MARKER_RADIUS_PX, color)
          layC.filter = {
            val f0: Filter_t = js.Array(Filters.>=, pcFn, minCount)
            prevMinCountOpt.fold [Filter_t] {
              f0
            } { prevMinCount =>
              js.Array(Filters.all,
                f0,
                js.Array(Filters.<, pcFn, prevMinCount)
              )
            }
          }
          layC
        }
        Some(minCount)
    }

    // Собрать слой cо счетчиком кол-ва узлов.
    glMap.addLayer {
      val layU = Layer.empty
      layU.id     = L.COUNT_LABELS_LAYER_ID
      layU.`type` = LayerTypes.SYMBOL
      layU.source = srcId
      layU.layout = {
        val slp = SymbolLayoutProps.empty
        slp.textField = "{" + pcFn + "}"
        slp
      }
      layU
    }
  }

}
