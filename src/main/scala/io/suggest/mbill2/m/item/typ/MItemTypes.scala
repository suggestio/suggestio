package io.suggest.mbill2.m.item.typ

import io.suggest.common.menum.{EnumApply, EnumMaybeWithName}
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:50
 * Description: Статическая модель типов item'ов заказа.
 */
object MItemTypes extends EnumMaybeWithName with EnumApply {

  /** Класс элементов модели. */
  protected[this] class Val(override val strId: String)
    extends super.Val(strId)
    with ValT
  {
    /** Новый статус item'а после оплаты заказа. */
    def orderClosedStatus   : MItemStatus = MItemStatuses.AwaitingMdr

    /** Какой статус выставлять item'у после получения оплаты? */
    def sioApprovedStatus   : MItemStatus = MItemStatuses.Offline

    /** Название по каталогу локализованных названий. */
    def nameI18n: String = {
      "Item.type." + strId
    }

    /** Является ли ресивером денег CBCA?
      * Для рекламных размещений внутри suggest.io -- она.
      * Для прочих возможных сделок -- нужно анализировать содержимое MItem.rcvrIdOpt.
      */
    def moneyRcvrIsCbca: Boolean = true

  }

  override type T = Val

  /**
    * Прямое размещение карточки прямо на каком-либо узле (используя id узла).
    * Это было самый первый тип размещения в suggest.io.
    * Скорее всего, этот же тип будет для размещения в маячках и группах маячков.
    */
  val AdvDirect         : T = new Val("a")

  /** Заказ геотеггинга для карточки. Размещение по шейпу и id узла-тега-ресивера. */
  val GeoTag            : T = new Val("t")

  /** Покупка размещения в каком-то месте на карте: по геошейпу без ресиверов. */
  val GeoPlace          : T = new Val("g")

  /** Размещение ADN-узла (магазина/ТЦ/etc) на карте. */
  val AdnNodeMap        : T = new Val("m")

  /** Плата за активность маячка в системе s.io.
    * Неоплаченный маячок "не работает", хотя и засоряет эфир своими сигналами. */
  val BleBeaconActive   : T = new Val("b")

  /** Покупка срочного доступа к внешнему размещению (разовая абонплата). */
  //val AdvExtFee         : T = new Val("e")


  /** Типы, относящиеся к рекламным размещениям. */
  def advTypes                = advGeoTypes reverse_::: advDirectTypes
  def advTypesIds             = onlyIds( advTypes )

  /** Только типы item'ов, относящиеся к гео-размещениям. */
  def advGeoTypes             = GeoTag :: GeoPlace :: Nil
  def advGeoTypeIds           = onlyIds( advGeoTypes )

  def advDirectTypes          = AdvDirect :: Nil


  /** Поддержка маппинга для play router. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[T] = {
    new QueryStringBindableImpl[T] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for {
          maybeStrId <- strB.bind(key, params)
        } yield {
          maybeStrId.right.flatMap { strId =>
            maybeWithName(strId)
              .toRight("error.invalid")
          }
        }
      }
      override def unbind(key: String, value: T): String = {
        strB.unbind(key, value.strId)
      }
    }
  }

}



