package models.mup

import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.swfs.client.proto.lookup.IVolumeLocation
import models.req.{IReq, ISioUser, MReqWrap}
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 16:26
  * Description: Модель реквеста с данными для аплоада, генерится из [[util.acl.CanUploadFile]].
  */
trait IUploadReq[A]
  extends IReq[A]
{

  /** Распарсенный swfs fid, если есть. */
  def swfsOpt: Option[MSwfsFidInfo]

}


/** Дефолтовая реализация [[IUploadReq]]. */
case class MUploadReq[A](
                          override val swfsOpt              : Option[MSwfsFidInfo],
                          override val request              : Request[A],
                          override val user                 : ISioUser
                        )
  extends MReqWrap[A]
  with IUploadReq[A]


case class MSwfsFidInfo(
                         swfsFidParsedOpt     : Fid,
                         swfsMyVol            : IVolumeLocation,
                         swfsVolLookup        : Seq[IVolumeLocation]
                       )
