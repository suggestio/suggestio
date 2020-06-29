package models.mup

import io.suggest.n2.edge.MEdge
import io.suggest.n2.media.MEdgeMedia
import io.suggest.n2.node.MNode
import io.suggest.swfs.client.proto.lookup.IVolumeLocation
import io.suggest.swfs.fid.Fid
import models.req.{ISioUser, MReqWrap}
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 16:26
  * Description: Модель реквеста с данными для аплоада, генерится из [[util.acl.CanUpload]].
  */
case class MUploadReq[A](
                          swfsOpt                           : Option[MSwfsFidInfo],
                          existNodeOpt                      : Option[MNode],
                          override val request              : Request[A],
                          override val user                 : ISioUser
                        )
  extends MReqWrap[A]


case class MSwfsFidInfo(
                         swfsFidParsedOpt     : Fid,
                         swfsMyVol            : IVolumeLocation,
                         swfsVolLookup        : Seq[IVolumeLocation]
                       )


/** Upload chunk request. */
final case class MUploadChunkReq[A](
                                     mnode                             : MNode,
                                     fileEdge                          : MEdge,
                                     fileEdgeMedia                     : MEdgeMedia,
                                     override val request              : Request[A],
                                     override val user                 : ISioUser,
                                   )
  extends MReqWrap[A]

