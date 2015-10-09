package io.suggest.swfs.client.proto.assign

import io.suggest.swfs.client.proto.{IToQs, Replication}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 20:18
 * Description: Модель параметров assign-запроса.
 */

object AssignRequest {

  val empty = AssignRequest()

}


trait IAssignRequest extends IToQs {

  def dataCenter    : Option[String]
  def replication   : Option[Replication]

  /** Перегонка в query string. */
  override def toQs: String = {
    val sb = new StringBuilder

    for (dc <- dataCenter)
      sb.append('?')
        .append("dataCenter=").append(dc)

    def _addDelim(): StringBuilder = {
      val ch = if (sb.nonEmpty) '&' else '?'
      sb.append(ch)
    }

    for (rep <- replication) {
      _addDelim()
        .append("replication=").append(rep.toString)
    }

    sb.toString()
  }

}

case class AssignRequest(
  dataCenter    : Option[String]        = None,
  replication   : Option[Replication]   = None
)
  extends IAssignRequest
{
  override lazy val toQs = super.toQs
}
