package io.suggest.jd

import io.suggest.model.n2.edge.EdgeUid_t
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:08
  * Description: Модель описания embed'а, который слинкован через edge.
  * В частности, это текст, который тоже живёт и индексируется в эджах.
  */
object MJdEdgeId {

  implicit val MJD_EDGE_ID_FORMAT: OFormat[MJdEdgeId] = {
    (__ \ "i").format[EdgeUid_t]
      .inmap(apply, _.edgeUid)
  }

  implicit def univEq: UnivEq[MJdEdgeId] = UnivEq.derive

}


/** Данные для указателя на эдж.
  *
  * @param edgeUid id в карте эджей.
  */
case class MJdEdgeId(
                      edgeUid: EdgeUid_t
                    ) {

  override final def hashCode = edgeUid

}
