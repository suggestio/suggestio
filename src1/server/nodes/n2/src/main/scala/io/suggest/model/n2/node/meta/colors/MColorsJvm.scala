package io.suggest.model.n2.node.meta.colors

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 16:34
 * Description: Модель цветов. Одно поле -- один цвет.
 * Цветов обычно всего лишь 2-3, и явно их не будет десятки.
 * Модель не анализируется со стороны ES вообще.
 */

object MColorsJvm {

  import MColorDataJvm.MCOLOR_DATA_FORMAT

  /** Поддержка JSON. */
  implicit val MCOLORS_FORMAT: OFormat[MColors] = (
    (__ \ MColorKeys.Bg.strId).formatNullable[MColorData] and
    (__ \ MColorKeys.Fg.strId).formatNullable[MColorData] and
    (__ \ MColorKeys.Pattern.strId).formatNullable[MColorData]
  )(MColors.apply, unlift(MColors.unapply))

}