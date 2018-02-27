package models.req

import io.suggest.model.n2.media.MMedia
import io.suggest.model.n2.node.MNode
import models.mup.MSwfsFidInfo
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.02.18 19:48
  * Description: Интерфейс абстрактной модели реквеста за MMedia.
  */
trait IMediaOptReq[A] extends IReq[A] {

  def mmediaOpt: Option[MMedia]

}


/** Интерфейс модели реквеста за MMedia и соответствующей ей MNode. */
trait IMediaNodeReq[A]
  extends IMediaOptReq[A]
  with INodeReq[A]


/** Модель реквеста с данными запроса к MMedia.
  *
  * @param mmediaOpt MMedia запрашиваемой картинки, если найдена.
  * @param mmediaOrigOptFut Функция (обёртка над lazy val), возвращающая фьючерс MMedia оригинальной картинки.
  * @param mnode Узел, соотносящийся с MMedia.
  * @param user Данные по юзеру.
  * @param request Исходный HTTP-реквест.
  * @tparam A Тип body реквеста.
  */
case class MMediaOptNodeReq[A](
                                override val mmediaOpt   : Option[MMedia],
                                mmediaOrigOptFut         : () => Future[Option[MMedia]],
                                storageInfo              : MSwfsFidInfo,
                                override val mnode       : MNode,
                                override val user        : ISioUser,
                                override val request     : Request[A]
                              )
  extends MReqWrap[A]
  with IMediaNodeReq[A]
