package com.momentjs

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSImport}
import scala.scalajs.js.{Date, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.17 22:26
  * Description: Moment.js API.
  */
@JSImport("moment", JSImport.Namespace)
@js.native
object Moment extends js.Object {

  val CUSTOM_FORMAT: String = js.native
  val ISO_8601: String = js.native

  // parse local time
  def apply(): Moment = js.native
  def apply(str: String): Moment = js.native
  def apply(str: String, format: String | js.Array[String]): Moment = js.native
  def apply(str: String, format: String | js.Array[String], locale: String): Moment = js.native
  def apply(str: String, format: String | js.Array[String], strict: Boolean): Moment = js.native
  def apply(str: String, format: String | js.Array[String], locale: String, strict: Boolean): Moment = js.native
  def apply(array: js.Array[UnitVal_t]): Moment = js.native
  def apply(opj: js.Dictionary[js.Any]): Moment = js.native
  def apply(opj: js.Object): Moment = js.native   // same as js.Dictionary[Number]?
  def apply(jsDate: Date): Moment = js.native
  def apply(m: Moment): Moment = js.native

  // parse UTC time
  def utc(): Moment = js.native
  def utc(n: Double): Moment = js.native
  def utc(ns: js.Array[UnitVal_t]): Moment = js.native
  def utc(s: String): Moment = js.native
  def utc(s: String, format: String): Moment = js.native
  def utc(s: String, format: String | js.Array[String]): Moment = js.native
  def utc(s: String, format: String, x: String): Moment = js.native
  def utc(m: Moment): Moment = js.native
  def utc(d: Date): Moment = js.native

  // Parse UNIX timestamp
  def unix(n: Double): Moment = js.native

  // Parse timezoned time
  def parseZone(zone: String): Moment = js.native

  def max(moments: Moment*): Moment = js.native
  def max(moments: js.Array[Moment]): Moment = js.native

  def min(moments: Moment*): Moment = js.native
  def min(moments: js.Array[Moment]): Moment = js.native

  def duration(value: UnitVal_t, unit: String): Duration = js.native
  def duration(value: UnitVal_t): Duration = js.native
  def duration(value: js.Dictionary[UnitVal_t]): Duration = js.native
  def duration(value: String): Duration = js.native

  def normalizeUnits(unit: String): String = js.native

  def invalid(data: js.Object): Moment = js.native

  /** Set locale globally. */
  def locale(localeName: String): Unit = js.native

}


@JSGlobal
@js.native
class Moment extends js.Object {

  override def clone(): Moment = js.native

  def isValid(): Boolean = js.native

  /** @param unit see [[Units]]. */
  def isValidAt(unit: Int): Boolean = js.native

  def parsingFlags(): ParsingFlags = js.native

  def creationData(): CreationData = js.native

  def diff(m: Moment): Moment = js.native // TODO return value?

  def format(): String = js.native
  def format(fmt: String): String = js.native

  // Get/set. Setters mutate current instance: they return this.type (so mutations are explicit for user).
  def millisecond(): UnitVal_t = js.native
  def millisecond(ms: UnitVal_t): this.type = js.native

  def second(): UnitVal_t = js.native
  def second(s: UnitVal_t): this.type = js.native
  def seconds(): UnitVal_t = js.native
  def seconds(s: UnitVal_t): this.type = js.native

  def minute(): UnitVal_t = js.native
  def minute(m: UnitVal_t): this.type = js.native

  def hour(): UnitVal_t = js.native
  def hour(h: UnitVal_t): this.type = js.native

  // Day of month get/set.
  def date(): Int = js.native
  def date(dayOfMonth: Int): this.type = js.native

  // Day of week
  def day(): Int = js.native
  def day(dow: Int | String): this.type = js.native

  def weekday(): Int = js.native
  def weekday(wd: Int): this.type = js.native

  def isoWeekday(): Int = js.native
  def isoWeekday(iwd: Int | String): this.type = js.native

  def dayOfYear(): Int = js.native
  def dayOfYear(doy: Int): this.type = js.native

  def week(): Int = js.native
  def week(w: Int): this.type = js.native

  def isoWeek(): Int = js.native
  def isoWeek(w: Int): this.type = js.native

  def month(): Int = js.native
  def month(m: Int | String): this.type = js.native

  def quarter(): Int = js.native
  def quarter(q: Int): this.type = js.native

  def year(): Int = js.native
  def year(y: Int): this.type = js.native

  def weekYear(): Int = js.native
  def weekYear(wy: Int): Int = js.native

  def isoWeekYear(): Int = js.native
  def isoWeekYear(wy: Int): this.type = js.native

  def weeksInYear(): Int = js.native

  def isoWeeksInYear(): Int = js.native

  /** @param unit see [[Units]]. */
  def get(unit: String): UnitVal_t = js.native
  /** @param unit see [[Units]]. */
  def set(unit: String, value: UnitVal_t): this.type = js.native
  def set(changes: js.Dictionary[UnitVal_t]): this.type = js.native

  def add(m: Moment): Moment = js.native
  def add(value: UnitVal_t, unit: String): this.type = js.native
  def add(changes: js.Dictionary[UnitVal_t]): this.type = js.native
  def add(dur: Duration): this.type = js.native

  def substract(m: Moment): Moment = js.native
  def substract(value: UnitVal_t, unit: String): this.type = js.native
  def substract(changes: js.Dictionary[UnitVal_t]): this.type = js.native
  def substract(dur: Duration): this.type = js.native

}









