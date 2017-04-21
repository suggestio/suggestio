package util.adv.direct

import java.time.ZoneId

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.MPrice
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.primo.id.OptId
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.adv.MAdvBillCtx
import models.adv.direct.{AdvFormEntry, FormResult}
import models.mcal.ICalsCtx
import models.mproj.ICommonDi
import util.adv.AdvUtil
import util.billing.TfDailyUtil
import util.cal.CalendarUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 19:04
 * Description: Утиль для работы с биллингом, где имеют вес площади и расценки получателя рекламы.
 */
@Singleton
class AdvDirectBilling @Inject() (
  tfDailyUtil             : TfDailyUtil,
  advUtil                 : AdvUtil,
  calendarUtil            : CalendarUtil,
  mItems                  : MItems,
  val mCommonDi           : ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi._
  import slick.profile.api._


  /** Довольно грубый метод поиска уже занятых карточкой ресиверов.
    * Нужно переписать форму, чтобы можно было размещать в незанятые даты. */
  def findBusyRcvrsMap(adId: String): DBIOAction[Map[String, MItem], NoStream, Effect.Read] = {
    for {
      items <- sqlForAd(adId).result
    } yield {
      val iter = for {
        mitem <- items
        rcvrId <- mitem.rcvrIdOpt
      } yield {
        rcvrId -> mitem
      }
      iter.toMap
    }
  }

  private def sqlForAd(adId: String) = {
    mItems
      .query
      .filter { i =>
        (i.nodeId === adId) &&
          (i.iTypeStr === MItemTypes.AdvDirect.strId) &&
          (i.statusStr inSet MItemStatuses.advBusyIds.toSeq)
      }
  }

  /** Поиск занятых ресиверов для карточки среди запрошенных для размещения. */
  def findBusyRcvrsExact(adId: String, fr: FormResult): DBIOAction[Set[String], NoStream, Effect.Read] = {
    // Подготовить значения аргументов для вызова экшена
    val dtStart = fr.period.dateStart.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime
    val dtEnd   = fr.period.dateEnd.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime

    // Сборка логики db-экшена
    val dbAction = sqlForAd(adId)
      .filter { i =>
        // Интересуют только ряды, которые относятся к прямому размещению текущей карточки...
        (i.rcvrIdOpt inSet fr.nodeIdsIter.toSet) && (
          // Хотя бы одна из дат (start, end) должна попадать в уже занятый период размещения.
          (i.dateStartOpt <= dtStart && i.dateEndOpt >= dtStart) ||
          (i.dateStartOpt <= dtEnd   && i.dateEndOpt >= dtEnd)
        )
      }
      .map { i =>
        i.rcvrIdOpt
      }
      .result

    // Отмаппить результат экшена прямо в экшене, т.к. он слишком некрасивый.
    for (seqOpts <- dbAction) yield {
      seqOpts.iterator
        .flatten
        .toSet
    }
  }


  /**
   * Посчитать цену размещения рекламной карточки согласно переданной спеке.
   *
   * @param mad Размещаемая рекламная карточка.
   * @param adves2 Требования по размещению карточки на узлах.
   */
  def getAdvPrices(mad: MNode, adves2: Seq[AdvFormEntry]): Future[Seq[MPrice]] = {
    val rcvrsFut = mNodesCache.multiGet {
      adves2
        .iterator
        .map(_.adnId)
        .toSet
    }

    // Карта тарифов по id узлов.
    val tfsMapFut = rcvrsFut.flatMap( tfDailyUtil.getNodesTfsMap )

    val rcvrsMapFut = OptId.elsFut2idMapFut[String, MNode](rcvrsFut)

    // Получить необходимые календари, также составив карту по id
    val calsCtxFut = tfsMapFut.flatMap { tfsMap =>
      val calIds = tfDailyUtil.tfsMap2calIds( tfsMap )
      calendarUtil.getCalsCtx(calIds)
    }

    // Пока посчитать размеры карточки
    val bmc = advUtil.maybeAdModulesCount(mad)

    // Когда всё будет готово, нужно нагенерить результатов.
    for {
      tfsMap    <- tfsMapFut
      calsCtx   <- calsCtxFut
      rcvrsMap  <- rcvrsMapFut
    } yield {
      // Выдать список цен из списка запрашиваемых размещений.
      adves2
        .iterator
        .map { adve =>
          val abc = MAdvBillCtx(bmc, calsCtx, tfsMap, adve, rcvrsMap)
          advUtil.calcDateAdvPriceOnTf(adve.adnId, abc)
            .price
            .normalizeAmountByExponent
        }
        .toSeq
      // Для суммирования списка по валютам и получения итоговой цены надо использовать MPrice.sumPricesByCurrency().
    }
  }

  /**
    * Сохранить в БД реквесты размещения рекламных карточек.
    *
    * @param orderId id ордера-корзины.
    * @param mad рекламная карточка.
    * @param advs Список запросов на размещение.
    * @param rcvrTfs Карта тарифов ресиверов, ключ -- это id узла-ресивера.
    *                См. [[util.billing.TfDailyUtil.getNodesTfsMap()]].
    * @param mcalsCtx Контекст календарей.
    * @return db-экшен, добавляющий запросы размещения в корзину.
    */
  def mkAdvReqItems(orderId: Gid_t, mad: MNode, advs: TraversableOnce[AdvFormEntry], status: MItemStatus,
                    rcvrTfs: Map[String, MDailyTf], mcalsCtx: ICalsCtx, rcvrsMap: Map[String, MNode]): Iterator[MItem] = {
    val bmc = advUtil.maybeAdModulesCount(mad)

    for {
      adv <- advs.toIterator
      if adv.advertise
    } yield {
      val abc = MAdvBillCtx(bmc, mcalsCtx, rcvrTfs, adv, rcvrsMap)
      MItem(
        orderId       = orderId,
        iType         = MItemTypes.AdvDirect,
        status        = status,
        price         = advUtil.calcDateAdvPriceOnTf(adv.adnId, abc).price
          .normalizeAmountByExponent,
        nodeId        = mad.id.get,
        // TODO Тут java.time-словоблудие. Всё равно весь класс будет удалён вместе с формой, поэтому точность и дубликация кода тут не важна, лишь бы по-быстрее двигаться:
        dateStartOpt  = Some( adv.dateStart.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime ),
        dateEndOpt    = Some( adv.dateEnd.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime ),
        rcvrIdOpt     = Some(adv.adnId)
      )
    }
  }


  /** Есть ли прямые размещения на указанном узле?
    *
    * @param nodeId id целевого узла.
    * @return true, если есть хотя бы один busy adv item прямого размещения на указанном узле.
    */
  def hasAnyBusyToNode(nodeId: String): DBIOAction[Boolean, NoStream, Effect.Read] = {
    mItems.query
      .filter { i =>
        i.withNodeId( nodeId ) &&
          i.withStatuses( MItemStatuses.advBusy ) &&
          i.withTypes1( MItemTypes.AdvDirect, MItemTypes.TagDirect )
      }
      .exists
      .result
  }

}
