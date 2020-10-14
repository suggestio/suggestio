package io.suggest.bill

import boopickle.Default._
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.17 22:47
  * Description: Модель очень базовой текстовой инфы по узлу (или чему-либо ещё).
  */

/** Вспомогательная модель очень базовой инфы по чему-то сложному: только опциональный id и название чего-то. */
object MNameId {

  /** Поддержка boopickle-сериализации. */
  implicit def mNameIdPickler: Pickler[MNameId] = {
    generatePickler[MNameId]
  }

  @inline implicit def univEq: UnivEq[MNameId] = UnivEq.derive

  implicit def mNameIdFormat: OFormat[MNameId] = (
    (__ \ "i").formatNullable[String] and
    (__ \ "n").format[String]
  )(apply, unlift(unapply))

}


/** Простая модель для отображения очень базовых данных по какому-то узлу. */
case class MNameId(
                    id    : Option[String],
                    name  : String
                  )
