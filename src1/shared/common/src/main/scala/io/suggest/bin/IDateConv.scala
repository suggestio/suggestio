package io.suggest.bin

import java.nio.ByteBuffer

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.12.16 17:17
  */

/** Интерфейс абстрактного конвертера данных из одного типа в другой.
  *
  * @tparam Src Тип исходных данных.
  * @tparam Codec Маркер-тип алгоритма преобразования.
  * @tparam Dst Тип выходных данных.
  */
trait IDataConv[Src, Codec <: IConvCodec, Dst] {

  def convert(from: Src): Dst

}


/** Кодек -- интерфейс-маркер. */
sealed trait IConvCodec

/** Compile-time маркеры типов конвертации. */
object ConvCodecs {

  /** Маркер-тип заглушки вместо реальной конвертации. */
  sealed trait Dummy extends IConvCodec

  /** Тип-маркер для base64-кодирования. */
  sealed trait Base64 extends IConvCodec

}


/** Всякая утиль для IDataConv свалена сюда. */
object IDataConv {

  /** Dummy-конвертер как тривиальный пример реализации конвертера. */
  implicit def DummyDataConv[T]: IDataConv[T,ConvCodecs.Dummy,T] = {
    new IDataConv[T, ConvCodecs.Dummy, T] {
      override def convert(from: T): T = from
    }
  }


  /**
    * Конвертация из ByteBuffer в Array[Byte].
    * byte order тут влиять не должен, ибо только little-endian по смыслу массива.
    */
  implicit object ByteBuf2ByteArrConv extends IDataConv[ByteBuffer, ConvCodecs.Dummy, Array[Byte]] {
    override def convert(bytes: ByteBuffer): Array[Byte] = {
      val data = Array.ofDim[Byte](bytes.remaining())
      bytes.get(data)
      data
    }
  }

}

