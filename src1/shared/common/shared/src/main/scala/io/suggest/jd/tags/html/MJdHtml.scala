package io.suggest.jd.tags.html

import io.suggest.jd.MJdEdgeId
import japgolly.univeq._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/** HTML-encoding model over JD-tree. */
object MJdHtml {

  object Fields {
    final def HTML_TYPE   = "type"
    final def KEY         = "key"
    final def VALUE       = "value"
    final def EDGE_UIDS   = "edge"
  }

  @inline implicit def univEq: UnivEq[MJdHtml] = UnivEq.derive

  implicit def jdHtmlTagJson: OFormat[MJdHtml] = {
    val F = Fields
    (
      (__ \ F.HTML_TYPE).format[MJdHtmlType] and
      (__ \ F.KEY).formatNullable[String] and
      (__ \ F.VALUE).formatNullable[String] and
      (__ \ F.EDGE_UIDS).formatNullable[List[MJdEdgeId]]
        .inmap[List[MJdEdgeId]](
          _ getOrElse Nil,
          edgeUids => Option.when(edgeUids.nonEmpty)( edgeUids )
        )
    )(apply, unlift(unapply))
  }

}

/** HTML attribute value container for each html-attribute in HTML-JD-tag.
  *
  * @param htmlType Type of current html-encoding particle: tag (tag starting) | attribute | content | ...
  * @param key Lower-case name of tag or attribute.
  *            None for content data.
  * @param value String value for attribute or content.
  * @param edgeUid JD-edge ids list.
  *                If value and edgeIds both defined, inline value should be substituted/interpolated into string value somehow.
  */
final case class MJdHtml(
                          htmlType   : MJdHtmlType,
                          key        : Option[String],
                          value      : Option[String]        = None,
                          edgeUid    : List[MJdEdgeId]       = Nil,
                        )
