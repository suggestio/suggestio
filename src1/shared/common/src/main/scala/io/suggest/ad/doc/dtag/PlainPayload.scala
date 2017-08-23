package io.suggest.ad.doc.dtag

import io.suggest.model.n2.edge.EdgeUid_t
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 16:35
  * Description: Тег, обозначающий необходимость plain-рендера какого-то элемента payload.
  *
  * Основным случаем такого рендера является... СТРОКА ТЕКСТА!
  * Текст хранится отдельно, т.к. его так можно удобно индексировать в ES, и дедублицировать везде.
  */
object PlainPayload {

  /** Поддержка play-json. */
  implicit val PLAIN_PAYLOAD_FORMAT: OFormat[PlainPayload] = {
    (__ \ "r").format[EdgeUid_t]
      .inmap[PlainPayload]( apply, _.edgeId )
  }

}


/** Класс тега просто рендера элемента payload'а.
  *
  * @param edgeId id payload-элемента, который требуется отрендерить.
  */
case class PlainPayload(
                         edgeId: EdgeUid_t
                       )
  extends IDocTag {

  override def dtName = MDtNames.PlainPayload

  override def children = Nil

}
