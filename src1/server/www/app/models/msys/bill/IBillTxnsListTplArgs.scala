package models.msys.bill

import io.suggest.mbill2.m.balance.MBalance
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.txn.MTxn

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.02.16 12:04
  * Description: Модель аргументов для вызова шаблона sys-списка транзакций.
  */

trait IBillTxnsListTplArgs {

  /** Отображаемые транзакции в желаемом порядке. */
  def txns: Seq[MTxn]

  /** Балансы, связанные с транзакциями. */
  def balancesMap: Map[Gid_t, MBalance]

}
