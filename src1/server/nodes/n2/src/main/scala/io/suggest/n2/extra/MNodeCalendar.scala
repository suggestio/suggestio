package io.suggest.n2.extra

import io.suggest.cal.m.MCalType
import io.suggest.es.{IEsMappingProps, MappingDsl}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Calendar inside calendar-type node used forbilling tariffs calculations.
  * Here jollyday calendar spec is stored. Decoded and parsed by jollyday, when needs.
  */

object MNodeCalendar extends IEsMappingProps {

  /** Elasticsearch field names. */
  object Fields {
    final def DATA_FN       = "data"
    final def CAL_TYPE_FN   = "calType"
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.DATA_FN       -> FText.notIndexedJs,
      F.CAL_TYPE_FN   -> FKeyWord.indexedJs,
    )
  }

  implicit def nodeCalendarJson: OFormat[MNodeCalendar] = {
    val F = Fields
    (
      (__ \ F.CAL_TYPE_FN).format[MCalType] and
      (__ \ F.DATA_FN).format[String]
    )( apply, unlift(unapply) )
  }


  @inline implicit def univEq: UnivEq[MNodeCalendar] = UnivEq.derive

  def calType = GenLens[MNodeCalendar](_.calType)
  def data = GenLens[MNodeCalendar](_.data)

}


/** Calendar (jollyday) payload.
  *
  * @param calType Internal s.io type of calendar.
  * @param data Calendar spec.
  */
case class MNodeCalendar(
                          calType     : MCalType,
                          data        : String,
                        )
