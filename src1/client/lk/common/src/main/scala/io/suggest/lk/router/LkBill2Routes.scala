package io.suggest.lk.router

import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Future
import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 12:26
 * Description: js-router для обращения к серверу за новыми данными транзакций.
 */
@js.native
sealed trait LkBill2Routes extends js.Object {

  def txnsList(adnId: String, page: Int, inline: Boolean): Route = js.native

  /** Получить бинарь с данными размещения по узлу. */
  def nodeAdvInfo(nodeId: String, forAdId: String = null): Route = js.native

}


/** Интерфейс для API LkBill2 nodeAdvInfo(). */
trait ILkBill2NodeAdvInfoApi {

  def nodeAdvInfo(nodeId: String): Future[MNodeAdvInfo]

}


/** HTTP-реализация [[ILkBill2NodeAdvInfoApi]]. */
trait LkBill2NodeAdvInfoHttpApiImpl extends ILkBill2NodeAdvInfoApi {

  protected[this] def _nodeAdvInfoRoute(nodeId: String): Route = {
    jsRoutes.controllers.LkBill2.nodeAdvInfo( nodeId = nodeId )
  }

  override def nodeAdvInfo(nodeId: String): Future[MNodeAdvInfo] = {
    Xhr.unBooPickleResp[MNodeAdvInfo](
      Xhr.requestBinary( _nodeAdvInfoRoute(nodeId) )
    )
  }

}
