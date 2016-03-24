package util.adv.build

import com.google.inject.assistedinject.Assisted
import com.google.inject.{ImplementedBy, Inject, Singleton}
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{IItem, IMItems, MItem, MItems}
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

  val accFut: Future[Acc]

  /** Очистить исходное состояние текущих услуг карточки.
    * Используется для рассчета состояния с нуля, вместо обновления существующего состояния.
    * @param full true Полная очистка, размещений.
    *             false Очищение строго в рамках полномочий того или иного билдера.
    */
  def clearAd(full: Boolean): IAdvBuilder = {
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

  protected[this] def _thisFut = Future.successful(this)

  protected[this] def throwUnsupported(mitem: IItem) = {
    throw new UnsupportedOperationException(s"${mitem.iType} is not supported by this builder")
  }


  /** Подготовка к развертыванию новых adv-item'ов.
    * Новых -- т.е. НЕ reinstall, а именно новых.
    * Так, гео-теги требуют заранее создать для них почву в виде узлов-тегов.
    *
    * @param mitems
    * @return
    */
  def prepareInstallNew(mitems: Iterable[MItem]): IAdvBuilder = {
    this
  }

  /** Установка услуги в карточку. */
  // TODO Нужно разбить install на sql-install и mad-install части. Так, при регулярно-используемом reinstall дропается sql-часть.
  def install(mitem: MItem): IAdvBuilder = {
    throwUnsupported(mitem)
  }

  /** Деинсталляция услуги из карточки. */
  def uninstall(mitem: MItem, reasonOpt: Option[String] = None): IAdvBuilder = {
    throwUnsupported(mitem)
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
