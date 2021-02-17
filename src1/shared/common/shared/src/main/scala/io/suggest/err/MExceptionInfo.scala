package io.suggest.err

import io.suggest.common.empty.EmptyUtil
import io.suggest.scalaz.ScalazUtil
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.05.2020 15:39
  * Description: Кросс-платформенная инфа по исключению.
  */
object MExceptionInfo {

  @inline implicit def univEq: UnivEq[MExceptionInfo] = UnivEq.derive

  implicit def exInfoJson: OFormat[MExceptionInfo] = (
    (__ \ "c").format[String] and
    (__ \ "m").formatNullable[String] and
    (__ \ "s").formatNullable[Seq[String]]
      .inmap[Seq[String]](
        EmptyUtil.opt2ImplEmptyF(Nil),
        traces => Option.when(traces.nonEmpty)(traces)
      )
  )(apply, unlift(unapply))


  /** Извлечение данных из exception. */
  def from(ex: Throwable, maxStackLen: Int): MExceptionInfo = {
    MExceptionInfo(
      className = ex.getClass.getSimpleName,
      message   = Option( ex.getMessage ),
      stackTrace = ex
        .getStackTrace
        .iterator
        .take(maxStackLen)
        .map(_.toString)
        .toSeq,
    )
  }


  def className = GenLens[MExceptionInfo]( _.className )
  def message = GenLens[MExceptionInfo]( _.message )
  def stackTrace = GenLens[MExceptionInfo]( _.stackTrace )


  def validate( exInfo: MExceptionInfo ): ValidationNel[String, MExceptionInfo] = {
    (
      Validation.liftNel( exInfo.className )(
        !_.matches("[$a-zA-Z0-9_.]{3,80}"),
        "className.invalid"
      ) |@|
      ScalazUtil.liftNelOpt( exInfo.message ) { exMsg =>
        Validation success StringUtil.strLimitLen( exMsg, 256 )
      } |@|
      Validation.success {
        for {
          trc0 <- exInfo.stackTrace
          trc2 = StringUtil.strLimitLen( trc0.trim, 64 )
          if trc2.nonEmpty
        } yield trc2
      }
    )(apply)
  }

}


/** Инфа по исключению.
  *
  * @param className Название класса exception'а.
  * @param message Сообщение исключения.
  * @param stackTrace Стэк-трейс.
  */
case class MExceptionInfo(
                           className        : String,
                           message          : Option[String]      = None,
                           stackTrace       : Seq[String]         = Nil,
                         )
