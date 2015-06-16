package io.suggest.sc.focus

import io.suggest.model.{LightEnumeration, ILightEnumeration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 18:36
 * Description: Заготовки для моделей режимов focused-рендера карточек.
 */
object FocusedRenderNames {

  /** id обычного focused-рендера (без элементов оформления producer'а. */
  def NORMAL_ID = "n"

  /** id полного рендера, включающего элементы оформления выдачи продьюсера. */
  def FULL_ID   = "f"

  // Названия полей для JSON-объектов focused-рендеров.
  /** Имя поля, содержащего строку HTML с рендером. */
  def HTML_FN = "h"

  /** Имя поля, содержащего метаднные по использованному режиму рендера. */
  def MODE_FN = "m"

  /** Имя поля с человеческим порядковым номером. */
  def INDEX_FN = "i"

}


import FocusedRenderNames._


/** Абстрактная enum-модель режимов focused-рендера. */
trait FocusedRenderModes extends ILightEnumeration {

  /** Трейт экземпляром модели. */
  protected trait ValT extends super.ValT {
    def strId: String
    override def toString = strId
  }

  override type T <: ValT

  /** Генерация инстансов. */
  protected def instance(strId: String): T

  /** Нормальный рендер, т.е. без producer-заголовка и оформления. */
  def Normal: T = instance(NORMAL_ID)

  /** Полный рендер: отрендерены все элементы оформления в рамках продьюсера. */
  def Full  : T = instance(FULL_ID)

}


/** Трейт облегченной реализации модели [[FocusedRenderModes]] для js с минимумом зависимостей. */
trait FocusedRenderModesLight extends FocusedRenderModes with LightEnumeration {

  /** Экземпляр модели. */
  sealed protected class Val(override val strId: String) extends ValT

  override type T = Val

  override protected def instance(strId: String): T = new Val(strId)

  override def maybeWithName(n: String): Option[T] = {
    if (n == NORMAL_ID) {
      Some(Normal)
    } else if (n == FULL_ID) {
      Some(Full)
    } else {
      None
    }
  }
}
