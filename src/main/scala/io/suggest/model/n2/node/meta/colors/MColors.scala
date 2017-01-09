package io.suggest.model.n2.node.meta.colors

import io.suggest.common.empty.{IEmpty, EmptyProduct}
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

object MColors extends IEmpty {

  override type T = MColors

  override val empty: MColors = {
    new MColors() {
      override def nonEmpty = false
    }
  }

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MColors] = (
    (__ \ MColorKeys.Bg.strId).formatNullable[MColorData] and
    (__ \ MColorKeys.Fg.strId).formatNullable[MColorData] and
    (__ \ MColorKeys.Pattern.strId).formatNullable[MColorData]
  )(apply, unlift(unapply))

}


case class MColors(
  bg        : Option[MColorData]    = None,
  fg        : Option[MColorData]    = None,
  pattern   : Option[MColorData]    = None
)
  extends EmptyProduct
