package io.suggest.adn.edit

import io.suggest.common.geom.d2.MSize2di
import io.suggest.math.MathConst
import scalaz.ValidationNel

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 14:59
 * Description: Константы редактора ADN-узла.
 */
object NodeEditConstants {


  object Name {
    def LEN_MAX = 128
    // 0 - допускать безымянные узлы.
    def LEN_MIN = 0

    /** Валидация названия узла. */
    def validateNodeName(raw: String): ValidationNel[String, String] = {
      val name = raw.trim
      val ePrefix = "e.node.name.len.too"
      MathConst.Counts
        .validateMinMax(name.length, min = LEN_MIN, max = LEN_MAX, ePrefix)
        .map(_ => name)
    }

  }

  /** id контейнера для кнопки сохранения. */
  def SAVE_BTN_CONTAINER_ID = "aer"

  /** id контейнера для формы. */
  def FORM_CONTAINER_ID = "aec"


  /** Данные галереи узла. */
  object Gallery {

    def WIDTH_PX  = 625
    def HEIGHT_PX = 200

    def WH_PX = MSize2di(width = WIDTH_PX, height = HEIGHT_PX)

  }

}
