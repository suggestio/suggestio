package io.suggest.jd.tags.qd

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:14
  * Description: Модель описания внешнего embed'а.
  */
object MEmbedExt {

  implicit val EMBED_EXT_FORMAT: OFormat[MEmbedExt] = {
    (__ \ "s").format[String]
      .inmap(apply, _.src)
  }

  implicit def univEq: UnivEq[MEmbedExt] = UnivEq.derive

}

case class MEmbedExt(
                      src: String
                    )
