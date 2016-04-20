package io.suggest.sc.sjs.vm.mapbox

import io.suggest.common.maps.mapbox.MapBoxConstants.{TargetPoint, UserGeoLoc}
import io.suggest.sc.map.ScMapConstants
import io.suggest.sc.map.ScMapConstants.Nodes.Sources
import io.suggest.sc.sjs.m.mgeo.{MGeoLoc, MGeoPoint}
import io.suggest.sc.sjs.m.mmap.MapNodesSource
import io.suggest.sjs.common.geo.json.GjType
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.mapbox.gl.event.EventData
import io.suggest.sjs.mapbox.gl.geojson.GeoJsonSource
import io.suggest.sjs.mapbox.gl.layer.circle.CirclePaintProps
import io.suggest.sjs.mapbox.gl.layer.symbol.SymbolLayoutProps
import io.suggest.sjs.mapbox.gl.layer.{Layer, LayerTypes}
import io.suggest.sjs.mapbox.gl.map.{GlMap, GlMapOptions}
import org.scalajs.dom.Element
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
      opts.center = ugl.point.toArray
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

  /**
    * Выставить в карту новую геолокацию
    *
    * @param mGeoLoc Данные геолокации во внутреннем формате.
    */
  def setUserGeoLoc(mGeoLoc: MGeoLoc): this.type = {
    // Собрать слой (если ещё не собран), где будет отображена текущая геолокация.
    val srcId = UserGeoLoc.SRC_ID
    val myPoint = mGeoLoc.point.toGjPoint

    glMap.getSource(srcId).fold[Unit] {
      // Пока нет ни слоя, ни source. Создать их
      // Сборка source
      val src = GeoJsonSource.gjSrc(myPoint)
      glMap.addSource(srcId, src)

      // Сборка layer'а.
      val lay = Layer.empty
      lay.id      = UserGeoLoc.LAYER_ID
      lay.`type`  = LayerTypes.CIRCLE
      lay.source  = srcId
      lay.paint   = {
        val paint = CirclePaintProps.empty
        paint.circleRadius = UserGeoLoc.CENTER_RADIUS_PX
        paint.circleColor  = UserGeoLoc.CENTER_COLOR
        paint
      }
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

  def center = MGeoPoint( glMap.getCenter() )


  /**
    * Выставить точку-прицел текущей позиции на карте.
    *
    * @param point Гео-точка текущей позициии.
    */
  def setCurrentPos(point: MGeoPoint): this.type = {
    // Слой и сорс маркера обычно не существует до первого драггинга. И создаются они одновременно.
    val srcId = TargetPoint.SRC_ID
    val gjPoint = point.toGjPoint

    glMap.getSource(srcId).fold [Unit] {
      // Не создано ещё пока слоя-цели для наведения.
      val src = GeoJsonSource.gjSrc(gjPoint)
      glMap.addSource(srcId, src)

      // Сборка слоя с точкой прицеливания.
      val lay = Layer.empty
      lay.id      = TargetPoint.LAYER_ID
      lay.`type`  = LayerTypes.CIRCLE
      lay.source  = srcId
      lay.paint   = {
        val paint = CirclePaintProps.empty
        paint.circleRadius = TargetPoint.CENTER_RADIUS_PX
        paint.circleColor  = TargetPoint.CENTER_COLOR
        paint
      }
      glMap.addLayer(lay)

    } { srcRaw =>
      // Уже есть цель для наведения. Обновить модель данных слоя.
      val src = srcRaw.asInstanceOf[GeoJsonSource]
      src.setData(gjPoint)
    }
    this
  }


  /** API для упрощенного управления слоями и сорсами. */
  trait SrcApi {
    def name: String
    def init(data: GjType): Unit
    def uninit(): Unit
  }

  /** API для управления src и layer'ами в  */
  object PointsSrcApi extends SrcApi {
    override def name = Sources.POINTS

    override def init(data: GjType): Unit = {
      // Точки узлов: собираем сорс.
      val pointsSn = name
      glMap.getSource(pointsSn).fold [Unit] {
        val src = GeoJsonSource.gjSrc(data)
        glMap.addSource(pointsSn, src)

        // Собираем слой точек узлов.
        val lay = Layer.empty
        lay.id = pointsSn
        lay.`type` = LayerTypes.CIRCLE
        lay.source = pointsSn
        lay.paint = {
          val paint = CirclePaintProps.empty
          paint.circleRadius = Sources.POINT_RADIUS_PX
          paint.circleColor  = Sources.FILL_COLOR
          paint
        }
        glMap.addLayer(lay)
      } { srcRaw =>
        val src = srcRaw.asInstanceOf[GeoJsonSource]
        src.setData(data)
      }
    }

    override def uninit(): Unit = {
      val srcId = name
      for (_ <- glMap.getSource(srcId)) {
        glMap.removeLayer(srcId)
          .removeSource(srcId)
      }
    }
  }

  /** Управление src и layer'ами для кластеров узлов. */
  object ClustersSrcApi extends SrcApi {
    override def name = Sources.CLUSTERS

    override def init(data: GjType): Unit = {
      // Собрать модель
      val clustersSn = name
      glMap.getSource(clustersSn).fold [Unit] {
        val src = GeoJsonSource.gjSrc(data)
        glMap.addSource(clustersSn, src)

        // Собрать слой кружков
        val layC = Layer.empty
        layC.id = clustersSn
        layC.`type` = LayerTypes.CIRCLE
        layC.source = clustersSn
        layC.paint = {
          val paint = CirclePaintProps.empty
          paint.circleRadius = Sources.CLUSTER_RADIUS_PX
          paint.circleColor  = Sources.FILL_COLOR
          paint
        }
        glMap.addLayer(layC)

        // Собрать слой надписей на кружках
        val layL = Layer.empty
        layL.id = Sources.CLUSTER_LABELS
        layL.`type` = LayerTypes.SYMBOL
        layL.source = clustersSn
        layL.layout = {
          val lp = SymbolLayoutProps.empty
          lp.textField = "{" + ScMapConstants.Nodes.COUNT_FN + "}"
          lp
        }
        glMap.addLayer(layL)

      } { srcRaw =>
        val src = srcRaw.asInstanceOf[GeoJsonSource]
        src.setData(data)
      }
    }

    override def uninit(): Unit = {
      val srcId = Sources.CLUSTERS
      for (_ <- glMap.getSource(srcId)) {
        glMap.removeLayer(srcId)
          .removeLayer(Sources.CLUSTER_LABELS)
          .removeSource(srcId)
      }
    }
  }

  /** Все доступные API для карты узлов. */
  def nodesSrcApis = Seq[SrcApi](PointsSrcApi, ClustersSrcApi)

  /**
    * Обновить все слои карты узлов на основе нового объема данных.
    *
    * @param layers Новые данные по ВСЕМ слоям.
    *               Если искомого слоя там нет, то считаем, что пусто.
    * @return this.
    */
  def updateNodesMapLayers(layers: Seq[MapNodesSource]): Unit = {
    for {
      srcApi <- nodesSrcApis
    } {
      layers.find(_.name == srcApi.name).fold [Unit] {
        // Нет данных по слою, надо удалить его.
        srcApi.uninit()
      } { newData =>
        // Есть новые данные по слою.
        srcApi.init(newData.data)
      }
    }
  }

}
