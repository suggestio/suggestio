package io.suggest.lk.adv.geo.tags.init

import io.suggest.lk.adv.geo.tags.fsm.AgtFormFsm
import io.suggest.lk.adv.geo.tags.m.signal.{RadiusChanged, TagsChanged}
import io.suggest.lk.adv.geo.tags.vm.AgtForm
import io.suggest.lk.router.jsRoutes
import io.suggest.lk.tags.edit.fsm.TagsEditFsm
import io.suggest.maps.rad.init.RadMapInit
import io.suggest.sjs.common.controller.{IInit, InitRouter}
import io.suggest.sjs.common.fsm.IInitLayoutFsm
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.leaflet.event.Event
import io.suggest.sjs.leaflet.path.circle.Circle

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 10:24
 * Description: Инициализация формы размещения в геотегах.
 */
trait AgtFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.AdvGtagForm) {
      Future {
        new AgtFormInit()
          .init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Класс инициализации формы размещения в тегах. */
class AgtFormInit extends IInit {

  /** Запуск инициализации текущего модуля. */
  override def init(): Unit = {

    // Инициализировать основной FSM:
    val mainFsm = new AgtFormFsm
    mainFsm.start()

    // Привязать форму к созданному FSM.
    val ilf = IInitLayoutFsm.f(mainFsm)
    AgtForm.find().foreach(ilf)


    // Инициализировать FSM редактора тегов.
    val tagsFsm = new TagsEditFsm {
      override protected def _addTagRoute: Route = {
        jsRoutes.controllers.LkAdvGeoTag.tagEditorAddTag()
      }
      override protected def _tagsChanged(): Unit = {
        super._tagsChanged()
        // Когда что-то изменяется в наборе тегов, необходимо асинхронно уведомлять основной FSM.
        mainFsm ! TagsChanged
      }
    }
    tagsFsm.start()


    // Инициализировать географическую карту
    val rmapInit = new RadMapInit {
      /** Нужно уведомлять FSM формы при изменении радиуса круга. */
      override def onRadiusChange(circle: Circle, e: Event): Unit = {
        super.onRadiusChange(circle, e)
        mainFsm ! RadiusChanged
      }
    }
    rmapInit.init()
  }

}
