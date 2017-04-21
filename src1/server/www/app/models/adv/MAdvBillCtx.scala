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
  def blockModulesCount   : Option[Int]

  /** Контекст календарей, необходимых для рассчёта выходных/рабочих дней. */
  def mcalsCtx            : ICalsCtx

  /** Период размещения. */
  def ivl                 : IDateStartEnd

  /** Рекламная карточка. */
  def mad                 : MNode

  /** Карта ресиверов. */
  def rcvrsMap            : Map[String, MNode]

  /** Упрощённый доступ к id рекламной карточки. */
  def adId = mad.id.get

  override def toString: String = {
    s"${getClass.getSimpleName}(${tfsMap.size}tfs${blockModulesCount.fold("")(",bmc=" + _)},$mcalsCtx,mad#${mad.id.orNull})"
  }

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
  override def rcvrsMap           = wrapped.rcvrsMap
}


/** Дефолтовая реализация контейнера [[IAdvBillCtx]]. */
case class MAdvBillCtx(
                        override val blockModulesCount   : Option[Int],
                        override val mcalsCtx            : ICalsCtx,
                        override val tfsMap              : Map[String, MDailyTf],
                        override val ivl                 : IDateStartEnd,
                        override val mad                 : MNode,
                        override val rcvrsMap            : Map[String, MNode]
                      )
  extends IAdvBillCtx
{

  def withRcvrsMap(rcvrsMap2: Map[String, MNode]) = copy(rcvrsMap = rcvrsMap2)

}