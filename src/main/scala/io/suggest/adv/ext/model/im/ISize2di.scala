package io.suggest.adv.ext.model.im

import io.suggest.model.{EnumMaybeWithName, LightEnumeration, ILightEnumeration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 15:58
 * Description: Размер двумерный целочисленный.
 */
trait ISize2di {
  def width: Int
  def height: Int

  def sizeWhEquals(sz1: ISize2di): Boolean = {
    sz1.width == width  &&  sz1.height == height
  }

  override def toString: String = "Sz2D(w=" + width + ";h=" + height + ")"
}


/** Именованая версия [[ISize2di]]. Полезно для enum'ов.
  * Позволяет задать допустимый размер строковым алиасом. */
trait INamedSize2di extends ISize2di {

  /** Алиас (название) размера. */
  def szAlias: String

  override def toString = "Sz2D(" + szAlias + ",w=" + width + ";h=" + height + ")"
}


/** Базовые возможности поиска по размерам.
  * Нужно для объединения моделей размеров от разных сервисов. */
trait ServiceSizesEnumBaseT extends ILightEnumeration {

  protected trait ValT extends super.ValT with INamedSize2di

  override type T <: ValT

  // Сюда можно добавить интерфейс логики, специфичный для size-model'ей.
}


/** mix-in для сборки light-enum'ов на базе шаблона модели [[ServiceSizesEnumBaseT]]. */
trait ServiceSizesEnumLightT extends ServiceSizesEnumBaseT with LightEnumeration {

  // Чтобы можно было переопределять без геморроя. Без override для надежности.
  def maybeWithName(n: String): Option[T] = None
}


trait ServiceSizesEnumScalaT extends EnumMaybeWithName with ServiceSizesEnumBaseT {
  /** Интерфейс экземпляра модели. */
  protected trait ValT extends super.ValT {
    override val szAlias: String
  }

  /** Абстрактный экземпляр модели. */
  abstract protected class Val(val szAlias: String)
    extends super.Val(szAlias)
    with ValT

  override type T = Val

}
