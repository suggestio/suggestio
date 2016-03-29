package util.adv.build

import com.google.inject.assistedinject.Assisted
import com.google.inject.{ImplementedBy, Inject, Singleton}
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{IMItems, MItem, MItems}
import models.adv.build.Acc
import models.mproj.{ICommonDi, IMCommonDi}
import org.joda.time.DateTime
import util.adv.direct.AdvDirectBuilder
import util.adv.geo.tag.AgtBuilder
import util.n2u.{IN2NodesUtilDi, N2NodesUtil}
import util.{PlayMacroLogsI, PlayMacroLogsImpl}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.16 18:46
  * Description: v2-биллинг с полиморфизмом разновидностей размещений и действий по ним привели
  * к необходимости серьезного упрощения и расширения возможностей API.
  *
  * Был придуман механизм билдеров для сброки комплекных эффектов
  * размещения, снятия с размещения, пересборки размещения и дальнейшего применения оных.
  */

/** Трейт для guice-фабрики инстансов [[IAdvBuilder]]. */
trait AdvBuilderFactory {
  /** Вернуть инстанс билдера, готового к работе. */
  def builder(acc0Fut: Future[Acc], now: DateTime): IAdvBuilder
}

/** Интерфейс для DI-поля с инстансом [[AdvBuilderFactory]]. */
trait AdvBuilderFactoryDi {
  def advBuilderFactory: AdvBuilderFactory
}


