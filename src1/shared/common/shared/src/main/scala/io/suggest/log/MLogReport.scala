package io.suggest.log

import io.suggest.scalaz.ScalazUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.ValidationNel
import scalaz.std.list._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.05.2020 17:42
  * Description: JSON-модель отчёта remote-лога.
  */
object MLogReport {

  @inline implicit def univEq: UnivEq[MLogReport] = UnivEq.derive

  implicit def logReportJson: OFormat[MLogReport] = {
    (__ \ "v1")
      .format[List[MLogMsg]]
      .inmap[MLogReport]( apply, _.msgs )
  }

  def msgs = GenLens[MLogReport]( _.msgs )

  def validate(logReport: MLogReport): ValidationNel[String, MLogReport] = {
    ScalazUtil
      .validateAll( logReport.msgs )( MLogMsg.validate(_).map(_ :: Nil) )
      .map( apply )
  }

}


/** Модель-контейнер лог-отчёта с клиента.
  *
  * @param msgs Список лог-сообщений.
  */
final case class MLogReport(
                             msgs         : List[MLogMsg],
                           )
