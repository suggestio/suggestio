package models.req

import io.suggest.n2.edge.MEdge
import io.suggest.n2.media.MEdgeMedia
import io.suggest.n2.node.MNode
import models.mup.MSwfsFidInfo
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.02.18 19:48
  * Description: Интерфейс абстрактной модели реквеста за MMedia.
  */

/** Модель реквеста с данными запроса к dyn-картинке, которой может ещё пока не существовать.
  *
  * @param derivativeOpt Опциональный узел, содержащий данные файла с файлом-деривативом.
  * @param mnode Узел, соотносящийся с оригиналом картинки.
  * @param user Данные по юзеру.
  * @param request Исходный HTTP-реквест.
  * @tparam A Тип body реквеста.
  */
final case class MFileReq[A](
                              override val edge        : MEdge,
                              override val edgeMedia   : MEdgeMedia,
                              storageInfo              : Option[MSwfsFidInfo],
                              override val mnode       : MNode,
                              override val user        : ISioUser,
                              override val request     : Request[A],
                              derivativeOpt            : Option[MNode] = None,
                            )
  extends MReqWrap[A]
  with IEdgeMediaReq[A]
{

  def derivativeOrOrig: MNode =
    derivativeOpt getOrElse mnode

}