/** Интерфейс adv-билдеров. Они все очень похожи. */
@ImplementedBy( classOf[AdvBuilder] )
trait IAdvBuilder
  extends PlayMacroLogsI
{
  val di: AdvBuilderDi

  import di._
  import mCommonDi._
  import slick.driver.api._

  /** Аккамулятор результатов.
    * Используется для доступа к результатам работы билдера.
    */
  val accFut: Future[Acc]

  /** Очистить исходное состояние текущих услуг карточки.
    * Используется для рассчета состояния с нуля, вместо обновления существующего состояния.
    *
    * @param full true Полная очистка, размещений.
    *             false Очищение строго в рамках полномочий того или иного билдера.
    */
  def clearAd(full: Boolean = false): IAdvBuilder = {
    this
  }

  /** Текущее время. Для унификации выставляемых дат. */
  def now: DateTime

  /** Сюда закидываются типы item'ов, поддерживаемых этим билдером,
    * в любом порядке, желательно через "::" . */
  def supportedItemTypes: List[MItemType] = Nil

  def supportedItemTypesStrSet = supportedItemTypes.iterator.map(_.strId).toSet

  /** Финализировать все размещения карточки по базе биллинга. */
  def finalizeBilling(statuses: MItemStatus*): IAdvBuilder = {
    withAccUpdated { acc0 =>
      val _now = now
      val supItmTypesStr = supportedItemTypes
        .iterator
        .map(_.strId)
        .toSet
      val statusesStr = statuses
        .iterator
        .map(_.strId)
        .toSet
      val dbAction = mItems.query
        .filter { i =>
          (i.adId === acc0.mad.id.get) &&
            (i.statusStr inSet statusesStr) &&
            (i.iTypeStr inSet supItmTypesStr)
        }
        .map { i =>
          (i.status, i.dateEndOpt, i.dateStatus)
        }
        .update( (MItemStatuses.Finished, Some(_now), _now) )
      acc0.copy(
        dbActions = dbAction :: acc0.dbActions
      )
    }
  }


  /** Доступ к copy() с новым инстансом madFut. */
  def withAcc(accFut2: Future[Acc]): IAdvBuilder

  def withAccUpdated(f: Acc => Acc): IAdvBuilder = {
    val acc2Fut = accFut.map(f)
    withAcc(acc2Fut)
  }

  def withAccUpdatedFut(f: Acc => Future[Acc]): IAdvBuilder = {
    val acc2Fut = accFut.flatMap(f)
    withAcc(acc2Fut)
  }


  // API v2
  // Теперь install разделен на sql- и mad-части, а API на вход принимает item'ы оптом, а не поштучно.
  // Да и основной API теперь внезапно стало синхронным. Для подготовки каких-то асинхронных данных теперь нужно
  // запиливать beforeInstall/beforeUnistall. Просто по итогда API v1 необходимость в этом отпала сама собой.

  def installSql(items: Iterable[MItem]): IAdvBuilder = {

    val itypes = supportedItemTypes
    val (ditems, others) = items.partition { i =>
      itypes.contains(i.iType)
    }

    lazy val logPrefix = s"installSql(${items.size}):"

    // Если есть неизвестные item'ы, то ругаемся в логи и пропускаем их.
    if (others.nonEmpty)
      _logUnsupportedItems(logPrefix, others)

    // Собираем db-экшены для инсталляции
    if (ditems.nonEmpty) {
      LOGGER.trace(s"$logPrefix There are ${ditems.size} for install...")
      withAccUpdated { acc0 =>
        val dbas1 = ditems.foldLeft(acc0.dbActions) { (dbas0, mitem) =>
          val dbAction = {
            val dateStart2 = now
            val dateEnd2 = dateStart2.plus( mitem.dtIntervalOpt.get.toPeriod )
            val mitemId = mitem.id.get
            mItems.query
              .filter { _.id === mitemId }
              .map { i => (i.status, i.dateStartOpt, i.dateEndOpt, i.dateStatus) }
              .update( (MItemStatuses.Online, Some(dateStart2), Some(dateEnd2), dateStart2) )
              .filter { rowsUpdated =>
                LOGGER.trace(s"$logPrefix Updated item[$mitemId]: dateEnd => $dateEnd2")
                rowsUpdated == 1
              }
          }
          dbAction :: dbas0
        }
        acc0.copy(
          dbActions = dbas1
        )
      }

    } else {
      this
    }
  }

  def installNode(items: Iterable[MItem]): IAdvBuilder = {
    if (items.nonEmpty)
      _logUnsupportedItems("installNode()", items)
    this
  }

  private def _logUnsupportedItems(logPrefix: String, items: Traversable[MItem]): Unit = {
    LOGGER.error(s"$logPrefix ${items.size} items have unsupported types, they are skipped:${items.mkString("\n", ",\n", "")}")
  }

  def unInstallSql(items: Iterable[MItem], reasonOpt: Option[String] = None): IAdvBuilder = {
    val itypes = supportedItemTypes
    val (tagItems, others) = items.partition { i =>
      itypes.contains( i.iType )
    }

    lazy val logPrefix = s"uninstallSql(${items.size}):"
    if (others.nonEmpty)
      _logUnsupportedItems(logPrefix, others)

    // Собрать изменения для БД
    if (tagItems.nonEmpty) {
      val _now = now
      val itemIds = tagItems.iterator.flatMap(_.id).toSet
      LOGGER.trace(s"$logPrefix Generating unInstall SQL for ${itemIds.size} items: ${itemIds.mkString(",")}, now = ${_now}")
      val dbAction = mItems.query
        .filter(_.id inSet itemIds)
        .map { i =>
          (i.status, i.dateEndOpt, i.dateStatus, i.reasonOpt)
        }
        .update((MItemStatuses.Finished, Some(_now), _now, reasonOpt))
        .filter { rowsUpdated =>
          val itemIdsLen = itemIds.size
          val r = rowsUpdated == itemIdsLen
          if (!r)
            LOGGER.warn(s"$logPrefix Unexpected rows deleted: $rowsUpdated, expected $itemIdsLen")
          r
        }

      // Собрать новый акк.
      withAccUpdated { acc0 =>
        acc0.copy(
          dbActions = dbAction :: acc0.dbActions
        )
      }

    } else {
      this
    }
  }

}


/** Контейнер для DI-аргументов вынесен за пределы билдера для ускорения и упрощения ряда вещей. */
@Singleton
class AdvBuilderDi @Inject() (
  override val n2NodesUtil        : N2NodesUtil,
  override val mItems             : MItems,
  override val mCommonDi          : ICommonDi
)
  extends IN2NodesUtilDi
  with IMCommonDi
  with IMItems
{

  import mCommonDi.configuration

  /** Создавать ли узлы-теги для геотегов?
    * Изначально они создавались, но особо были нужны.
    */
  val AGT_CREATE_TAG_NODES: Boolean = configuration.getBoolean("adv.geo.tag.create.nodes").getOrElse(false)

}


/** Финальная реализация [[IAdvBuilder]]. */
case class AdvBuilder @Inject() (
  @Assisted override val accFut   : Future[Acc],
  @Assisted override val now      : DateTime,
  override val di                 : AdvBuilderDi
)
  extends AdvDirectBuilder
  with AgtBuilder
  with PlayMacroLogsImpl
{

  override def withAcc(accFut2: Future[Acc]): IAdvBuilder = {
    copy(accFut = accFut2)
  }
}
