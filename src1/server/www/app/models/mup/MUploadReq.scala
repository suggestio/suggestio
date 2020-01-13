package models.mup

import io.suggest.swfs.client.proto.lookup.IVolumeLocation
import io.suggest.swfs.fid.Fid
import models.req.{ISioUser, MReqWrap}
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 16:26
  * Description: Модель реквеста с данными для аплоада, генерится из [[util.acl.CanUploadFile]].
  */
case class MUploadReq[A](
                          swfsOpt                           : Option[MSwfsFidInfo],
                          override val request              : Request[A],
                          override val user                 : ISioUser
                        )
  extends MReqWrap[A]


case class MSwfsFidInfo(
                         swfsFidParsedOpt     : Fid,
                         swfsMyVol            : IVolumeLocation,
                         swfsVolLookup        : Seq[IVolumeLocation]
                       )
