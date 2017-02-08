package io.suggest.init.routed

import io.suggest.common.menum.{EnumMaybeWithName, ILightEnumeration, LightEnumeration, StrIdValT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.15 11:01
 * Description: Шаблон для моделей js-init-целей.
 * Шаблоны в js передают данные для селективной инициализации через аттрибут в body.
 * js считывает спецификацию и производит инициализацию (вызов) указанных scala.js-модулей в заданном порядке.
 */
object JsInitConstants {

  /** Название аттрибута для тега body, куда записывается инфа для направленной инициализации. */
  def RI_ATTR_NAME = "data-ri"

  // Идентификаторы различных инициализаторов.
  /** Код запуска отображения flashing-уведомлений. */
  def ID_FLASHING                 = "a"
  /** Вертикальной центровки вертикальных линий верстки. */
  def ID_VCENTER_LINES            = "b"
  /** Форма размещения карточки на внешних сервисах. */
  def ID_LK_ADV_EXT_FORM          = "c"
  /** Исполнитель внешнего размещения в соц.сетях и других сервисах. */
  def ID_ADV_EXT_RUNNER           = "d"
  /** Вертикальная центровка содержимого колонок на ident-страницах. */
  def ID_IDENT_VCENTER_CONTENT    = "e"
  /** Форма редактирования узла в личном кабинете. */
  def ID_LK_NODE_EDIT_FORM        = "f"
  /** init-обработка .js-hidden элементов */
  def ID_JS_HIDDEN                = "g"
  /** Поддержка попапов. */
  def ID_POPUPS                   = "h"
  /** Поддержка формы создания/редактирования рекламных карточек. */
  def ID_AD_FORM                  = "i"
  /** Если на странице присутствует форма для ввода капчи, то нужно забиндить соотв.функции. */
  def ID_CAPTCHA_FORM             = "j"
  /** Есть скрытая капча на странице. */
  def ID_HIDDEN_CAPTCHA           = "k"
  /** id таргета js-инициализации страницы списка транзакций биллинга. */
  def ID_BILL_TXNS_LIST           = "l"
  /** id таргета инициализации страницы формой размещения в геотегах. */
  def ADV_GEO_FORM                = "m"
  /** id таргета инициализации формы прямого размещения карточки на узлах сети. */
  def ADV_DIRECT_FORM             = "n"
  /** id таргета формы размещения ADN-узла на географической карте. */
  def ADN_MAP_FORM                = "o"
  /** id таргета формы управления узлами в личном кабинете. */
  def LK_NODES_FORM               = "p"

}


import JsInitConstants._


/** Шаблон для сборки шаблонов enum-моделей и их последующих реалзаций. */
trait MInitTargetsBaseT extends ILightEnumeration with StrIdValT {

  /** Абстрактный экземпляр модели. */
  protected trait ValT extends super.ValT

  override type T <: ValT

  /** Сборка инстанса экземпляра модели. */
  protected def instance(strId: String): T

  /** Активация всплывающих уведомлений. */
  val Flashing: T = instance(ID_FLASHING)

  /** Вертикальная центровка вертикальных линий. */
  val VCenterLines: T = instance(ID_VCENTER_LINES)

  /** js для формы внешнего размещения карточки. */
  val LkAdvExtForm: T = instance(ID_LK_ADV_EXT_FORM)

  /** js-исполнитель внешнего размещения. */
  val AdvExtRunner: T = instance(ID_ADV_EXT_RUNNER)

  /** Вертикальная центровка в ident. */
  val IdentVCenterContent: T = instance(ID_IDENT_VCENTER_CONTENT)

  /** Форма редактирования узла в личном кабинете. */
  val LkNodeEditForm: T = instance(ID_LK_NODE_EDIT_FORM)

  /** Обработать все .js-hidden элементы. */
  val JsHidden: T = instance(ID_JS_HIDDEN)

  /** Инициализировать поддержку попапов. */
  val Popups: T = instance(ID_POPUPS)

  /** init-target инициализации работы формы создания/редактирования рекламной карточки. */
  val AdForm: T = instance(ID_AD_FORM)

  /** init-target для включения js к форме ввода капчи. */
  val CaptchaForm: T = instance(ID_CAPTCHA_FORM)

  /** Цель инициализации для поддержки скрытой капчи, загружаемой и отображаемой опционально. */
  val HiddenCaptcha: T = instance(ID_HIDDEN_CAPTCHA)

  /** Цель для инициализации страницы списка транзакций биллинга. */
  val BillTxnsList: T = instance(ID_BILL_TXNS_LIST)

  /** Цель для инициализации страницы размещения в геотегах. */
  val AdvGeoForm: T = instance(ADV_GEO_FORM)

  /** Цель инициализации формы прямого размещения карточки на узлах. */
  val AdvDirectForm: T = instance(ADV_DIRECT_FORM)

  /** Цель инициализации формы размещения ADN-узла на географической карте. */
  val AdnMapForm: T    = instance(ADN_MAP_FORM)

  /** Цель инициализации формы управления узлами в ЛК узла. */
  val LkNodesForm: T   = instance(LK_NODES_FORM)

}


/** Заготовка для scala.Enumeration-реализации модели. */
trait MJsInitTargetsT extends Enumeration with MInitTargetsBaseT with EnumMaybeWithName {
  /** Экземпляр модели. */
  // TODO Когда элементов станет много, лучше будет заюзать аккамулятор + карту вместо Enumeration. Но это будет на стороне реализации, не здесь.
  protected sealed class Val(val strId: String)
    extends super.Val(strId)
    with ValT

  override type T = Val
  override protected def instance(strId: String): T = {
    new Val(strId)
  }
}


/** Реализация модели для sjs без использования Set и прочих Enumeration. */
trait MJsInitTargetsLigthT extends MInitTargetsBaseT with LightEnumeration {
  /** Экземпляр модели. */
  protected sealed class Val(val strId: String) extends ValT

  override type T = Val
  override protected def instance(strId: String): T = {
    new Val(strId)
  }

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Flashing.strId             => Some(Flashing)
      case VCenterLines.strId         => Some(VCenterLines)
      case LkAdvExtForm.strId         => Some(LkAdvExtForm)
      case AdvExtRunner.strId         => Some(AdvExtRunner)
      case IdentVCenterContent.strId  => Some(IdentVCenterContent)
      case LkNodeEditForm.strId       => Some(LkNodeEditForm)
      case AdForm.strId               => Some(AdForm)
      case CaptchaForm.strId          => Some(CaptchaForm)
      case HiddenCaptcha.strId        => Some(HiddenCaptcha)
      case BillTxnsList.strId         => Some(BillTxnsList)
      case AdvGeoForm.strId           => Some(AdvGeoForm)
      case AdvDirectForm.strId        => Some(AdvDirectForm)
      case AdnMapForm.strId           => Some(AdnMapForm)
      case LkNodesForm.strId          => Some(LkNodesForm)
      case _                          => None
    }
  }
}

