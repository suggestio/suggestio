package com.momentjs

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 10:31
  * Description: API for momentjs Duration.
  */
@js.native
class Duration extends js.Object {

  def humanize(withSuffix: Boolean = js.native): String = js.native

  def milliseconds(): Int = js.native
  def asMilliseconds(): UnitVal_t = js.native
  def seconds(): Int = js.native
  def asSeconds(): Double = js.native
  def minutes(): Int = js.native
  def asMinutes(): Double = js.native
  def hours(): Int = js.native
  def asHours(): Double = js.native
  def days(): Int = js.native
  def asDays(): Double = js.native
  def weeks(): Int = js.native
  def asWeeks(): Double = js.native
  def months(): Int = js.native
  def asMonths(): Double = js.native
  def years(): Int = js.native
  def asYears(): Double = js.native

  def add(value: Double, unit: String = js.native): this.type = js.native
  def add(dur: Duration): this.type = js.native
  def add(changes: js.Dictionary[Double]): this.type = js.native

  def substract(value: Double, unit: String = js.native): this.type = js.native
  def substract(dur: Duration): this.type = js.native
  def substract(changes: js.Dictionary[Double]): this.type = js.native

  def as(unit: String): Double = js.native
  def get(unit: String): Int = js.native

  def toJSON(): js.Object = js.native

  def isDuration(v: js.Any = js.native): Boolean = js.native

}
