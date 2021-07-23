package io.suggest.i18n

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** Enum describing supported/known languages. */
object MLanguages extends StringEnum[MLanguage] {

  // Keep alphabetical order here.

  case object English extends MLanguage("en") {
    override def singular = "English"
  }

  case object Russian extends MLanguage("ru") {
    override def singular = "Russian"
  }

  override def values = findValues

  final def default = English

  def byCode(langCode: String): MLanguage = {
    val langCodeLc = langCode.toLowerCase
    if (
      ("ru" :: "be" :: "uk" :: Nil)   // TODO Need more languages here...
        .exists(langCodeLc startsWith _)
    )
      Russian
    else
      English
  }

}


sealed abstract class MLanguage(override val value: String) extends StringEnumEntry {
  def singular: String
  override final def toString = singular
}

object MLanguage {

  @inline implicit def univEq: UnivEq[MLanguage] = UnivEq.derive

  implicit def languageFormat: Format[MLanguage] =
    EnumeratumUtil.valueEnumEntryFormat( MLanguages )

}
