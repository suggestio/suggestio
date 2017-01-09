package io.suggest.model.n2.node.meta.colors

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 16:29
 * Description: Инфа о цвете представляется этой моделью.
 * В прошлой архитектуре, сохранялся только цвет кода, например "FFFFFF" и больше ничего.
 * Модель не анализируется со стороны ES вообще.
 */
object MColorData {

  val CODE_FN = "c"

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MColorData] = {
    (__ \ CODE_FN).format[String]
      .inmap(apply, _.code)
  }

}

case class MColorData(
  code: String
)
