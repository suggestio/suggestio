package models.mbill

import io.suggest.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.16 12:08
  * Description: Контейнер для аргументы вызова шаблона биллинга узла в личном кабинете.
  * @see [[views.html.lk.billing.nodeBillingTpl]].
  */
final case class MLkBillNodeTplArgs(
                                     mnode        : MNode,
                                     dailyTfArgs  : Option[MTfDailyTplArgs]
                                   )
