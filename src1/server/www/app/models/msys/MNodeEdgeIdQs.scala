package models.msys

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.10.16 17:23
  * Description: qs-модель для указания на конкретный эдж конкретного узла.
  * Используется только в /sys/, т.к. она слишком низкоуровневая и нужна только в особо-технических случаях.
  */

object MNodeEdgeIdQs {

  /** Контейнер с именами полей модели. */
  object Fields {

    /** Имя поля с id узла. */
    def NODE_ID_FN  = "n"

    /** Имя поля с номером es-версии узла. */
    def NODE_VSN_FN = "v"

    /** id (порядковый номер) эджа. */
    def EDGE_ID_FN  = "e"

  }


  import Fields._

  /** Поддержка в play router. */
  implicit def mNodeEdgeIdQsQsb(implicit
                                strB   : QueryStringBindable[String],
                                longB  : QueryStringBindable[Long],
                                intB   : QueryStringBindable[Int]
                               ): QueryStringBindable[MNodeEdgeIdQs] = {

    new QueryStringBindableImpl[MNodeEdgeIdQs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MNodeEdgeIdQs]] = {
        val k = key1F(key)
        for {
          nodeIdE     <- strB.bind  (k(NODE_ID_FN),   params)
          nodeVsnE    <- longB.bind (k(NODE_VSN_FN),  params)
          edgeIdE     <- intB.bind  (k(EDGE_ID_FN),   params)
        } yield {
          for {
            nodeId    <- nodeIdE.right
            nodeVsn   <- nodeVsnE.right
            edgeId    <- edgeIdE.right
          } yield {
            MNodeEdgeIdQs(
              nodeId  = nodeId,
              nodeVsn = nodeVsn,
              edgeId  = edgeId
            )
          }
        }
      }

      override def unbind(key: String, value: MNodeEdgeIdQs): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Seq(
            strB.unbind   (k(NODE_ID_FN),   value.nodeId),
            longB.unbind  (k(NODE_VSN_FN),  value.nodeVsn),
            intB.unbind   (k(EDGE_ID_FN),   value.edgeId)
          )
        }
      }
    }
  }

}


/** Контейнер координат одного эджа. */
case class MNodeEdgeIdQs(
  // Не используем MEsUuId, т.к. это бывает небезопасно для ibeacon-маячков, имеющий комплексный id.
  nodeId    : String,
  nodeVsn   : Long,
  edgeId    : Int
)
