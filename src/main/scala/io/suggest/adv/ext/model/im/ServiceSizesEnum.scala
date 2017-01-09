package io.suggest.adv.ext.model.im

import io.suggest.common.geom.d2.INamedSize2di
import io.suggest.common.menum.{ILightEnumeration, EnumMaybeWithName, LightEnumeration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 10:28
 * Description:
 */

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
