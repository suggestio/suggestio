package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MJsCtxFieldsT

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 13:48
 * Description: Состояние запроса. Через этого также происходит обмен данными.
 */

object MJsCtx extends MJsCtxFieldsT with FromStringT {

  override type T = MJsCtx

  def fromDyn(dyn: js.Dynamic): MJsCtx = {
    val d = dyn.asInstanceOf[js.Dictionary[js.Dynamic]]
    MJsCtx(
      action = d.get(ACTION_FN).toString,
      service = MServiceInfo.fromDyn(d.get(SERVICE_FN).get),
      domain = d.get(DOMAIN_FN) match {
        case Some(domainsRaw) =>
          domainsRaw.asInstanceOf[js.Array[String]]
        case None => js.Array()
      }
      // status игнорим, ибо плевать
    )
  }

}


import MJsCtx._


case class MJsCtx(
  action  : String,
  service : MServiceInfo,
  domain  : js.Array[String],
  status  : Option[MAnswerStatus]  = None
) {

  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    lit.updateDynamic(ACTION_FN)(action)
    lit.updateDynamic(SERVICE_FN)(service.toJson)
    if (domain.nonEmpty)
      lit.updateDynamic(DOMAIN_FN)(domain)
    if (status.isDefined)
      lit.updateDynamic(STATUS_FN)(status.get.jsStr)
    lit
  }

}
