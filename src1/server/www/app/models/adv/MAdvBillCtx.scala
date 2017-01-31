package models.adv

import models.MDailyTf
import models.mcal.ICalsCtx
import models.mdt.IDateStartEnd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.01.17 15:09
  * Description: Модель для объединения common-данных контекста начального биллинга любых размещений карточек.
  * Появился для группировки стабильных данных по тарифам, календарям и прочему.
  * Использутся на стадии формы для рассчёта стоимости в качестве данных для рассчёта стоимости по тарифу.
  */
trait IAdvBillCtx {

  def tfsMap              : Map[String, MDailyTf]

  /** Кол-во блоков карточки (площадь карточки). */
  def blockModulesCount   : Int

  /** Контекст календарей, необходимых для рассчёта выходных/рабочих дней. */
  def mcalsCtx            : ICalsCtx

  /** Период размещения. */
  def ivl                 : IDateStartEnd

}


/** Дефолтовая реализация контейнера [[IAdvBillCtx]]. */
case class MAdvBillCtx(
                        override val blockModulesCount   : Int,
                        override val mcalsCtx            : ICalsCtx,
                        override val tfsMap              : Map[String, MDailyTf],
                        override val ivl                 : IDateStartEnd
                      )
  extends IAdvBillCtx