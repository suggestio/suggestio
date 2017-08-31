package io.suggest.jd.tags.qd

import io.suggest.model.n2.edge.EdgeUid_t
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:08
  * Description: Модель описания embed'а, который слинкован через edge.
  * В частности, это текст, который тоже живёт и индексируется в эджах.
  */
object MQdEdgeInfo {

  implicit val EDGE_EMBED_FORMAT: OFormat[MQdEdgeInfo] = {
    (__ \ "i").format[EdgeUid_t]
      .inmap(apply, _.edgeUid)
  }

  implicit def univEq: UnivEq[MQdEdgeInfo] = UnivEq.derive

}


/** Данные для указателя на эдж.
  *
  * @param edgeUid id в карте эджей.
  */
case class MQdEdgeInfo(
                       edgeUid: EdgeUid_t
                     )
