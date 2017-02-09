package models.mpay.yaka

import enumeratum._
import io.suggest.primo.IStrId

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
