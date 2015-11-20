package io.suggest.lk.adv.gtag.init

import io.suggest.lk.tags.edit.TagsEditInit
import io.suggest.maps.rad.init.RadMapInit
import io.suggest.maps.rad.vm.inputs.{InpZoom, InpLon, InpLat}
import io.suggest.sjs.common.controller.{IInitDummy, InitRouter}
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.sjs.leaflet.{Leaflet => L}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 10:24
  * Description: Инициализация формы размещения в геотегах.
  */
trait AdvGtagFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.AdvGtagForm) {
      Future {
        new AdvGtagFormInit()
          .init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Класс инициализации формы размещения в тегах. */
class AdvGtagFormInit
  extends IInitDummy
  with TagsEditInit
  with RadMapInit // Карта должа инициализироваться в конце.
  with SjsLogger
{

  val inpLatOpt   = InpLat.find()
  val inpLonOpt   = InpLon.find()
  val inpZoomOpt  = InpZoom.find()

  /** Провести собственную инициализацию карты на основе моделей инпутов map state. */
  override protected def _earlyInitMap(lmap: LMap): Unit = {
    val r = for {
      inpLat  <- inpLatOpt
      lat     <- inpLat.value
      inpLon  <- inpLonOpt
      lon     <- inpLon.value
      inpZoom <- inpZoomOpt
      zoom    <- inpZoom.value
    } yield {
      lmap.setView(
        center = L.latLng(lat, lng = lon),
        zoom   = zoom
      )
    }
    // Если с инициализацией не фартануло, то передать исполнение на super-инициализацию.
    if (r.isEmpty) {
      warn( WarnMsgs.RAD_MAP_NO_START_STATE )
      super._earlyInitMap(lmap)
    }
  }

  /** Отработать готовность карты запуском FSM, обрабатывающего эту форму. */
  override protected def _radMapReady(lmap: LMap): Unit = {
    super._radMapReady(lmap)
    ???
  }

}