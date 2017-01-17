package io.suggest.sjs.common.bin

import java.nio.ByteBuffer

import io.suggest.bin.{ConvCodecs, IDataConv}

import scala.scalajs.js.typedarray.TypedArrayBuffer

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 10:32
  * Description: Утиль для поддержки base64.
  */
object EvoBase64JsUtil {

  /** js-only base64-декодер в рамках интерфейса конвертеров данных. */
  implicit case object EvoBase64JsDecoder extends IDataConv[String, ConvCodecs.Base64, ByteBuffer] {
    override def convert(base64: String): ByteBuffer = {
      val arr = JsBinaryUtil.base64DecToArr(base64)
      TypedArrayBuffer.wrap(arr.buffer)
    }
  }

}
