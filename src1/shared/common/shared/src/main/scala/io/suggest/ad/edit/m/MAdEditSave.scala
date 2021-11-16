package io.suggest.ad.edit.m

import io.suggest.jd.MJdEdge
import io.suggest.n2.extra.doc.MNodeDoc
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Save model support boilerplate for ad-edit. */
object MAdEditSave {

  @inline implicit def univEq: UnivEq[MAdEditSave] = UnivEq.derive

  implicit def adEditSaveJson: OFormat[MAdEditSave] = {
    (
      (__ \ "doc").format[MNodeDoc] and
      (__ \ "edges").format[Iterable[MJdEdge]] and
      (__ \ "title").formatNullable[String]
    )(apply, unlift(unapply))
  }

}


/** Ad editor data container for saving.
  * Diverged from MJdData, because of missing html-field, but contains useless fields like JdTagId.
  *
  * @param doc Doc: node doc for saving.
  *            Originally, jd-editor contained only jd-tree submission.
  *            Future ad-editor will submit html-only stuff to server.
  * @param edges Edges data for uploaded files, jd-texts, etc.
  * @param title Optional ad title.
  */
final case class MAdEditSave(
                              doc         : MNodeDoc,
                              edges       : Iterable[MJdEdge],
                              title       : Option[String],
                              // metadata, sale tariffs, etc, etc
                            )
