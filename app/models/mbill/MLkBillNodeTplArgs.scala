package models.mbill

import models.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.16 12:08
  * Description: Контейнер для аргументы вызова шаблона биллинга узла в личном кабинете.
  * [[views.html.lk.billing.showAdnNodeBillingTpl]].
  */
trait ILkBillNodeTplArgs {

  /** Текущий узел. */
  def mnode: MNode

  /** Аргументы для рендера посуточного тарифа текущего узла-ресивера. */
  def dailyTfArgs: Option[IDailyTfTplArgs]

}


/** Дефолтовая реализация модели [[ILkBillNodeTplArgs]]. */
case class MLkBillNodeTplArgs(
  override val mnode        : MNode,
  override val dailyTfArgs  : Option[IDailyTfTplArgs]
)
  extends ILkBillNodeTplArgs
