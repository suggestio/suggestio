package io.suggest.ad.doc.dtag

import play.api.libs.json.OFormat
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.17 12:19
  * Description: Struct-тег документа. Содержит только дочерние элементы.
  */

object Document {

  /** Поддержка play-json. */
  implicit val DOCUMENT_FORMAT: OFormat[Document] = {
    IDocTag.CHILDREN_IDOC_TAG_FORMAT
      .inmap[Document]( rawApply, _.children )
  }

  def rawApply(chs: Seq[IDocTag]): Document = {
    apply()(chs: _*)
  }

  def rawUnapply(d: Document): Option[Seq[IDocTag]] = {
    Some(d.children)
  }

}


/** Класс документа-корня. */
case class Document()(
                     override val children: IDocTag*
                   )
  extends IDocTag {

  override def dtName = MDtNames.Document

}
