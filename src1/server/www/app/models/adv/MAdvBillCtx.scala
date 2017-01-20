package models.adv

import models.MDailyTf
import models.mcal.MCalCtx
import org.joda.time.LocalDate

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.01.17 15:09
  * Description: Модель для объединения common-данных контекста начального биллинга любых размещений карточек.
  * Появился для группировки данных по тарифам, календарям и прочему.
  * Использутся на стадии формы для рассчёта стоимости.
  *
  * @param blockModulesCount Кол-во блоков карточки (площадь карточки).
  * @param dailyTfs Тарифы посуточного размещения.
  * @param calCtx Контекст календарей, необходимых для рассчёта выходных/рабочих дней.
  * @param dateStart Дата начала размещения.
  * @param dateEnd Дата окончания размещения.
  *                Опциональна для бессрочного размещения на все деньги.
  */
case class MAdvBillCtx(
                        blockModulesCount   : Int,
                        dailyTfs            : Map[String, MDailyTf],
                        calCtx              : MCalCtx,
                        dateStart           : LocalDate,
                        dateEnd             : Option[LocalDate]
                      )
