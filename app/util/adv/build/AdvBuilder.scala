package util.adv.build

import com.google.inject.{Inject, Singleton, ImplementedBy}
import com.google.inject.assistedinject.Assisted
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{MItems, IMItems, IItem, MItem}
import io.suggest.model.n2.edge.{NodeEdgesMap_t, MNodeEdges}
import models.MPredicate
import models.adv.build.Acc
import models.mproj.{ICommonDi, IMCommonDi}
import org.joda.time.DateTime
import util.adv.direct.AdvDirectBuilder
import util.adv.geo.tag.AdvGeoTagBuilder
import util.{PlayMacroLogsDyn, PlayMacroLogsImpl, PlayMacroLogsI}
import util.n2u.{IN2NodesUtilDi, N2NodesUtil}

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
  def builder(acc0Fut: Future[Acc]): IAdvBuilder
}

/** Интерфейс для DI-поля с инстансом [[AdvBuilderFactory]]. */
trait AdvBuilderFactoryDi {
  def advBuilderFactory: AdvBuilderFactory
}


class AdvBuilderUtil extends PlayMacroLogsDyn {

  /**
    * Вспомогательный метод для удаления эджа из карты эджей на основе данных биллинга.
    *
    * @param edges Исходная карта эджей.
    * @param mitem Текущий item биллинга.
    * @param pred Ожидаемый предикат эджа.
    * @return Пропатченная карточка.
    */
  def uninstallEdge(edges: NodeEdgesMap_t, mitem: MItem, pred: MPredicate): NodeEdgesMap_t = {
    val mitemId = mitem.id.get
    lazy val logPrefix = s"_uninstallEdgeFrom(item[$mitemId]):"
    val iter = edges
      .valuesIterator
      .filter { e =>
        val isRemove = e.info.billGids.contains( mitemId )

        // Записать в логи грядущее стирание эджа.
        if (isRemove)
          LOGGER.trace(s"$logPrefix uninstalling edge: $e")

        // Для самоконтроля: проверяем остальные поля удаляемого эджа по данным биллинга
        if (isRemove && !(e.predicate == pred && mitem.rcvrIdOpt.contains( e.nodeId )))
          LOGGER.error(s"_uninstallEdgeFrom(item[$mitemId]): Erasing unexpected edge using info.billGid: $e, rcvrId must == ${mitem.rcvrIdOpt}, pred == $pred")

        !isRemove
      }
    MNodeEdges.edgesToMap1( iter )
  }

  def throwUnsupported(mitem: IItem) = {
    throw new UnsupportedOperationException(s"${mitem.iType} is not supported by this builder")
  }

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

  val accFut: Future[Acc]

  /** Очистить исходное состояние текущих услуг карточки.
    * Используется для рассчета состояния с нуля, вместо обновления существующего состояния. */
  def clearAd(): IAdvBuilder = {
    this
  }

  /** Сюда закидываются типы item'ов, поддерживаемых этим билдером,
    * в любом порядке, желательно через "::" . */
  def supportedItemTypes: List[MItemType] = Nil

  def supportedItemTypesStrSet = supportedItemTypes.iterator.map(_.strId).toSet

  /** Финализировать все размещения карточки по базе биллинга. */
  def finalizeBilling(statuses: MItemStatus*): IAdvBuilder = {
    withAccUpdated { acc0 =>
      val now = DateTime.now()
      val supItmTypesStr = supportedItemTypes.iterator.map(_.strId).toSet
      val statusesStr = statuses.iterator.map(_.strId).toSet
      val dbAction = mItems.query
        .filter { i =>
          (i.adId === acc0.mad.id.get) &&
            (i.statusStr inSet statusesStr) &&
            (i.iTypeStr inSet supItmTypesStr)
        }
        .map { i =>
          (i.status, i.dateEndOpt, i.dateStatus)
        }
        .update( (MItemStatuses.Finished, Some(now), now) )
      acc0.copy(
        dbActions = dbAction :: acc0.dbActions
      )
    }
  }

  protected[this] def _thisFut = Future.successful(this)

  /** Установка услуги в карточку. */
  def install(mitem: MItem): IAdvBuilder = {
    util.throwUnsupported(mitem)
  }

  /** Деинсталляция услуги из карточки. */
  def uninstall(mitem: MItem, reasonOpt: Option[String] = None): IAdvBuilder = {
    util.throwUnsupported(mitem)
  }

  /** Доступ к copy() с новым инстансом madFut. */
  def withAcc(accFut2: Future[Acc]): IAdvBuilder

  protected[this] def withAccUpdated(f: Acc => Acc): IAdvBuilder = {
    val acc2Fut = accFut.map(f)
    withAcc(acc2Fut)
  }

  protected[this] def withAccUpdatedFut(f: Acc => Future[Acc]): IAdvBuilder = {
    val acc2Fut = accFut.flatMap(f)
    withAcc(acc2Fut)
  }

}


/** Контейнер для DI-аргументов вынесен за пределы билдера для ускорения и упрощения ряда вещей. */
@Singleton
class AdvBuilderDi @Inject() (
  override val n2NodesUtil        : N2NodesUtil,
  override val mItems             : MItems,
  val util                        : AdvBuilderUtil,
  override val mCommonDi          : ICommonDi
)
  extends IN2NodesUtilDi
  with IMCommonDi
  with IMItems


/** Финальная реализация [[IAdvBuilder]]. */
case class AdvBuilder @Inject() (
  @Assisted override val accFut   : Future[Acc],
  override val di                 : AdvBuilderDi
)
  extends AdvDirectBuilder
  with AdvGeoTagBuilder
  with PlayMacroLogsImpl
{

  override def withAcc(accFut2: Future[Acc]): IAdvBuilder = {
    copy(accFut = accFut2)
  }
}
