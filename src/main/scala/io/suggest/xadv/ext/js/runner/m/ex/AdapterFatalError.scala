package io.suggest.xadv.ext.js.runner.m.ex

import io.suggest.xadv.ext.js.runner.m.{MService, MErrorInfoT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 15:56
 * Description: Адаптер обнаружил какое-то сильное нарушение в работе. Например потерянные данные в контексте
 * или что-то ещё, к чему скорее всего причастен только код адаптера.
 */
case class AdapterFatalError(service: MService, info: String) extends Error with MErrorInfoT {
  override def msg: String = "e.adv.ext.adp.internal"
  override def args: Seq[String] = Seq(service.strId, info)
}
