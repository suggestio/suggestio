package io.suggest.lk.adv.gtag.init

import io.suggest.lk.tags.edit.TagsEditInit
import io.suggest.maps.rad.init.RadMapInit
import io.suggest.sjs.common.controller.{IInitDummy, InitRouter}
import io.suggest.sjs.common.util.SjsLogger

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
