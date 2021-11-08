package io.suggest.jd.tags.html

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** Jd-tree encoding types enumeration for HTML tree. */
object MJdHtmlTypes extends StringEnum[MJdHtmlType] {

  /** Type for html tag.
    * Attrs and content stored in tree subforest: attrs first, sub-tags and content - after.
    */
  case object Tag extends MJdHtmlType("tag")

  /** Attribute type.
    * Key must be defined. Mostly, value is defined.
    * For complex attrs (like {{{style="..."}}}), value is empty, and children attrs is defined in subforest.
    */
  case object Attribute extends MJdHtmlType("attr")

  /** Content data. For example, plain text.
    * Key must be empty.
    * Value or edge-uid must be defined.
    */
  case object Content extends MJdHtmlType("data")


  override def values = findValues

}

/** Html sub-types super-class for all possible sub-types. */
sealed abstract class MJdHtmlType(override val value: String) extends StringEnumEntry


object MJdHtmlType {

  @inline implicit def univEq: UnivEq[MJdHtmlType] = UnivEq.derive

  implicit def jdHtmlTypePlayJson: Format[MJdHtmlType] =
    EnumeratumUtil.valueEnumEntryFormat( MJdHtmlTypes )

}
