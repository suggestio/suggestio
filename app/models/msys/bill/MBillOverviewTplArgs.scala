package models.msys.bill

import io.suggest.mbill2.m.balance.MBalance
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.txn.MTxn

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.02.16 12:04
  * Description: Модель аргументов для вызова шаблона [[views.html.sys1.bill.overviewTpl]].
  */

trait IBillOverviewTplArgs extends IBillTxnsListTplArgs {
}


/** Дефолтовая реализация модели [[IBillOverviewTplArgs]]. */
case class MBillOverviewTplArgs(
  override val txns         : Seq[MTxn],
  override val balancesMap  : Map[Gid_t, MBalance]
)
  extends IBillOverviewTplArgs
