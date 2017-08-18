package io.suggest.ad.doc.dtag

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
    (__ \ "r").format[Int]
      .inmap[PlainPayload]( apply, _.resourceId )
  }

}


/** Класс тега просто рендера элемента payload'а.
  *
  * @param resourceId id payload-элемента, который требуется отрендерить.
  */
case class PlainPayload(
                         resourceId: Int
                       )
  extends IDocTag {

  override def dtName = MDtNames.PlainPayload

  override def children = Nil

}
