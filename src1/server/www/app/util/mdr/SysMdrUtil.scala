package util.mdr

import java.time.OffsetDateTime

import com.google.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.mbill2.m.item.{IMItem, ItemStatusChanged, MItem, MItems}
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.util.effect.RW
import io.suggest.model.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.model.n2.node.MNodes
import models.MNode
import models.mdr.{MRefuseFormRes, MRefuseModes, MdrSearchArgs, RefuseForm_t}
import models.mproj.ICommonDi
import models.req.{IAdReq, IReqHdr}
import util.PlayMacroLogsImpl

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.16 11:13
  * Description: Всякая утиль для контроллера [[controllers.SysMdr]]: формы, фунции и прочее.
  */
class SysMdrUtil @Inject() (
  val mItems        : MItems,
  mNodes            : MNodes,
  val mCommonDi     : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._

  def someNow = Some(OffsetDateTime.now)

  /** Сборка эджа от текущего модератора.
    *
    * @param reasonOpt None значит всё хорошо,
    *                  Some() значит карточка не прошла модерацию по указанной причине.
    * @return Готовый экземляр MEdge
    */
  def mdrEdge(reasonOpt: Option[String] = None)(implicit request: IReqHdr): MEdge = {
    mdrEdgeI {
      MEdgeInfo(
        dateNi    = someNow,
        commentNi = reasonOpt,
        flag      = Some(reasonOpt.isEmpty)
      )
    }
  }

  /** Сборка эджа текущего модератора с указанной инфой по модерации. */
  def mdrEdgeI(einfo: MEdgeInfo)(implicit request: IReqHdr): MEdge = {
    MEdge(
      predicate = MPredicates.ModeratedBy,
      nodeIds   = request.user.personIdOpt.toSet,
      info      = einfo
    )
  }

  /** Код обновления эджа модерации живёт здесь. */
  def updMdrEdge(info: MEdgeInfo)(implicit request: IAdReq[_]): Future[MNode] = {
    // Сгенерить обновлённые данные модерации.
    val mdr2 = mdrEdgeI(info)

    LOGGER.trace(s"_updMdrEdge() Mdr mad[${request.mad.idOrNull}] with mdr-edge $mdr2")

    // Запускаем сохранение данных модерации.
    mNodes.tryUpdate(request.mad) { mad0 =>
      mad0.copy(
        edges = mad0.edges.copy(
          out = {
            val iter0 = mad0.edges.withoutPredicateIter( MPredicates.ModeratedBy )
            val iter2 = Iterator(mdr2)
            MNodeEdges.edgesToMap1(iter0 ++ iter2)
          }
        )
      )
    }
  }


  import play.api.data._
  import Forms._
  import util.FormUtil._


  /** Маппинг причины отказа. */
  def reasonM: Mapping[String] = {
    nonEmptyText(minLength = 4, maxLength = 1024)
      .transform(strTrimSanitizeF, strIdentityF)
  }

  /** Маппинг формы отказа в размещении. */
  def refuseFormM: RefuseForm_t = {
    val m = mapping(
      "reason"  -> reasonM,
      "mode"    -> MRefuseModes.mappingOpt
    )
    { MRefuseFormRes.apply }
    { MRefuseFormRes.unapply }
    Form(m)
  }


  import slick.profile.api._


  type Q_t = Query[mItems.MItemsTable, MItem, Seq]

  def onlyReqs(q: Q_t): Q_t = {
    q.filter( _.statusStr === MItemStatuses.AwaitingMdr.strId)
  }

  def onlyStatuses(q: Q_t, statuses: Seq[MItemStatus]): Q_t = {
    q.filter(_.statusStr inSet statuses.iterator.map(_.strId).toSeq)
  }

  def onlyItype(q: Q_t, itype: MItemType): Q_t = {
    q.filter( _.iTypeStr === itype.strId)
  }

  /** Общий код сборки всех SQL queries для сборки items модерации карточки. */
  def itemsQuery(nodeId: String): Q_t = {
    mItems.query
      .filter(_.nodeId === nodeId)
  }

  def itemsQueryAwaiting(nodeId: String): Q_t = {
    onlyReqs(
      itemsQuery(nodeId)
    )
  }


  /** Логика поштучной обработки item'ов. */
  def _processOneItem[Res_t <: IMItem](dbAction: DBIOAction[Res_t, NoStream, RW]): Future[Res_t] = {
    // Запуск обновления MItems.
    val saveFut = slick.db.run {
      dbAction.transactionally
    }

    // Обрадовать другие компоненты системы новым событием
    saveFut.onSuccess { case res =>
      sn.publish( ItemStatusChanged(res.mitem) )
    }

    saveFut
  }


  /** Результат вызова _processItemsForAd(). */
  sealed case class ProcessItemsRes(itemIds: Seq[Gid_t], successMask: Seq[Boolean], itemsCount: Int)

  /** Логика массовой обработки item'ов. */
  def _processItemsForAd[Res_t <: IMItem](nodeId: String, q: Q_t)
                                         (f: Gid_t => DBIOAction[Res_t, NoStream, RW]): Future[ProcessItemsRes] = {
    // TODO Opt Тут можно db.stream применять
    val itemIdsFut = slick.db.run {
      q.map(_.id)
        .result
    }

    lazy val logPrefix = s"_processItemsForAd($nodeId ${System.currentTimeMillis}):"
    LOGGER.trace(s"$logPrefix Bulk approve items, $f")

    for {
      itemIds <- itemIdsFut
      saveFut = Future.traverse(itemIds) { itemId =>
        _processOneItem(f(itemId))
          // Следует мягко разруливать ситуации, когда несколько модераторов одновременно аппрувят item'ы одновременно.
          .map { _ => true }
          .recover {
            // Вероятно, race conditions двух модераторов.
            case _: NoSuchElementException =>
              LOGGER.warn(s"$logPrefix Possibly conficting mdr MItem UPDATE. Suppressed.")
              false
            case ex: Throwable =>
              LOGGER.error(s"$logPrefix Unknown error occured while approving item $itemId", ex)
              true
          }
      }
      itemsCount  = itemIds.size
      saveRes     <- saveFut

    } yield {
      ProcessItemsRes(itemIds, saveRes, itemsCount)
    }
  }


  /** SQL для экшена поиска id карточек, нуждающихся в модерации. */
  def _findPaidAdIds4MdrAction(args: MdrSearchArgs) = {
    val b0 = mItems
      .query
      .filter { i =>
        (i.iTypeStr inSet MItemTypes.advTypesIds.toSeq) &&
          (i.statusStr === MItemStatuses.AwaitingMdr.strId)
      }

    val b1 = args.hideAdIdOpt.fold(b0) { hideAdId =>
      b0.filter { i =>
        i.nodeId =!= hideAdId
      }
    }

    b1.map(_.nodeId)
      //.sortBy(_.id.asc)   // TODO Нужно подумать над сортировкой возвращаемого множества adId
      .distinct
      .take( args.limit )
      .drop( args.offset )
      .result
  }

}


/** Интерфейс для DI-поля с инстансом [[SysMdrUtil]]. */
trait ISysMdrUtilDi {
  def sysMdrUtil: SysMdrUtil
}
