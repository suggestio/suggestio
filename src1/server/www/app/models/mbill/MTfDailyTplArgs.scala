package models.mbill

import io.suggest.model.n2.bill.tariff.daily.MTfDaily
import io.suggest.model.n2.node.MNode
import models.mcal.MCalendar

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.16 22:31
  * Description: Модель аргументов для шаблона [[views.html.lk.billing._dailyTfTpl]].
  */

trait ITfDailyTplArgs {

  /** Текущий узел. */
  def mnode: MNode

  /** Посуточный тариф прямого размещения на данном узле. */
  def tfDaily: MTfDaily

  /** Если надо отрендерить цены в контексте карточки, то тут данные по ценам карточки. */
  def madTfOpt: Option[MAdTfInfo]

  /** Календари, используются для рендера тарифа. */
  def calsMap: Map[String, MCalendar]

}


/** Дефолтовая реализация [[ITfDailyTplArgs]]. */
case class MTfDailyTplArgs(
                            override val mnode      : MNode,
                            override val tfDaily    : MTfDaily,
                            override val madTfOpt   : Option[MAdTfInfo],
                            override val calsMap    : Map[String, MCalendar]
)
  extends ITfDailyTplArgs


/** Инфа по фактическим ценам размещения карточки. */
case class MAdTfInfo(
                      modulesCount  : Int,
                      tfDaily       : MTfDaily
                    )