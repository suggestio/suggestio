package models.mgeo

import play.api.mvc.QueryStringBindable
import util.qsb.QsbKey1T

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.15 10:54
 * Description: Модель данных указателя на геошейп.
 * Шейп хранится в узле среди других шейпов, поэтому нужен id узла и внутренний id шейпа.
 */
object MGsPtr {

  def NODE_ID_FN = "n"
  def GS_ID_FN   = "i"

  implicit def qsb(implicit
                   strB: QueryStringBindable[String],
                   intB: QueryStringBindable[Int]): QueryStringBindable[MGsPtr] = {
    new QueryStringBindable[MGsPtr] with QsbKey1T {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MGsPtr]] = {
        val k = key1F(key)
        for {
          nodeIdEith  <- strB.bind(k(NODE_ID_FN), params)
          gsIdEith    <- intB.bind(k(GS_ID_FN),   params)
        } yield {
          for {
            nodeId    <- nodeIdEith.right
            gsId      <- gsIdEith.right
          } yield {
            MGsPtr(nodeId, gsId)
          }
        }
      }

      override def unbind(key: String, value: MGsPtr): String = {
        val k = key1F(key)
        Iterator(
          strB.unbind(k(NODE_ID_FN), value.nodeId),
          intB.unbind(k(GS_ID_FN),   value.gsId)
        )
          .mkString("&")
      }
    }
  }

}


case class MGsPtr(
  nodeId  : String,
  gsId    : Int
)
