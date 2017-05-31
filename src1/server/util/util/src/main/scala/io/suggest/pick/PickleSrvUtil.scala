package io.suggest.pick

import java.nio.ByteBuffer
import java.util.Base64
import io.suggest.bin.IDataConv.ByteBuf2ByteArrConv

import io.suggest.bin.{ConvCodecs, IDataConv}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.12.16 17:03
  * Description: Инжектируемая утиль для поддержки клиент-серверной сериализации и десериализации данных.
  */
object PickleSrvUtil {

  /** Конвертер из Array[Byte] в самую обычную base64-строку. */
  // scala.js пока не содежит java.util.Base64, поэтому данный код живёт на уровне util, а не common.
  implicit case object Base64ByteArrEncoder extends IDataConv[Array[Byte], ConvCodecs.Base64, String] {
    override def convert(data: Array[Byte]): String = {
      Base64.getEncoder
        .encodeToString(data)
    }
  }


  /** Комбо-конвертер, кодирующий из byte buffer'а в самую стандартную обычную base64-строку. */
  // TODO Нужно, чтобы scalac сам как-то генерил аналогичный код, т.к. он немного умеет implicit chain в implicit conversions.
  implicit object Base64ByteBufEncoder extends IDataConv[ByteBuffer, ConvCodecs.Base64, String] {
    override def convert(bytes: ByteBuffer): String = {
      Base64ByteArrEncoder.convert(
        ByteBuf2ByteArrConv.convert(bytes)
      )
    }
  }


  /** Десериализация base64-данных из String в ByteBuffer. */
  implicit object Base64ByteBufDecoder extends IDataConv[String, ConvCodecs.Base64, ByteBuffer] {
    override def convert(from: String): ByteBuffer = {
      val barr = Base64.getDecoder
        .decode( from )
      ByteBuffer.wrap(barr)
    }
  }

}
