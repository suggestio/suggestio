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
      .inmap[JsonDocument]( rawApply, _.children )
  }

  def rawApply(chs: Seq[IDocTag]): JsonDocument = {
    apply()(chs: _*)
  }

  def rawUnapply(d: JsonDocument): Option[Seq[IDocTag]] = {
    Some(d.children)
  }

}


/** Класс документа-корня. */
case class JsonDocument()(
                          override val children: IDocTag*
                         )
  extends IDocTag {

  override def dtName = MJdTagNames.Document

}
