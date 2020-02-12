package models.mup

import io.suggest.n2.edge.MEdge
import io.suggest.n2.media.MEdgeMedia
import io.suggest.n2.node.MNode
import models.req.{ISioUser, MReqWrap}
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.2020 10:15
  * Description: Реквест для download'а.
  */
case class MDownLoadReq[A](
                            swfsOpt                           : Option[MSwfsFidInfo],
                            mnode                             : MNode,
                            fileEdge                          : MEdge,
                            edgeMedia                         : MEdgeMedia,
                            override val request              : Request[A],
                            override val user                 : ISioUser
                          )
  extends MReqWrap[A]
