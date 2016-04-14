package io.suggest.sc.sjs.c.mapbox

import io.suggest.common.maps.mapbox.MapBoxConstants
import io.suggest.common.maps.mapbox.MapBoxConstants.Layers.MyGeoLoc
import io.suggest.sc.sjs.m.mgeo.IGeoLocSignal
import io.suggest.sc.sjs.m.mmap.EnsureMap
import io.suggest.sc.sjs.vm.search.tabs.geo.SGeoRoot
import io.suggest.sjs.common.geo.json.{GjGeometry, GjTypes}
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.mapbox.gl.geojson.{GeoJsonSource, GeoJsonSourceDescr}
import io.suggest.sjs.mapbox.gl.layer.circle.CirclePaintProps
import io.suggest.sjs.mapbox.gl.layer.{Layer, LayerTypes}
import io.suggest.sjs.mapbox.gl.map.{GlMap, GlMapOptions}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 22:12
  * Description: FSM-аддон для состояния готовности js к работе.
  */

// TODO Распилить состояние на несколько: js ужеготов, карта инициализируется, карта инициализирована.

trait MbgljsReady extends MbFsmStub {

  /** Трейт для сборки состояния готовности mapbox-gl.js к работе на странице. */
  trait MbgljsReadyStateT extends FsmState {

    override def afterBecome(): Unit = {
      super.afterBecome()
      // если в состоянии есть несвоевременные сообщения, то отработать их.
      val sd0 = _stateData
      val earlyMsgs = sd0.early
      if (earlyMsgs.nonEmpty) {
        _stateData = sd0.copy(
          early = Nil
        )
        for (em <- earlyMsgs.reverseIterator) {
          _sendEvent(em)
        }
      }
    }

    override def receiverPart: Receive = {
      // ScFsm намекает о необходимости убедиться, что карта готова к работе.
      case em: EnsureMap =>
        _ensureMap(em)

      // Обновилась текущая геолокация юзера, необходимо отразить это на карте.
      case gs: IGeoLocSignal =>
        _geoLocReceived(gs)
    }


    /** Инициализация карты в текущей выдаче, если необходимо. */
    def _ensureMap(em: EnsureMap): Unit = {
      val sd0 = _stateData
      if (sd0.glmap.isEmpty) {
        for (cont <- SGeoRoot.find()) {
          // Пока div контейнера категорий содержит какой-то мусор внутри, надо его очищать перед использованием.
          cont.clear()
          // Собираем опции для работы.
          val opts = GlMapOptions.empty
          opts.container  = cont._underlying
          opts.style      = "mapbox://styles/konstantin2/cimolhvu600f4cunjtnyk1hs6"
          // TODO Воткнуть сюда дефолтовый lat lng и zoom.

          // Инициализировать карту.
          val map0 = new GlMap(opts)

          // Сохранить карту в состояние FSM.
          _stateData = sd0.copy(
            glmap   = Some(map0)
          )
        }

      } else {
        warn(WarnMsgs.MAPBOXLG_ALREADY_INIT)
      }
    }


    def _geoLocReceived(gs: IGeoLocSignal): Unit = {
      // Собрать слой (если ещё не собран), где будет отображена текущая геолокация.

      val sd0 = _stateData
      val glmap = sd0.glmap.get

      val srcId = MapBoxConstants.Layers.MY_GEOLOC_LAYER_ID
      val coords = gs.data
      val myPoint = coords.point.toGjPoint

      Option( glmap.getSource(srcId) ).fold[Unit] {
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

  }

}
