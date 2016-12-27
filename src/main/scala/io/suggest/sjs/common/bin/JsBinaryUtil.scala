package io.suggest.sjs.common.bin

import scala.scalajs.js.typedarray.Uint8Array

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.12.16 17:07
  * Description: JS-специфичная утиль для работы с бинарщиной.
  */
object JsBinaryUtil {

  /**
    * Interpret byte buffer as unsigned little endian 8 bit integer.
    * @param data Массив [байт] исходный.
    * @param offset индекс байта.
    * @return Целое беззнаковое.
    */
  def littleEndianToUint8(data: Uint8Array, offset: Int): Int = {
    data(offset)
  }

}
