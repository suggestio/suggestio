package io.suggest.adv.ext.model.ctx

import io.suggest.model.{LightEnumeration, ILightEnumeration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 14:53
 * Description: Модель допустимых режимов загрузки картинки в хранилища внешних сервисов.
 */
trait MPicUploadModesT extends ILightEnumeration {
  
  protected trait ValT extends super.ValT {
    val jsName: String
  }

  override type T <: ValT

  val S2s: T

}


/** Заготовка легковесной реализации модели без коллекций. */
trait MPicUploadModesLightT extends MPicUploadModesT with LightEnumeration {
  override def maybeWithName(n: String): Option[T] = {
    n match {
      case S2s.jsName => Some(S2s)
      case _          => None
    }
  }
}
