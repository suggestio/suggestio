package io.suggest.log

import io.suggest.common.html.HtmlConstants
import io.suggest.err.MExceptionInfo
import io.suggest.msg.ErrorMsg_t
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
  * Created: 04.05.2020 15:36
  * Description: Контейнер данных по одному лог-сообщению.
  */

object MLogMsg {

  @inline implicit def univEq: UnivEq[MLogMsg] = UnivEq.force

  /** Чтобы не было warning - structural types - need import, тут определён отдельный класс.
    * Без кучи warning'ов компилятора можно было заинлайнить это в builder (без конструктора класса). */
  final case class builder( url: Option[String] = None, stackTraceLen: Int ) {

    final class InnerBuilder[R]( classSimpleName: String, andThen: MLogMsg => R)(severity: LogSeverity) {
      /** Из-за наличия default-аргументов, эта функция объявленна в классе, не в функции. */
      def apply(errorMsg: ErrorMsg_t = null,  ex: Throwable = null,  msg: Any = null ): R = {
        val lm = MLogMsg(
          severity  = severity,
          from      = classSimpleName,
          code      = Option(errorMsg),
          message   = Option(msg).map(_.toString),
          exception = Option( MExceptionInfo.from(ex, stackTraceLen) ),
          url       = url,
        )
        andThen( lm )
      }
    }
    def apply[R](classSimpleName: String, andThen: MLogMsg => R) =
      new InnerBuilder(classSimpleName, andThen)(_)

  }


  /** Сериализация JSON. */
  implicit def logMsgJson: OFormat[MLogMsg] = (
    (__ \ "sev").format[LogSeverity] and
    (__ \ "src").format[String] and
    (__ \ "code").formatNullable[ErrorMsg_t] and
    (__ \ "msg").formatNullable[String] and
    (__ \ "ex").formatNullable[MExceptionInfo] and
    (__ \ "url").formatNullable[String]
  )(apply, unlift(unapply))


  def severity = GenLens[MLogMsg]( _.severity )
  def from = GenLens[MLogMsg]( _.from )
  def code = GenLens[MLogMsg]( _.code )
  def message = GenLens[MLogMsg]( _.message )
  def exception = GenLens[MLogMsg]( _.exception )
  def url = GenLens[MLogMsg]( _.url )


  def validate(logMsg: MLogMsg): ValidationNel[String, MLogMsg] = {
    (
      (Validation.success( logMsg.severity ): ValidationNel[String, LogSeverity]) |@|
      (Validation success StringUtil.strLimitLen( logMsg.from, 64) ) |@|
      ScalazUtil.liftNelOpt( logMsg.code ) { eMsgCode =>
        Validation.liftNel( eMsgCode )( !_.matches("[._a-zA-Z0-1-]{1,256}"), "code.invalid" )
      } |@|
      ScalazUtil.liftNelOpt( logMsg.message ) { logMessage =>
        Validation success StringUtil.strLimitLen( logMessage, maxLen = 512 )
      } |@|
      ScalazUtil.liftNelOpt( logMsg.exception )( MExceptionInfo.validate ) |@|
      ScalazUtil.liftNelOpt( logMsg.url ) { url =>
        Validation.success( StringUtil.strLimitLen( url, 256 ) )
      }
    )(apply)
  }

}

/** Класс с данными для логгирования. Он отправляется в каждый конкретный логгер.
  *
  * @param severity Важность.
  * @param from Откуда сообщение (класс).
  * @param code Код ошибки по списку кодов.
  * @param message Произвольное сообщение об ошибке.
  * @param exception Исключение, если есть.
  */
final case class MLogMsg(
                          severity      : LogSeverity,
                          from          : String,
                          code          : Option[ErrorMsg_t]        = None,
                          message       : Option[String]            = None,
                          exception     : Option[MExceptionInfo]    = None,
                          url           : Option[String]            = None,
                        ) {

  /** Рендер в строку, но без severity, fsmState, code. */
  def onlyMainText: String = {
    // Нааккумулировать данных для логгирования из модели logMsg в строку.
    var tokensAcc = List.empty[String]

    val d = HtmlConstants.SPACE
    val n = HtmlConstants.NEWLINE_UNIX.toString

    for (ex <- exception) {
      for (st <- ex.stackTrace.reverseIterator)
        tokensAcc = n :: st.mkString(n,n,n) :: tokensAcc
      for (msg <- ex.message)
        tokensAcc = d :: msg :: tokensAcc
      tokensAcc = d :: ex.getClass.getSimpleName :: tokensAcc
    }

    for (msg <- message)
      tokensAcc = n :: msg :: tokensAcc

    tokensAcc = from :: ":" :: d :: tokensAcc

    // Отрендерить в логи итог работы...
    tokensAcc.mkString
  }


  override def toString: ErrorMsg_t = {
    val sb = new StringBuilder(256)

    sb.append( severity )
      .append( HtmlConstants.SPACE )
      .append( from )
      .append( HtmlConstants.SPACE )

    for (c <- code)
      sb.append( c ).append( HtmlConstants.SPACE )

    for (msg <- message)
      sb.append(msg).append( HtmlConstants.SPACE )

    for (u <- url)
      sb.append( u )

    for (ex <- exception) {
      sb.append(HtmlConstants.NEWLINE_UNIX)
        .append( ex.className )

      for (exMsg <- ex.message)
        sb.append( HtmlConstants.SPACE )
          .append( exMsg )

      for (exStackTrace <- ex.stackTrace)
        sb.append( HtmlConstants.NEWLINE_UNIX )
          .append( exStackTrace )
    }

    sb.toString()
  }

}
