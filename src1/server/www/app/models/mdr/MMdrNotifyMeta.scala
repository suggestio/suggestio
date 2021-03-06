package models.mdr

import io.suggest.bill.MPrice
import io.suggest.common.empty.EmptyProduct
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.txn.MTxn

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.17 16:41
  * Description: Модель-контейнер метаданных окружения запуска процедуры уведомления о необходимости модерации.
  */
object MMdrNotifyMeta {

  def empty = MMdrNotifyMeta()

}


/** Контейнер данных для рендера шаблона [[views.html.sys1.mdr._mdrNeededEmailTpl]].
  *
  * @param paidTotal
  * @param orderId
  * @param txn
  * @param personId
  * @param personName
  * @param toMdrNodeId На какой узел рендерить ссылку. Если None, то будет
  */
case class MMdrNotifyMeta(
                           paidTotal    : Option[MPrice]  = None,
                           orderId      : Option[Gid_t]   = None,
                           txn          : Option[MTxn]    = None,
                           personId     : Option[String]  = None,
                           personName   : Option[String]  = None,
                           toMdrNodeId  : Option[String]  = None,
                         )
  extends EmptyProduct
