package io.suggest.sc.search

import io.suggest.common.empty.EmptyUtil
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.17 16:15
  * Description: Модель ответа сервера по тегам выдачи.
  */
object MSc3NodeSearchResp {

  /** Поддержка play-json для инстансов [[MSc3NodeSearchResp]]. */
  implicit def msc3NodesSearchRespFormat: OFormat[MSc3NodeSearchResp] = {
    (__ \ "t").formatNullable[Seq[MSc3NodeInfo]]
      .inmap [Seq[MSc3NodeInfo]] (
        EmptyUtil.opt2ImplEmpty1F(Nil),
        { tags => if (tags.isEmpty) None else Some(tags) }
      )
      .inmap(apply, _.results)
  }

  implicit def univEq: UnivEq[MSc3NodeSearchResp] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}
case class MSc3NodeSearchResp(
                               results: Seq[MSc3NodeInfo]
                             )


/** Модель данных по одному найденному тегу. */
object MSc3NodeInfo {

  /** Поддержка play-json для инстансов [[MSc3NodeInfo]]. */
  implicit def MSC3_TAG_FORMAT: OFormat[MSc3NodeInfo] = (
    (__ \ "n").format[String] and
    (__ \ "i").format[String]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MSc3NodeInfo] = UnivEq.derive

}

/** Данные по одному узлу (изначально - найденному тегу).
  *
  * @param name Отображаемое юзеру название тега.
  * @param nodeId id узла-тега.
  */
case class MSc3NodeInfo(
                         name     : String,
                         nodeId   : String,
                       )
