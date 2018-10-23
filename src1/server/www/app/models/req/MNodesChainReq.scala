package models.req

import io.suggest.model.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 15:59
  * Description: Интерфейс реквестов с цепью узлов внутри.
  */
trait INodesChainReq[A] extends INodeReq[A] with INodeOptReq[A] {

  /** Цепочка узлов, которые затрагиваются экшеном. */
  def nodesChain: Seq[MNode]

  /** Последняя нода в цепочке узлов -- это целевой узел, запрошенный юзером. */
  override def mnode = nodesChain.last
  override def mnodeOpt = nodesChain.lastOption
  // ??? mnode, mnodeOpt - эти поля могут быть неуместны здесь, конфликтуя в будущем с аналогичными полями класса.
  // Возможно их надо выкинуть, вынести за пределы этого трейта или переименовать.

}


/** Дефолтовая реализация [[INodesChainReq]]. */
case class MNodesChainReq[A](
                              override val nodesChain : Seq[MNode],
                              override val request    : Request[A],
                              override val user       : ISioUser
                            )
  extends MReqWrap[A]
  with INodesChainReq[A]
