package models.mdr

import io.suggest.bill.MPrice
import io.suggest.common.empty.EmptyProduct
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.txn.MTxn

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.17 16:41
  * Description: Верстка для шаблона письма модераторам: [[views.html.sys1.mdr._mdrNeededEmailTpl]].
  */
object MSysMdrEmailTplArgs {

  def empty = MSysMdrEmailTplArgs()

}


case class MSysMdrEmailTplArgs(
                                paid      : Option[MPrice]  = None,
                                orderId   : Option[Gid_t]   = None,
                                txn       : Option[MTxn]    = None,
                                personId  : Option[String]  = None,
                                personName: Option[String]  = None
                              )
  extends EmptyProduct
