package models.req

import io.suggest.model.n2.media.MMedia
import io.suggest.model.n2.node.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.02.18 19:48
  * Description: Интерфейс абстрактной модели реквеста за MMedia.
  */
trait IMediaReq[A] extends IReq[A] {

  def mmedia: MMedia

}


/** Интерфейс модели реквеста за MMedia и соответствующей ей MNode. */
trait IMediaNodeReq[A]
  extends IMediaReq[A]
  with INodeReq[A]


/** Модель реквеста с данными запроса к MMedia.
  *
  * @param mmedia MMedia.
  * @param mnode Узел, соотносящийся с MMedia.
  * @param user Данные по юзеру.
  * @param request Исходный HTTP-реквест.
  * @tparam A Тип body реквеста.
  */
case class MMediaNodeReq[A](
                             override val mmedia   : MMedia,
                             override val mnode    : MNode,
                             override val user     : ISioUser,
                             override val request  : Request[A]
                           )
  extends MReqWrap[A]
  with IMediaNodeReq[A]
