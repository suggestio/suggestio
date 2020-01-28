package io.suggest.n2.edge.edit

import io.suggest.n2.edge.MEdge
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.01.2020 0:28
  * Description: Для sys-node-edges сервер может присылать на клиент обновлённые данные по эджу через эту модель.
  */
object MEdgeWithId {

  implicit def edgeWithIdJson: OFormat[MEdgeWithId] = (
    (__ \ "i").format[MNodeEdgeIdQs] and
    (__ \ "e").format[MEdge]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MEdgeWithId] = UnivEq.derive

}

case class MEdgeWithId(
                        edgeId    : MNodeEdgeIdQs,
                        edge      : MEdge,
                      )
