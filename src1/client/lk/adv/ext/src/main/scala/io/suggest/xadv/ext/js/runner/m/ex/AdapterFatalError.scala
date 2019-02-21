package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.ext.svc.MExtService
import io.suggest.xadv.ext.js.runner.m.MErrorInfoT

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 15:56
 * Description: Адаптер обнаружил какое-то сильное нарушение в работе. Например потерянные данные в контексте
 * или что-то ещё, к чему скорее всего причастен только код адаптера.
 */
case class AdapterFatalError(service: MExtService, einfo: String) extends Error with MErrorInfoT {
  override def msg = "e.adv.ext.adp.internal"
  override def args = service.value :: Nil

  override def info = Some(Dictionary[Any](
    "msg" -> einfo
  ))
}
