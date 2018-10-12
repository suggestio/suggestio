package io.suggest.sys.mdr

import io.suggest.common.empty.EmptyUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.18 14:29
  * Description: Ответ сервера на запрос следующего узла модерации.
  */
object MMdrNextResp {

  /** Поддержка play-json. */
  implicit def mMdrNextRespFormat: OFormat[MMdrNextResp] = (
    (__ \ "n").formatNullable[MNodeMdrInfo] and
    (__ \ "e").formatNullable[Iterable[String]]
      .inmap[Iterable[String]](
        EmptyUtil.opt2ImplEmpty1F(Nil),
        { nodeIds => if (nodeIds.isEmpty) None else Some(nodeIds) }
      ) and
    (__ \ "q").format[MMdrQueueReport]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MMdrNextResp] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


case class MMdrNextResp(
                         nodeOpt              : Option[MNodeMdrInfo],
                         errorNodeIds         : Iterable[String],
                         mdrQueue             : MMdrQueueReport,
                       ) {

  def withNodeOpt(nodeOpt: Option[MNodeMdrInfo]) = copy(nodeOpt = nodeOpt)

}
