package io.suggest.sjs.dom2

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.{ArrayBuffer, ArrayBufferView, Uint8Array}
import scala.scalajs.js.|

object TextDecoder {
  object Encoding {
    def UTF8 = "utf-8"
    def DEFAULT = UTF8
  }
}

/** @see [[https://developer.mozilla.org/en-US/docs/Web/API/TextDecoder]] */
@js.native
@JSGlobal
class TextDecoder(val encoding: String = js.native) extends js.Object {
  val fatal: Boolean = js.native
  val ignoreBOM: Boolean = js.native
  def decode(buffer: ArrayBuffer | ArrayBufferView = js.native,
             options: TextDecodeOptions = js.native): String = js.native
}

trait TextDecodeOptions extends js.Object {
  val stream: js.UndefOr[Boolean] = js.undefined
}


/** @see [[https://developer.mozilla.org/en-US/docs/Web/API/TextEncoder]] */
@js.native
@JSGlobal
class TextEncoder() extends js.Object {
  val encoding: String = js.native
  def encode(text: String): Uint8Array = js.native
  def encodeInto(text: String, intoArray: Uint8Array): TextEncoderEncodeIntoResult = js.native
}

@js.native
trait TextEncoderEncodeIntoResult extends js.Object {
  def read: Int = js.native
  def written: Int = js.native
}