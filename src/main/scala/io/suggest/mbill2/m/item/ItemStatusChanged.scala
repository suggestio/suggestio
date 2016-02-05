package io.suggest.mbill2.m.item

import io.suggest.event.SioEventT
import io.suggest.event.SioNotifier.Classifier
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemType

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.16 16:40
  * Description: Событие для SioNotifier, оповещающее об изменении статуса какого-то item'а (товара/услуги).
  */
object ItemStatusChanged {

  private def headSne = Some( classOf[ItemStatusChanged].getSimpleName )

  def classifier(status   : Option[MItemStatus] = None,
                 iType    : Option[MItemType]   = None,
                 orderId  : Option[Gid_t]       = None): Classifier = {
    List( headSne, status, iType, orderId )
  }

}


case class ItemStatusChanged(mitem: MItem) extends SioEventT {

  override def getClassifier: Classifier = {
    ItemStatusChanged.classifier(
      status  = Some(mitem.status),
      iType   = Some(mitem.iType),
      orderId = mitem.id
    )
  }

}
