package io.suggest.common.empty

import io.suggest.primo.TypeT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.16 15:48
 * Description: Интерфейс для поля, возвращающего пустые экземпляры моделей.
 */
trait IEmpty extends TypeT {

  def empty: T

}
