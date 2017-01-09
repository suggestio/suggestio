package models.mbill

import models.mcal.MCalendar
import models.{MDailyTf, MNode}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.16 22:31
  * Description: Модель аргументов для шаблона [[views.html.lk.billing._dailyTfTpl]].
  */

trait IDailyTfTplArgs {

  /** Текущий узел. */
  def mnode: MNode

  /** Посуточный тариф прямого размещения на данном узле. */
  def dailyTf: MDailyTf

  /** Календари, используются для рендера тарифа. */
  def calsMap: Map[String, MCalendar]

}


/** Дефолтовая реализация [[IDailyTfTplArgs]]. */
case class MDailyTfTplArgs(
  override val mnode      : MNode,
  override val dailyTf    : MDailyTf,
  override val calsMap    : Map[String, MCalendar]
)
  extends IDailyTfTplArgs
