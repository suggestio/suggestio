package io.suggest.log

import java.time.temporal.ChronoUnit
import java.time.{Instant, OffsetDateTime, ZoneId}

import io.suggest.common.html.HtmlConstants
import io.suggest.err.MExceptionInfo
import io.suggest.msg.ErrorMsg_t
import io.suggest.scalaz.ScalazUtil
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{NonEmptyList, Validation, ValidationNel}
import scalaz.syntax.apply._
import io.suggest.dt.CommonDateTimeUtil.Implicits._

import scala.util.Try

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

    final class InnerBuilder[R]( classSimpleName: Option[String], andThen: MLogMsg => R)(severity: LogSeverity) {
      /** Из-за наличия default-аргументов, эта функция объявленна в классе, не в функции. */
      def apply(errorMsg: ErrorMsg_t = null,  ex: Throwable = null,  msg: Any = null ): R = {
        val lm = MLogMsg(
          severity  = severity,
          from      = classSimpleName,
          logMsg      = Option(errorMsg),
          message   = Option(msg).map(_.toString),
          exception = Option(ex).map( MExceptionInfo.from(_, stackTraceLen) ),
          url       = url,
        )
        andThen( lm )
      }
    }
    def apply[R](classSimpleName: Option[String], andThen: MLogMsg => R) =
      new InnerBuilder(classSimpleName, andThen)(_)

  }


  /** Сериализация JSON. */
  implicit def logMsgJson: OFormat[MLogMsg] = (
    (__ \ "sev").format[LogSeverity] and
    (__ \ "src").formatNullable[String] and
    (__ \ "code").formatNullable[ErrorMsg_t] and
    (__ \ "msg").formatNullable[String] and
    (__ \ "ex").formatNullable[MExceptionInfo] and
    (__ \ "url").formatNullable[String] and
    (__ \ "dt").formatNullable[Instant]
  )(apply, unlift(unapply))


  def severity = GenLens[MLogMsg]( _.severity )
  def from = GenLens[MLogMsg]( _.from )
  def code = GenLens[MLogMsg]( _.logMsg )
  def message = GenLens[MLogMsg]( _.message )
  def exception = GenLens[MLogMsg]( _.exception )
  def url = GenLens[MLogMsg]( _.url )
  def dateTime = GenLens[MLogMsg]( _.dateTime )


  def validate(logMsg: MLogMsg): ValidationNel[String, MLogMsg] = (
    Validation.success[NonEmptyList[String], LogSeverity]( logMsg.severity ) |@|
    ScalazUtil.liftNelOpt( logMsg.from ) { from =>
      Validation.success[NonEmptyList[String], String]( StringUtil.strLimitLen( from, 64) )
    } |@|
    ScalazUtil.liftNelOpt( logMsg.logMsg ) { eMsgCode =>
      // В регэксп внесены символы [*=], т.к. использование произвольных месседжей для логгирования требует "подсветить" сторку.
      Validation.liftNel( eMsgCode )( !_.matches("[._a-zA-Z0-1*=-]{1,256}"), "code.invalid: " + eMsgCode )
    } |@|
    ScalazUtil.liftNelOpt( logMsg.message ) { logMessage =>
      Validation.success[NonEmptyList[String], String]( StringUtil.strLimitLen( logMessage, maxLen = 512 ) )
    } |@|
    ScalazUtil.liftNelOpt( logMsg.exception )( MExceptionInfo.validate ) |@|
    ScalazUtil.liftNelOpt( logMsg.url ) { url =>
      Validation.success[NonEmptyList[String], String]( StringUtil.strLimitLen( url, 256 ) )
    } |@|
    ScalazUtil.liftNelOpt( logMsg.dateTime ) { dt0 =>
      // Возможные сдвиги в датах нормируем на +- 1 год.
      val now = Instant.now
      Validation.liftNel(dt0)(
        {dt =>
          val daysPerYear = 365
          dt.isBefore( now.minus(daysPerYear, ChronoUnit.DAYS) ) ||
          dt.isAfter( now.plus(daysPerYear, ChronoUnit.DAYS) )
        },
        "dt.invalid"
      )
    }
  )(apply)

}


/** Класс с данными для логгирования. Он отправляется в каждый конкретный логгер.
  *
  * @param severity Важность.
  * @param from Откуда сообщение (класс).
  * @param logMsg Код ошибки по списку кодов.
  * @param message Произвольное сообщение об ошибке.
  * @param exception Исключение, если есть.
  */
final case class MLogMsg(
                          severity      : LogSeverity,
                          from          : Option[String]            = None,
                          logMsg        : Option[ErrorMsg_t]        = None,
                          message       : Option[String]            = None,
                          exception     : Option[MExceptionInfo]    = None,
                          url           : Option[String]            = None,
                          dateTime      : Option[Instant]           = Some( Instant.now() )
                        ) {

  /** Рендер в строку, но без severity, fsmState, code. */
  def onlyMainText: String = {
    // Нааккумулировать данных для логгирования из модели logMsg в строку.
    var tokensAcc = List.empty[String]

    val d = HtmlConstants.SPACE
    val n = HtmlConstants.NEWLINE_UNIX.toString

    for (ex <- exception) {
      for (st <- ex.stackTrace.reverseIterator)
        tokensAcc = n :: st :: tokensAcc
      for (msg <- ex.message)
        tokensAcc = d :: msg :: tokensAcc
      tokensAcc = d :: ex.getClass.getSimpleName :: tokensAcc
    }

    for (msg <- message)
      tokensAcc = n :: StringUtil.strLimitLen(msg, 512) :: tokensAcc

    tokensAcc = d :: tokensAcc

    for (frm <- from)
      tokensAcc = frm :: ":" :: tokensAcc

    // Отрендерить в логи итог работы...
    tokensAcc.mkString
  }


  override def toString: ErrorMsg_t = {
    val sb = new StringBuilder( 1024 )

    val s = HtmlConstants.SPACE

    sb.append('[').append( severity ).append(']')
      .append( '\t' )

    for (frm <- from)
      sb.append( frm )
        .append( HtmlConstants.COLON )
        .append( s )

    for (dt <- dateTime)
      sb.append( '[' )
        .append(
          // Небезопасно вызывать из js, если нет комплекта вкомпиленных таймзон.
          // java.time.zone.ZoneRulesException: Unknown time-zone ID: Europe/Moscow
          Try( OffsetDateTime.ofInstant( dt, ZoneId.systemDefault() ) )
            .getOrElse( dt.getEpochSecond )
            .toString
        )
        .append( ']' ).append( s )

    for (c <- logMsg)
      sb.append( c )
        .append( s )

    for (msg <- message)
      sb.append("## ")
        .append( StringUtil.strLimitLen(msg, 512) )
        .append( s )

    for (u <- url)
      sb.append("|(")
        .append( u )
        .append(")")

    for (ex <- exception) {
      sb.append( HtmlConstants.NEWLINE_UNIX )
        .append( ex.className )

      for (exMsg <- ex.message)
        sb.append( HtmlConstants.COLON )
          .append( s )
          .append( exMsg )

      for (exStackTrace <- ex.stackTrace)
        sb.append( HtmlConstants.NEWLINE_UNIX )
          .append( exStackTrace )
    }

    sb.toString()
  }

}
