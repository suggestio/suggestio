package com.momentjs

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
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

  def apply(): Moment = js.native
  def apply(str: String): Moment = js.native
  def apply(str: String, format: String | js.Array[String]): Moment = js.native
  def apply(str: String, format: String | js.Array[String], locale: String): Moment = js.native
  def apply(str: String, format: String | js.Array[String], strict: Boolean): Moment = js.native
  def apply(str: String, format: String | js.Array[String], locale: String, strict: Boolean): Moment = js.native
  def apply(array: js.Array[Int]): Moment = js.native
  def apply(opj: js.Dictionary[Int]): Moment = js.native
  def apply(jsDate: Date): Moment = js.native
  def apply(m: Moment): Moment = js.native

  def utc(): Moment = js.native
  def utc(n: Double): Moment = js.native
  def utc(ns: js.Array[Double]): Moment = js.native
  def utc(s: String): Moment = js.native
  def utc(s: String, format: String): Moment = js.native
  def utc(s: String, format: String | js.Array[String]): Moment = js.native
  def utc(s: String, format: String, x: String): Moment = js.native
  def utc(m: Moment): Moment = js.native
  def utc(d: Date): Moment = js.native

  def unix(n: Double): Moment = js.native

}


@js.native
class Moment extends js.Object

