package models.req

import io.suggest.mbill2.m.item.MItem
import io.suggest.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 11:21
  * Description: Модель-контейнер данных реквестов с mad и mitem внутри одновременно.
  */
trait IItemNodeReq[A]
  extends IItemReq[A]
  with INodeReq[A]


/** Дефолтовая реализация [[IItemNodeReq]]. */
case class MItemNodeReq[A](
                            override val mitem    : MItem,
                            override val mnode    : MNode,
                            override val user     : ISioUser,
                            override val request  : Request[A]
                          )
  extends MReqWrap[A]
  with IItemNodeReq[A]

