package io.suggest.geo

import org.scalajs.dom.PositionError

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.17 18:20
  * Description: Модель исключения геопозиционирования.
  */
case class PositionException(code: Int, message: String) extends RuntimeException

object PositionException {

  /** Сборка экзепшена на основе нативной браузерной ошибки позиционирования.
    *
    * @param pe Экземпляр dom.PositionError.
    * @return Инстанс [[PositionException]].
    */
  def apply(pe: PositionError): PositionException = {
    PositionException(
      code    = pe.code,
      message = pe.message
    )
  }

}
