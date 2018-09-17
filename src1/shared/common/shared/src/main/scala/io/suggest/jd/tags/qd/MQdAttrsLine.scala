package io.suggest.jd.tags.qd

import io.suggest.common.empty.EmptyProduct
import io.suggest.err.ErrorConstants
import io.suggest.math.MathConst
import io.suggest.primo.ISetUnset
import io.suggest.text.MTextAlign
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scalaz.ValidationNel
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.17 16:02
  * Description: Аттрибуты строки.
  */
object MQdAttrsLine {

  object Fields {
    val HEADER_FN       = "h"
    val LIST_FN         = "t"
    val INDENT_FN       = "n"
    val CODE_BLOCK_FN   = "c"
    val BLOCK_QUOTE_FN  = "b"
    val ALIGN_FN        = "a"
  }

  /** Поддержка play-json. */
  implicit val QD_ATTRS_LINE_FORMAT: OFormat[MQdAttrsLine] = {
    val F = Fields
    (
      (__ \ F.HEADER_FN).formatNullable[ISetUnset[Int]] and
      (__ \ F.LIST_FN).formatNullable[ISetUnset[MQdListType]] and
      (__ \ F.INDENT_FN).formatNullable[ISetUnset[Int]] and
      (__ \ F.CODE_BLOCK_FN).formatNullable[ISetUnset[Boolean]] and
      (__ \ F.BLOCK_QUOTE_FN).formatNullable[ISetUnset[Boolean]] and
      (__ \ F.ALIGN_FN).formatNullable[ISetUnset[MTextAlign]]
    )(apply, unlift(unapply))
  }

  implicit def univEq: UnivEq[MQdAttrsLine] = UnivEq.derive


  /** Валидация инстанса MQdAttrsLine для дальнейшего сохранения в БД. */
  def validateForStore(attrsLine: MQdAttrsLine): ValidationNel[String, MQdAttrsLine] = {
    val errMsgF = ErrorConstants.emsgF("line")
    def vldInt1(vSuOpt: Option[ISetUnset[Int]], max: Int, fn: String) = {
      def errF = errMsgF(fn)
      ISetUnset.validateSetOpt(vSuOpt, errF) {
        MathConst.Counts.validateMinMax(_, 1, max, errF)
      }
    }
    val F = Fields
    // Надо проверить числа в header и в indent.
    (
      vldInt1(attrsLine.header, 6, F.HEADER_FN) |@|
      ISetUnset.validateSetOptDflt(attrsLine.list,       errMsgF(F.LIST_FN)) |@|
      vldInt1(attrsLine.indent, 8, F.INDENT_FN) |@|
      ISetUnset.validateSetOptDflt(attrsLine.codeBlock,  errMsgF(F.CODE_BLOCK_FN)) |@|
      ISetUnset.validateSetOptDflt(attrsLine.blockQuote, errMsgF(F.BLOCK_QUOTE_FN)) |@|
      ISetUnset.validateSetOptDflt(attrsLine.align,      errMsgF(F.ALIGN_FN))
    )(apply)
  }

}


case class MQdAttrsLine(
                         header      : Option[ISetUnset[Int]]           = None,
                         list        : Option[ISetUnset[MQdListType]]   = None,
                         indent      : Option[ISetUnset[Int]]           = None,
                         codeBlock   : Option[ISetUnset[Boolean]]       = None,
                         blockQuote  : Option[ISetUnset[Boolean]]       = None,
                         align       : Option[ISetUnset[MTextAlign]]    = None
                       )
  extends EmptyProduct
{

  /** Бывают line-аттрибуты, требующие группировки строк в кучу.
    * Например: list.
    * Если сгруппировать
    *
    * @param other line-аттрибуты какой-то другой группы.
    * @return true, когда надо сгруппировать две сущности вместе.
    *         false, когда надо рендерить в отдельных группа.
    */
  // TODO Это грязный кривой костыль, чтобы избежать склеивания строк в группах indent/align/etc.
  // Он вызывает рендер пачки pre/blockquote/etc тегов, вместо одного общего.
  // Для разных тегов должна быть разная политика группировки.
  // Например, для pre-группы надо \n-концы строк между строками
  // А для list -- вообще без \n или br
  // Для остальных - br или p
  def isGroupsWith(other: MQdAttrsLine): Boolean = {
    // TODO Пока не ясно, что с заголовком.
    align.isEmpty &&
      indent.isEmpty &&
      codeBlock.isEmpty &&
      blockQuote.isEmpty &&
      (this ==* other)
  }

}
