package models.mpay.yaka

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.17 16:31
  * Description: Модель экшенов яндекс-кассы.
  */
/** Класс одного элемента модели, т.е. экшена яндекс-кассы. */
sealed abstract class MYakaAction(override val value: String) extends StringEnumEntry {
  override final def toString = value
}


/** Модель экшенов яндекс-кассы. */
object MYakaActions extends StringEnum[MYakaAction] {

  /** Проверка без изменений в биллинги сервера. */
  case object Check extends MYakaAction("checkOrder")

  /** Проведение платёжа в биллинге s.io. */
  case object Payment extends MYakaAction("paymentAviso")

  override def values = findValues

}


/** Модель return-экшенов. Там только успехи и ошибки. */
object MYakaReturnActions extends StringEnum[MYakaAction] {

  /** Юзер возвращается в /success. */
  case object Success extends MYakaAction("PaymentSuccess")

  /** Юзера отправили в /fail. */
  case object Fail extends MYakaAction("PaymentFail")

  override def values = findValues


  implicit def mYakaActionQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MYakaAction] = {
    new QueryStringBindableImpl[MYakaAction] {

      override def bind(key: String, params: Map[String, Seq[String]]) = {
        for {
          strIdEith <- strB.bind(key, params)
        } yield {
          strIdEith.right.flatMap { strId =>
            withValueOpt(strId)
              .toRight("e.invalid.yaka.action")
          }
        }
      }

      override def unbind(key: String, yAction: MYakaAction) = {
        strB.unbind(key, yAction.value)
      }

    }
  }

}