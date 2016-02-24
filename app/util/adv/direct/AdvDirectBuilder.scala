package util.adv.direct

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.model.n2.edge._
import io.suggest.model.sc.common.SinkShowLevel
import models.MNode
import models.adv.build.Acc
import org.joda.time.DateTime
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.16 21:23
  * Description: adv-билдер bill2-mitem прямых размещений на узлах.
  */
trait AdvDirectBuilder extends IAdvBuilder {

  import di._
  import mCommonDi._
  import dbConfig.driver.api._

  private def _PRED   = MPredicates.Receiver
  private def _ITYPE  = MItemTypes.AdvDirect

  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  /** Очистить все прямые размещения карточки по биллингу.
    * Используется для рассчета состояния с нуля, вместо обновления существующего состояния. */
  override def clearAd(): IAdvBuilder = {
    val accFut2 = for {
      acc0 <- super.clearAd().accFut
    } yield {
      acc0.copy(
        mad = acc0.mad.copy(
          edges = acc0.mad.edges.copy(
            out = {
              // Выкидываем всех ресиверов без разбору.
              val iter = acc0.mad.edges.withoutPredicateIter( _PRED )
              MNodeEdges.edgesToMap1( iter )
            }
          )
        )
      )
    }
    withAcc( accFut2 )
  }


  /** Установка прямого размещения в карточку. */
  override def install(mitem: MItem): IAdvBuilder = {
    if (mitem.iType == _ITYPE) {
      withAccUpdated { acc0 =>
        _installDirect(mitem, acc0)
      }
    } else {
      // mitem относится к какому-то иному типу размещения, вне полномочий данного аддона.
      super.install(mitem)
    }
  }

  /** Логика установки прямого размещения для карточки. */
  private def _installDirect(mitem: MItem, acc0: Acc): Acc = {
    val mitemId = mitem.id.get
    lazy val logPrefix = s"_installDirect($mitemId/${System.currentTimeMillis}):"

    // Это инсталляция услуги прямого размещения на [оплаченном] узле.
    LOGGER.trace(s"$logPrefix on ad[${acc0.mad.idOrNull}]: $mitem")
    val p = _PRED
    val rcvrId = mitem.rcvrIdOpt.get

    // Найти эдж, связанный с покупаемым узлом-ресивером:
    val medge1 = acc0.mad
      .edges
      .withNodePred(rcvrId, p)
      .toSeq
      .headOption
      .fold {
        // Создаём новый эдж
        MEdge(
          predicate = p,
          nodeId    = mitem.rcvrIdOpt.get,
          info      = MEdgeInfo(
            sls     = mitem.sls,
            billGids = Set(mitemId)
          )
        )
      } { medge0 =>
        // Обновляем существующий эдж.
        LOGGER.trace(s"$logPrefix Patching existing edge: $medge0")
        medge0.copy(
          info = medge0.info.copy(
            sls       = medge0.info.sls ++ mitem.sls,
            billGids  = medge0.info.billGids + mitemId
          )
        )
      }

    // Заливаем новый эдж в исходную карточку:
    val mad2 = acc0.mad.copy(
      edges = acc0.mad.edges.copy(
        out = {
          val iter1 = acc0.mad.edges.withoutNodePred(rcvrId, p)
          val iter2 = Iterator(medge1)
          MNodeEdges.edgesToMap1( iter1 ++ iter2 )
        }
      )
    )

    // Готовим апдейт полей текущего item'а.
    val dbAction = {
      val dateStart2 = DateTime.now()
      val dateEnd2 = dateStart2.plus( mitem.dtIntervalOpt.get.toPeriod )
      mItems.query
        .filter { _.id === mitemId }
        .map { i => (i.statusStr, i.dateStartOpt, i.dateEndOpt, i.dateStatus) }
        .update( (MItemStatuses.Finished.strId, Some(dateStart2), Some(dateEnd2), dateStart2) )
        .filter(_ == 1)
    }

    acc0.copy(
      mad       = mad2,
      dbActions = dbAction :: acc0.dbActions
    )
  }


  /** Логика снятия с прямого размещения живёт здесь. */
  def _uninstallDirect(mitem: MItem, reasonOpt: Option[String], acc0: Acc): Acc= {
    val mitemId = mitem.id.get

    lazy val logPrefix = s"_uninstallDirect($mitemId/${System.currentTimeMillis}):"
    LOGGER.trace(s"$logPrefix on ad[${acc0.mad.idOrNull}]: $mitem")

    val p = _PRED
    val rcvrId = mitem.rcvrIdOpt.get

    val mad2 = acc0.mad
      .edges
      // Ищем по mitemId
      .iterator
      .find( _.info.billGids.contains(mitemId) )
      .fold[MNode] {
        LOGGER.warn(s"$logPrefix receiver[$rcvrId] not found in mad, but expected. Already deleted?")
        // Нет эджа, хотя должен бы быть
        acc0.mad

      } { medge0 =>
        // Старый эдж на месте, чистим его...
        val sls2: Set[SinkShowLevel] = if (mitem.sls.isEmpty) {
          LOGGER.debug(s"$logPrefix mitem.sls is empty, uninstalling receiver totally")
          Set.empty
        } else {
          medge0.info.sls -- mitem.sls
        }

        val edges2: NodeEdgesMap_t = if (sls2.isEmpty) {
          LOGGER.trace(s"$logPrefix removing rcvr[$rcvrId], because sls now empty")
          val iter = acc0.mad.edges.withoutNodePred(rcvrId, p)
          MNodeEdges.edgesToMap1(iter)

        } else {
          LOGGER.trace(s"$logPrefix updating rcvr[$rcvrId] with sls=${sls2.mkString(",")}")
          val medge1 = medge0.copy(
            info = medge0.info.copy(
              sls = sls2,
              billGids = medge0.info.billGids - mitemId
            )
          )
          acc0.mad.edges.out ++ MNodeEdges.edgesToMapIter(medge1)
        }

        acc0.mad.copy(
          edges = acc0.mad.edges.copy(
            out = edges2
          )
        )
      }

    // Сборка экшена обновления item'а.
    val dbAction = {
      val now = DateTime.now()
      mItems.query
        .filter { _.id === mitem.id.get }
        .map { i => (i.statusStr, i.dateEndOpt, i.dateStatus, i.reasonOpt) }
        .update( (MItemStatuses.Finished.strId, Some(now), now, reasonOpt) )
    }

    acc0.copy(
      mad         = mad2,
      dbActions   = dbAction :: acc0.dbActions
    )
  }

  /** Деинсталляция прямого размещения из карточки. */
  override def uninstall(mitem: MItem, reasonOpt: Option[String]): IAdvBuilder = {
    if (mitem.iType == _ITYPE) {
      withAccUpdated { acc0 =>
        _uninstallDirect(mitem, reasonOpt, acc0)
      }

    } else {
      super.uninstall(mitem)
    }
  }

}
