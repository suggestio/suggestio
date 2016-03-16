package io.suggest.lk.adv.geo.tags.init

import io.suggest.lk.adv.geo.tags.fsm.AgtFormFsm
import io.suggest.lk.adv.geo.tags.m.signal.TagsChanged
import io.suggest.lk.router.jsRoutes
import io.suggest.lk.tags.edit.fsm.TagsEditFsm
import io.suggest.maps.rad.init.RadMapInit
import io.suggest.sjs.common.controller.{IInit, IInitDummy, InitRouter}
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.util.SjsLogger

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
class AgtFormInit
  extends IInitDummy
  with AgtFormInitT
  with RadMapInit // Карта должна инициализироваться в конце.
  with SjsLogger


/** Аддон для abstract override для инициализации всех fsm этой формы. */
trait AgtFormInitT extends IInit {

  /** Запуск инициализации текущего модуля. */
  abstract override def init(): Unit = {
    super.init()

    // Запустить тут основной FSM:
    val mainFsm = new AgtFormFsm
    mainFsm.start()

    // Собрать и запустить FSM для тегов.
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
  }

}
