package io.suggest.lk.nodes

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.2020 9:22
  * Description: Кросс-платформенная модель обновляемого значения по ключу.
  * Т.к. тип может быть разный, тут разные поля для разных типов, + статически-типизированный ключ.
  */
object MLknOpValue {

  object Fields {
    def BOOL = "b"
  }

  @inline implicit def univEq: UnivEq[MLknOpValue] = UnivEq.derive

  implicit def lknOpValueJson: OFormat[MLknOpValue] = {
    val F = Fields
    (__ \ F.BOOL).formatNullable[Boolean]
      .inmap[MLknOpValue]( apply, _.bool )
  }

}


/** Контейнер данных по точечному обновлению одного значения в базе s.io. */
final case class MLknOpValue(
                              bool              : Option[Boolean]           = None,
                            )
