package io.suggest.model.n2.node.meta.colors

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.17 15:16
  * Description: Пошаренная модель данных по одному цвету.
  */

object MColorData {

  /** Название поля с кодом цвета. Обычно цвет задан как RGB. */
  val CODE_FN = "c"

  /** Поддержка boopickle. */
  implicit val mColorDataPickler: Pickler[MColorData] = {
    generatePickler[MColorData]
  }

}


case class MColorData(
  code: String
)
