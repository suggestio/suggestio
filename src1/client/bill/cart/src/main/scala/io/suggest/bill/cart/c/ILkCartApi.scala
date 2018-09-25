package io.suggest.bill.cart.c

import io.suggest.bill.cart.MOrderContent
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.routes.routes
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 16:41
  * Description: API для взаимодействия с сервером по вопросам биллинга/корзины.
  */
trait ILkCartApi {

  /** Чтение данных ордера с сервера.
    *
    * @param orderId id ордера. None для корзины.
    * @return Фьючерс с результатом запроса.
    */
  def getOrder(orderId: Option[Gid_t]): Future[MOrderContent]

}


/** Реализация Bill-Cart API поверх Http XHR. */
class LkCartApiXhrImpl extends ILkCartApi {

  override def getOrder(orderId: Option[Gid_t]): Future[MOrderContent] = {
    val route = routes.controllers.LkBill2.getOrder(
      orderId = orderId.mapDefined(_.asInstanceOf[Double])
    )
    Xhr.unJsonResp[MOrderContent] {
      Xhr.requestJsonText(route)
    }
  }

}
