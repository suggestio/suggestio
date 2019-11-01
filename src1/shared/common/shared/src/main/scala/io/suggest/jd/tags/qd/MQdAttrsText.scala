package io.suggest.jd.tags.qd

import io.suggest.color.MColorData
import io.suggest.common.empty.EmptyProduct
import io.suggest.err.ErrorConstants
import io.suggest.font.{MFont, MFontSize}
import io.suggest.primo.{IEqualsEq, IHashCodeLazyVal, ISetUnset}
import io.suggest.scalaz.ScalazUtil
import io.suggest.text.UrlUtil2
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:45
  * Description: Модель аттрибутов текста.
  */
object MQdAttrsText {

  def empty = MQdAttrsText()

  object Fields {
    val BOLD_FN       = "b"
    val ITALIC_FN     = "i"
    val UNDERLINE_FN  = "u"
    val STRIKE_FN     = "r"
    val COLOR_FN      = "c"
    val BACKGROUND_FN = "g"
    val LINK_FN       = "l"
    val SRC_FN        = "s"
    val FONT_FN       = "f"
    val SIZE_FN       = "z"
    val SCRIPT_FN     = "p"
  }

  /** Поддержка play-json. */
  implicit val QD_ATTRS_FORMAT: OFormat[MQdAttrsText] = {
    val F = Fields
    (
      (__ \ F.BOLD_FN).formatNullable[ISetUnset[Boolean]] and
      (__ \ F.ITALIC_FN).formatNullable[ISetUnset[Boolean]] and
      (__ \ F.UNDERLINE_FN).formatNullable[ISetUnset[Boolean]] and
      (__ \ F.STRIKE_FN).formatNullable[ISetUnset[Boolean]] and
      (__ \ F.COLOR_FN).formatNullable[ISetUnset[MColorData]] and
      (__ \ F.BACKGROUND_FN).formatNullable[ISetUnset[MColorData]] and
      (__ \ F.LINK_FN).formatNullable[ISetUnset[String]] and
      (__ \ F.SRC_FN).formatNullable[ISetUnset[String]] and
      (__ \ F.FONT_FN).formatNullable[ISetUnset[MFont]] and
      (__ \ F.SIZE_FN).formatNullable[ISetUnset[MFontSize]] and
      (__ \ F.SCRIPT_FN).formatNullable[ISetUnset[MQdScript]]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MQdAttrsText] = UnivEq.derive

  /** Провалидировать для сохранения в БД.
    * Все Unset считаются невалидными.
    * Цвета минифицируются до hex-only.
    *
    * @param attrsText Аттрибуты текста.
    * @return Результат валидации.
    */
  def validateForStore(attrsText: MQdAttrsText): ValidationNel[String, MQdAttrsText] = {
    val errMsgF = ErrorConstants.emsgF("text")

    def _vUrl(urlSuOpt: Option[ISetUnset[String]], fn: String) = {
      def linkErrF = errMsgF(fn)
      ISetUnset.validateSetOpt( urlSuOpt,    linkErrF)(UrlUtil2.validateUrl(_, linkErrF) )
        // Подавляем ошибки в ссылках путём срезания некорректных ссылок
        .orElse( Validation.success(None) )
    }

    // У ij idea крышу срывает от концентрации функций в одном месте начиная с 10й строчки списка:
    val F = Fields
    (
      ISetUnset.validateSetOptDflt( attrsText.bold,       errMsgF(F.BOLD_FN) ) |@|
      ISetUnset.validateSetOptDflt( attrsText.italic,     errMsgF(F.ITALIC_FN) ) |@|
      ISetUnset.validateSetOptDflt( attrsText.underline,  errMsgF(F.UNDERLINE_FN) ) |@|
      ISetUnset.validateSetOptDflt( attrsText.strike,     errMsgF(F.STRIKE_FN) ) |@|
      ISetUnset.validateSetOpt( attrsText.color,          errMsgF(F.COLOR_FN))(MColorData.validateHexCodeOnly) |@|
      ISetUnset.validateSetOpt( attrsText.background,     errMsgF(F.BACKGROUND_FN))(MColorData.validateHexCodeOnly) |@|
      _vUrl(attrsText.link, F.LINK_FN) |@|
      _vUrl(attrsText.src,  F.SRC_FN) |@|
      ISetUnset.validateSetOptDflt( attrsText.font,       errMsgF(F.FONT_FN) ) |@|
      ISetUnset.validateSetOptDflt( attrsText.size,       errMsgF(F.SIZE_FN) ) |@|
      ISetUnset.validateSetOptDflt( attrsText.script,     errMsgF(F.SCRIPT_FN) )
    )(apply)
  }

  val background = GenLens[MQdAttrsText](_.background)

}


/** Класс модели аттрибутов quill-delta-операции. */
case class MQdAttrsText(
                         bold        : Option[ISetUnset[Boolean]]       = None,
                         italic      : Option[ISetUnset[Boolean]]       = None,
                         underline   : Option[ISetUnset[Boolean]]       = None,
                         strike      : Option[ISetUnset[Boolean]]       = None,
                         color       : Option[ISetUnset[MColorData]]    = None,
                         background  : Option[ISetUnset[MColorData]]    = None,
                         link        : Option[ISetUnset[String]]        = None,
                       // TODO src аттрибут -- нужен ли вообще? Вроде он был взят из доки, которая вроде бы не очень актуальна.
                       // Надо вписать println() в конвертер дельты поля src и тыкать quill, пока в консоли что-нибудь не проявится.
                         src         : Option[ISetUnset[String]]        = None,
                         font        : Option[ISetUnset[MFont]]         = None,
                         size        : Option[ISetUnset[MFontSize]]     = None,
                         script      : Option[ISetUnset[MQdScript]]     = None,
                       )
  extends EmptyProduct
  // Для ScalaCSS-рендера: Максимальная скорость работы `==` и hashCode()
  with IHashCodeLazyVal
  with IEqualsEq
{

  /** Подразумевает ли данный набор аттрибутов необходимость использования css-стилей? */
  def isCssStyled: Boolean = {
    font.isDefined ||
      size.isDefined ||
      color.isDefined ||
      background.isDefined
  }

}
