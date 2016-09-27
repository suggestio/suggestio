package models.msc.resp

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.sc.ScConstants.Resp._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.16 18:55
  * Description: JSON-модель одного sc-resp-экшена.
  */
object MScRespAction {

  /** Поддержка JSON-сериализации этой write-only модели. */
  implicit val WRITES: OWrites[MScRespAction] = (
    (__ \ ACTION_FN).write[MScRespActionType] and
    (__ \ INDEX_RESP_ACTION).writeNullable[MScRespIndex]
  )(unlift(unapply))

}


case class MScRespAction(
  aType     : MScRespActionType,
  index     : Option[MScRespIndex]  = None
)
