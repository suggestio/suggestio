package models.req

import io.suggest.model.n2.media.MEdgeMedia
import io.suggest.model.n2.node.MNode
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
  * @param derivedOpt Опциональное выведенное значение.
  * @param mnode Узел, соотносящийся с MMedia.
  * @param user Данные по юзеру.
  * @param request Исходный HTTP-реквест.
  * @tparam A Тип body реквеста.
  */
case class MDynImgReq[A](
                          derivedOpt               : Option[MNode],
                          edgeMedia                : MEdgeMedia,
                          storageInfo              : MSwfsFidInfo,
                          override val mnode       : MNode,
                          override val user        : ISioUser,
                          override val request     : Request[A]
                        )
  extends MReqWrap[A]
  with INodeReq[A]
