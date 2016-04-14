package io.suggest.sc.sjs.vm.mapbox

import io.suggest.common.maps.mapbox.MapBoxConstants
import io.suggest.common.maps.mapbox.MapBoxConstants.Layers.MyGeoLoc
import io.suggest.sc.sjs.m.mgeo.MGeoLoc
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.mapbox.gl.event.EventData
import io.suggest.sjs.mapbox.gl.geojson.{GeoJsonSource, GeoJsonSourceDescr}
import io.suggest.sjs.mapbox.gl.layer.circle.CirclePaintProps
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
      // TODO Воткнуть сюда дефолтовый zoom на основе ugl.accuracyM
    }

    // Инициализировать карту.
    new GlMap(opts)
  }

}


case class GlMapVm(glmap: GlMap) {

  /**
    * Выставить в карту новую геолокацию
    *
    * @param mGeoLoc Данные геолокации во внутреннем формате.
    */
  def setUserGeoLoc(mGeoLoc: MGeoLoc): Unit = {
    // Собрать слой (если ещё не собран), где будет отображена текущая геолокация.
    val srcId = MapBoxConstants.Layers.MY_GEOLOC_LAYER_ID
    val myPoint = mGeoLoc.point.toGjPoint

    glmap.getSource(srcId).fold[Unit] {
      // Пока нет ни слоя, ни source. Создать их
      // Сборка source
      val src = new GeoJsonSource(
        GeoJsonSourceDescr(
          data = myPoint
        )
      )
      glmap.addSource(srcId, src)

      // Сборка layer'а.
      val lay = Layer.empty
      lay.id = srcId
      lay.`type` = LayerTypes.CIRCLE
      lay.source = srcId
      lay.paint = {
        val paint = CirclePaintProps.empty
        paint.circleRadius = MyGeoLoc.INNER_RADIUS_PX
        paint.circleColor  = MyGeoLoc.INNER_COLOR
        paint
      }
      glmap.addLayer(lay)

    } { srcObj =>
      val src = srcObj.asInstanceOf[GeoJsonSource]
      src.setData(myPoint)
    }
  }

  def on[U](eventType: String)(f: EventData => U): Unit = {
    glmap.on(eventType, f)
  }

}
