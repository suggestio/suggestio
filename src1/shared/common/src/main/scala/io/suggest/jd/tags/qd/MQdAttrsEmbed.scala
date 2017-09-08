package io.suggest.jd.tags.qd

import io.suggest.common.empty.EmptyProduct
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal, ISetUnset}
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.17 17:18
  * Description: Embed-only аттрибуты.
  */
object MQdAttrsEmbed {

  /** Поддержка play-json. */
  implicit val MQD_ATTRS_EMBED_FORMAT: OFormat[MQdAttrsEmbed] = (
    (__ \ "w").formatNullable[ISetUnset[Int]] and
    (__ \ "h").formatNullable[ISetUnset[Int]]
  )(apply, unlift(unapply))

  /** Поддержка UnivEq. */
  implicit def univEq: UnivEq[MQdAttrsEmbed] = UnivEq.derive

}


/** Класс модели аттрибутов embed'а.
  * Абсолютно все - необязательные.
  *
  * @param width Ширина.
  * @param height Высота.
  */
case class MQdAttrsEmbed(
                          width     : Option[ISetUnset[Int]]    = None,
                          height    : Option[ISetUnset[Int]]    = None
                        )
  extends EmptyProduct
  // Для ScalaCSS-рендера: Максимальная скорость работы `==` и hashCode()
  with IHashCodeLazyVal
  with IEqualsEq
