package models.adv

import models.{MDailyTf, MNode}
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

  /** Рекламная карточка. */
  def mad                 : MNode


  /** Упрощённый доступ к id рекламной карточки. */
  def adId = mad.id.get

}


/** wrap-реализация [[IAdvBillCtx]]. */
trait IAdvBillCtxWrap extends IAdvBillCtx {

  /** Нижележащий инстанс [[IAdvBillCtx]], который враппится этим трейтом. */
  def wrapped: IAdvBillCtx

  override def tfsMap             = wrapped.tfsMap
  override def blockModulesCount  = wrapped.blockModulesCount
  override def mcalsCtx           = wrapped.mcalsCtx
  override def ivl                = wrapped.ivl
  override def mad                = wrapped.mad
}


/** Дефолтовая реализация контейнера [[IAdvBillCtx]]. */
case class MAdvBillCtx(
                        override val blockModulesCount   : Int,
                        override val mcalsCtx            : ICalsCtx,
                        override val tfsMap              : Map[String, MDailyTf],
                        override val ivl                 : IDateStartEnd,
                        override val mad                 : MNode
                      )
  extends IAdvBillCtx