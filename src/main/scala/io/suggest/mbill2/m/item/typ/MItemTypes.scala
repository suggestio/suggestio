package io.suggest.mbill2.m.item.typ

import io.suggest.common.menum.{EnumApply, EnumMaybeWithName}
import io.suggest.mbill2.m.item.status.{MItemStatuses, MItemStatus}
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
    def orderClosedStatus   : MItemStatus = MItemStatuses.AwaitingSioAuto

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

  /** Прямое размещение карточки на каком-то узле.
    * Это было самый первый тип размещения в suggest.io. */
  val AdvDirect         : T = new Val("a")

  /** Заказ геотеггинга для карточки. */
  val GeoTag            : T = new Val("t")

  /** Покупка срочного доступа к внешнему размещению (разовая абонплата). */
  val AdvExtFee         : T = new Val("e")


  /** Типы, относящиеся к рекламным размещениям. */
  val onlyAdvTypes    = Set(AdvDirect, GeoTag)
  def onlyAdvTypesIds = onlyAdvTypes.map(_.strId)


  /** Поддержка маппинга для play router. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[T] = {
    new QueryStringBindable[T] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for {
          maybeStrId <- strB.bind(key, params)
        } yield {
          maybeStrId.right
            .flatMap { strId =>
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



