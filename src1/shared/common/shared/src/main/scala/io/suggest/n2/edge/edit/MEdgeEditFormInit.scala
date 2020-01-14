package io.suggest.n2.edge.edit

import io.suggest.n2.edge.MEdge
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.01.2020 14:56
  * Description: Клиент-серверная модель данных для начального состояния.
  */
object MEdgeEditFormInit {

  @inline implicit def univEq: UnivEq[MEdgeEditFormInit] = UnivEq.derive

  /** Поддержка JSON. */
  implicit def edgeEditFormInitJson: OFormat[MEdgeEditFormInit] = (
    (__ \ "e").formatNullable[MEdge] and
    (__ \ "i").format[MNodeEdgeIdQs]
  )(apply, unlift(unapply))

}


case class MEdgeEditFormInit(
                              edge            : Option[MEdge],
                              edgeId          : MNodeEdgeIdQs,
                            )
