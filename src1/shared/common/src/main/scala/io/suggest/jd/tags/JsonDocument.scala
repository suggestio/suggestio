package io.suggest.jd.tags

import play.api.libs.functional.syntax._
import play.api.libs.json.OFormat

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 12:19
  * Description: Struct-тег документа. Содержит только дочерние элементы.
  */

object JsonDocument {

  /** Поддержка play-json. */
  implicit val DOCUMENT_FORMAT: OFormat[JsonDocument] = {
    IDocTag.CHILDREN_IDOC_TAG_FORMAT
      .inmap[JsonDocument]( apply, _.children )
  }

  def a()(children: IDocTag*): JsonDocument = {
    apply( children )
  }

}


/** Класс документа-корня. */
case class JsonDocument(
                         override val children: Seq[IDocTag]
                       )
  extends IDocTag {

  override def jdTagName = MJdTagNames.DOCUMENT

  override def withChildren(children: Seq[IDocTag]): JsonDocument = {
    copy( children = children )
  }

}
