package models.mbill

import io.suggest.mbill2.m.gid.Gid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.04.17 11:49
  * Description: Аргументы для рендера шаблона [[views.html.lk.billing.order.OrderPaidEmailTpl]].
  *
  * @param orderId Номер оплаченного заказа.
  * @param withHello Рендерить преветствие?
  *                  None - нет.
  *                  Some(None) - обезличенное приветствие: Здравствуйте!
  *                  Some(Some("Vasya")) - личное приветствие: Здравствуйте, Vasya!
  * @param asEmail Рендерить как email-сообщение?
  */
case class MEmailOrderPaidTplArgs(
                                   asEmail    : Boolean,
                                   orderId    : Gid_t,
                                   onNodeId   : Option[String],
                                   withHello  : Option[Option[String]]
                                 ) {

  /** Internal constant to suppress rendering problems with messages().
    * With orderId.toString, messages() will render something like "Order #1 674" -- it looks bad.
    */
  val orderIdStr = orderId.toString

}
