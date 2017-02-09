package io.suggest.init.routed

import io.suggest.primo.{IStrId, TypeT}
import enumeratum._
import JsInitConstants._


/** Трейт для всех инстансов модели. */
sealed trait MJsiTg extends EnumEntry with IStrId {
  override final def entryName = toString
  override final def strId = toString
}


/** Кросс-платформенная модель целей js-инициализации. */
object MJsiTgs extends Enum[MJsiTg] with TypeT {

  /** Активация всплывающих уведомлений. */
  case object Flashing extends MJsiTg {
    override def toString = ID_FLASHING
  }

  /** Вертикальная центровка вертикальных линий. */
  case object VCenterLines extends MJsiTg {
    override def toString = ID_VCENTER_LINES
  }

  /** js для формы внешнего размещения карточки. */
  case object LkAdvExtForm extends MJsiTg {
    override def toString = ID_LK_ADV_EXT_FORM
  }

  /** js-исполнитель внешнего размещения. */
  case object AdvExtRunner extends MJsiTg {
    override def toString = ID_ADV_EXT_RUNNER
  }

  /** Вертикальная центровка в ident. */
  case object IdentVCenterContent extends MJsiTg {
    override def toString = ID_IDENT_VCENTER_CONTENT
  }

  /** Форма редактирования узла в личном кабинете. */
  case object LkNodeEditForm extends MJsiTg {
    override def toString = ID_LK_NODE_EDIT_FORM
  }

  /** Обработать все .js-hidden элементы. */
  case object JsHidden extends MJsiTg {
    override def toString = ID_JS_HIDDEN
  }

  /** Инициализировать поддержку попапов. */
  case object Popups extends MJsiTg {
    override def toString = ID_POPUPS
  }

  /** init-target инициализации работы формы создания/редактирования рекламной карточки. */
  case object AdForm extends MJsiTg {
    override def toString = ID_AD_FORM
  }

  /** init-target для включения js к форме ввода капчи. */
  case object CaptchaForm extends MJsiTg {
    override def toString = ID_CAPTCHA_FORM
  }

  /** Цель инициализации для поддержки скрытой капчи, загружаемой и отображаемой опционально. */
  case object HiddenCaptcha extends MJsiTg {
    override def toString = ID_HIDDEN_CAPTCHA
  }

  /** Цель для инициализации страницы списка транзакций биллинга. */
  case object BillTxnsList extends MJsiTg {
    override def toString = ID_BILL_TXNS_LIST
  }

  /** Цель для инициализации страницы размещения в геотегах. */
  case object AdvGeoForm extends MJsiTg {
    override def toString = ADV_GEO_FORM
  }

  /** Цель инициализации формы прямого размещения карточки на узлах. */
  case object AdvDirectForm extends MJsiTg {
    override def toString = ADV_DIRECT_FORM
  }

  /** Цель инициализации формы размещения ADN-узла на географической карте. */
  case object AdnMapForm extends MJsiTg {
    override def toString = ADN_MAP_FORM
  }

  /** Цель инициализации формы управления узлами в ЛК узла. */
  case object LkNodesForm extends MJsiTg {
    override def toString = LK_NODES_FORM
  }


  override def values = findValues

}
