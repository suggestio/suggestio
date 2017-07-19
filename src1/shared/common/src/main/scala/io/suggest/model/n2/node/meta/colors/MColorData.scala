package io.suggest.model.n2.node.meta.colors

import boopickle.Default._
import io.suggest.common.html.HtmlConstants
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

  /** Поддержка JSON. */
  implicit val MCOLOR_DATA_FORMAT: OFormat[MColorData] = {
    (__ \ MColorData.CODE_FN).format[String]
      .inmap(MColorData.apply, _.code)
  }

}


case class MColorData(
  code: String
) {

  def hexCode = HtmlConstants.DIEZ + code

}
