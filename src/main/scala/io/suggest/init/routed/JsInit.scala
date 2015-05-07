package io.suggest.init.routed

import io.suggest.model.{EnumMaybeWithName, LightEnumeration, ILightEnumeration}

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

}


import JsInitConstants._


/** Шаблон для сборки шаблонов enum-моделей и их последующих реалзаций. */
trait MInitTargetsBaseT extends ILightEnumeration {

  /** Абстрактный экземпляр модели. */
  protected trait ValT extends super.ValT {
    /** Строковой идентификатор из [[JsInitConstants]]. */
    def strId: String
    override def toString: String = strId
  }

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

  // TODO Надо не забывать добавлять новые элементы в MInitTargetLightT.maybeWithName().
}


/** Заготовка для scala.Enumeration-реализации модели. */
trait MJsInitTargetsT extends Enumeration with MInitTargetsBaseT with EnumMaybeWithName {
  /** Экземпляр модели. */
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
      case _                          => None
    }
  }
}

