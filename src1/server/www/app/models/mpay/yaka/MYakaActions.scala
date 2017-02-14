package models.mpay.yaka

import enumeratum._
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.primo.IStrId
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.17 16:31
  * Description: Модель экшенов яндекс-кассы.
  */
/** Класс одного элемента модели, т.е. экшена яндекс-кассы. */
sealed abstract class MYakaAction extends EnumEntry with IStrId {
  override def strId = toString
}

/** Модель экшенов яндекс-кассы. */
object MYakaActions extends Enum[MYakaAction] {

  /** Проверка без изменений в биллинги сервера. */
  case object Check extends MYakaAction {
    override def toString = "checkOrder"
  }

  /** Проведение платёжа в биллинге s.io. */
  case object Payment extends MYakaAction {
    override def toString = "paymentAviso"
  }

  override def values = findValues

}


/** Модель return-экшенов. Там только успехи и ошибки. */
object MYakaReturnActions extends Enum[MYakaAction] {

  /** Юзер возвращается в /success. */
  case object Success extends MYakaAction {
    override def toString = "PaymentSuccess"
  }

  /** Юзера отправили в /fail. */
  case object Fail extends MYakaAction {
    override def toString = "PaymentFail"
  }

  override def values = findValues


  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MYakaAction] = {
    new QueryStringBindableImpl[MYakaAction] {

      override def bind(key: String, params: Map[String, Seq[String]]) = {
        for {
          strIdEith <- strB.bind(key, params)
        } yield {
          strIdEith.right.flatMap { strId =>
            withNameOption(strId)
              .toRight("e.invalid.yaka.action")
          }
        }
      }

      override def unbind(key: String, value: MYakaAction) = {
        strB.unbind(key, value.strId)
      }

    }
  }

}