package models.mbill

import io.suggest.n2.bill.tariff.daily.MTfDaily
import io.suggest.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.16 22:31
  * Description: Модель аргументов для шаблона [[views.html.lk.billing._dailyTfTpl]].
  */

final case class MTfDailyTplArgs(
                                  mnode      : MNode,
                                  tfDaily    : MTfDaily,
                                  madTfOpt   : Option[MAdTfInfo],
                                  calsMap    : Map[String, MNode],
                                )


/** Инфа по фактическим ценам размещения карточки. */
case class MAdTfInfo(
                      modulesCount  : Int,
                      tfDaily       : MTfDaily
                    )