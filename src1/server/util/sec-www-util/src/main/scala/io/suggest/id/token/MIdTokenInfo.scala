package io.suggest.id.token

import io.suggest.common.empty.EmptyUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.19 18:54
  * Description: Информация по конкретным шагам идентификации.
  */
object MIdTokenInfo {

  implicit def mIdTokenInfoJson: OFormat[MIdTokenInfo] = {
    (__ \ "c").formatNullable[List[MIdMessage]]
      .inmap[List[MIdMessage]](
        EmptyUtil.opt2ImplEmptyF(Nil),
        msgs => if (msgs.isEmpty) None else Some(msgs)
      )
      .inmap[MIdTokenInfo]( apply, _.idMsgs )
  }

  @inline implicit def univEq: UnivEq[MIdTokenInfo] = UnivEq.derive

}


/** Контейнер данных текущих проверок.
  *
  * @param idMsgs Отправленные сообщения юзеру для идентификации.
  */
case class MIdTokenInfo(
                         idMsgs    : List[MIdMessage]     = Nil,
                       )
