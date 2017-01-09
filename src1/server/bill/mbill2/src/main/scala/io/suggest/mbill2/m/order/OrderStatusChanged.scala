package io.suggest.mbill2.m.order

import io.suggest.event.SioEventT
import io.suggest.event.SioNotifier.Classifier

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.16 17:42
  * Description: Событие измения статуса ордера.
  */
object OrderStatusChanged {

  private def headSne = Some( classOf[OrderStatusChanged].getSimpleName )

  def getClassifier(status: Option[MOrderStatus] = None): Classifier = {
    List(headSne, status)
  }

}


case class OrderStatusChanged(morder: MOrder) extends SioEventT {
  override def getClassifier: Classifier = {
    OrderStatusChanged.getClassifier(
      status = Some(morder.status)
    )
  }
}
