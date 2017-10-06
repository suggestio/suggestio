package io.suggest.color

import boopickle.Default._
import io.suggest.common.html.HtmlConstants
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

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
      .inmap(apply, _.code)
  }

  implicit def univEq: UnivEq[MColorData] = UnivEq.derive


  def stripingDiez(colorCode: String): MColorData = {
    val cc2 = if (colorCode.startsWith( HtmlConstants.DIEZ )) {
      colorCode.replaceFirst( HtmlConstants.DIEZ, "" )
    } else {
      colorCode
    }
    MColorData(cc2)
  }

}


case class MColorData(
                       code: String
                       // TODO Добавить rgb: js.Object{r,g,b}. Добавить normed_frequency: Int = 0..10.
                     ) {

  def hexCode = HtmlConstants.DIEZ + code

}
