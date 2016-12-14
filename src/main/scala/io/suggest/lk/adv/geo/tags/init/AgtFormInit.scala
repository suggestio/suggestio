package io.suggest.lk.adv.geo.tags.init

import io.suggest.lk.adv.geo.r.AdvGeoFormR
import io.suggest.lk.adv.geo.tags.fsm.AgtFormFsm
import io.suggest.lk.adv.geo.tags.m.signal.{RadiusChanged, TagsChanged}
import io.suggest.lk.adv.geo.tags.vm.{AdIdInp, AgtForm}
import io.suggest.lk.adv.m.IAdv4FreeProps
import io.suggest.lk.price.vm.PriceUrlInput
import io.suggest.lk.router.jsRoutes
import io.suggest.lk.tags.edit.fsm.TagsEditFsm
import io.suggest.sjs.common.controller.{IInit, InitRouter}
import io.suggest.sjs.common.fsm.IInitLayoutFsm
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.tags.search.ITagSearchArgs
import io.suggest.sjs.leaflet.event.Event
import io.suggest.sjs.leaflet.path.circle.Circle
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.js

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
        new AgtFormInit2()
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
    val mainFsm = AgtFormFsm
    mainFsm.start()

    // Привязать форму к созданному FSM.
    AgtForm.find()
      .foreach( IInitLayoutFsm.f(mainFsm) )

    // Инициализировать FSM редактора тегов.
    val tagsFsm = new TagsEditFsm {
      override def _addTagRoute: Route = {
        jsRoutes.controllers.LkAdvGeo.tagEditorAddTag()
      }
      override def tagsSearchRoute(args: ITagSearchArgs): Route = {
        jsRoutes.controllers.LkAdvGeo.tagsSearch(args.toJson)
      }
      override def _tagsChanged(): Unit = {
        super._tagsChanged()
        // Когда что-то изменяется в наборе тегов, необходимо асинхронно уведомлять основной FSM.
        mainFsm ! TagsChanged
      }
    }
    tagsFsm.start()


    // Инициализировать географическую карту
    val rmapInit = new AgtFormMapInit {
      /** Нужно уведомлять FSM формы при изменении радиуса круга. */
      override def onRadiusChange(circle: Circle, e: Event): Unit = {
        super.onRadiusChange(circle, e)
        mainFsm ! RadiusChanged
      }
    }
    rmapInit.init()
  }

}


/** Инициализатор формы георазмещения второго поколения на базе react.js. */
class AgtFormInit2 extends IInit {

  /** Запуск инициализации текущего модуля. */
  override def init(): Unit = {

    val adId = AdIdInp.find()
      .flatMap( _.adId )
      .get

    val rform = AdvGeoFormR.apply(
      // TODO Заполнять пропертисы с сервера.
      AdvGeoFormR.Props(
        adId          = adId,
        adv4free      = None
        /*Some( new IAdv4FreeProps {
          override def title  = "Размещать нахаляву?"
          override def fn     = "adv4ffreee"
        })*/
      )
    )
    ReactDOM.render(rform, dom.document.getElementById("xynta"))
  }

}
