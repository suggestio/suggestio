package util.adv.direct

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.model.n2.edge._
import models.adv.build.Acc
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
  import slick.driver.api._

  private def _PRED   = MPredicates.Receiver.AdvDirect
  private def _ITYPE  = MItemTypes.AdvDirect

  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  /** Очистить все прямые размещения карточки по биллингу.
    * Используется для рассчета состояния с нуля, вместо обновления существующего состояния.
    *
    * @param full true -- Вычистить всех ресиверов в т.ч. саморазмещение.
    *             false -- Вычистить только платных ресиверов.
    */
  override def clearAd(full: Boolean): IAdvBuilder = {
    val accFut2 = for {
      acc0 <- super.clearAd(full).accFut
    } yield {
      acc0.copy(
        mad = acc0.mad.copy(
          edges = acc0.mad.edges.copy(
            out = {
              // Полная чистка удаляет всех ресиверов. Обычная -- касается только AdvDirect.
              val p = if (full) MPredicates.Receiver else _PRED
              // Собрать новую карту эджей.
              MNodeEdges.edgesToMap1(
                acc0.mad.edges
                  .withoutPredicateIter(p)
              )
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
    lazy val logPrefix = s"_installDirect($mitemId ${System.currentTimeMillis}):"

    // Это инсталляция услуги прямого размещения на [оплаченном] узле.
    LOGGER.trace(s"$logPrefix on ad[${acc0.mad.idOrNull}]: $mitem")
    val p = _PRED
    val rcvrId = mitem.rcvrIdOpt.get

    // Найти эдж, связанный с покупаемым узлом-ресивером:
    val edges0 = acc0.mad.edges
    val edges2iter = edges0
      .withNodePred(rcvrId, p)
      .toSeq
      .headOption
      .fold [Iterator[MEdge]] {
        // Создаём новый эдж
        val e = MEdge(
          predicate = p,
          nodeIdOpt = mitem.rcvrIdOpt,
          info      = MEdgeInfo(
            sls     = mitem.sls,
            itemIds = Set(mitemId)    // TODO Подсказка для возможной отладки процесса, потом удалить полностью.
          )
        )
        // Дописать новый эдж к общей куче.
        edges0.iterator ++ Iterator(e)

      } { medge0 =>
        // Обновляем существующий эдж.
        LOGGER.trace(s"$logPrefix Patching existing edge: $medge0")
        val e = medge0.copy(
          info = medge0.info.copy(
            sls       = medge0.info.sls ++ mitem.sls,
            itemIds   = medge0.info.itemIds + mitemId   // TODO Подсказка для возможной отладки процесса, потом удалить полностью.
          )
        )
        // Отфильтровать старый эдж, добавить новый.
        edges0.withoutNodePred(rcvrId, p) ++ Iterator(e)
      }

    // Заливаем новый эдж в исходную карточку:
    val mad2 = acc0.mad.copy(
      edges = edges0.copy(
        out = MNodeEdges.edgesToMap1( edges2iter )
      )
    )

    // Готовим апдейт полей текущего item'а.
    val dbAction = {
      val dateStart2 = now
      val dateEnd2 = dateStart2.plus( mitem.dtIntervalOpt.get.toPeriod )
      mItems.query
        .filter { _.id === mitemId }
        .map { i => (i.status, i.dateStartOpt, i.dateEndOpt, i.dateStatus) }
        .update( (MItemStatuses.Online, Some(dateStart2), Some(dateEnd2), dateStart2) )
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

    lazy val logPrefix = s"_uninstallDirect($mitemId ${System.currentTimeMillis}):"
    LOGGER.trace(s"$logPrefix on ad[${acc0.mad.idOrNull}]: $mitem")

    // 2016.mar.24: Удалёна поддержка обновления mad отсюда, т.к. теперь при uninstall карточка пересобирается с помощью install.

    // Сборка экшена обновления item'а.
    val dbAction = {
      val _now = now
      mItems.query
        .filter { _.id === mitemId }
        .map { i => (i.status, i.dateEndOpt, i.dateStatus, i.reasonOpt) }
        .update( (MItemStatuses.Finished, Some(_now), _now, reasonOpt) )
    }

    acc0.copy(
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
