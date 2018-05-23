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
object MSc3TagsResp {

  /** Поддержка play-json для инстансов [[MSc3TagsResp]]. */
  implicit def MSC3_TAGS_RESP_FORMAT: OFormat[MSc3TagsResp] = {
    (__ \ "t").formatNullable[Seq[MSc3Tag]]
      .inmap [Seq[MSc3Tag]] (
        EmptyUtil.opt2ImplEmpty1F(Nil),
        { tags => if (tags.isEmpty) None else Some(tags) }
      )
      .inmap(apply, _.tags)
  }

  implicit def univEq: UnivEq[MSc3TagsResp] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}
case class MSc3TagsResp(
                         tags: Seq[MSc3Tag]
                       )


/** Модель данных по одному найденному тегу. */
object MSc3Tag {

  /** Поддержка play-json для инстансов [[MSc3Tag]]. */
  implicit def MSC3_TAG_FORMAT: OFormat[MSc3Tag] = (
    (__ \ "n").format[String] and
    (__ \ "i").format[String]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MSc3Tag] = UnivEq.derive

}

/** Данные по одному найденному тегу.
  *
  * @param name Отображаемое юзеру название тега.
  * @param nodeId id узла-тега.
  */
case class MSc3Tag(
                    name     : String,
                    nodeId   : String
                  )
