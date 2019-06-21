package io.suggest.streams

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.charset.StandardCharsets

import org.apache.commons.io.IOUtils

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.06.19 16:44
  * Description: Внутренняя утиль для взаимодействия c java.io.*Stream*.
  */
object JioStreamsUtil {

  /** В случае, когда требуется Input/OutputStream, а нужно работать с обычными строками,
    * на помощь приходит эта функция.
    *
    * @param input Входной текст.
    * @param outputSizeInit Начальный размер аккамулятора BAOS.
    * @param f Функция, получающая на вход два стрима.
    * @return Строка на выходе.
    */
  def stringIo[T: IJioBaosToData](input: String, outputSizeInit: Int = 128)
                                 (f: (InputStream, ByteArrayOutputStream) => _): T = {
    val bais = IOUtils.toInputStream(input, StandardCharsets.UTF_8)
    val baos = new ByteArrayOutputStream( outputSizeInit )
    try {
      f(bais, baos)
      implicitly[IJioBaosToData[T]]
        .toData( baos )
    } finally {
      // Это всё не нужно, но вдруг однажды станет нужно:
      bais.close()
      baos.close()
    }
  }

}


/** Интерфейс для typeclass'ов преобразования baos в выходные данные. */
trait IJioBaosToData[T] {
  def toData(baos: ByteArrayOutputStream): T
}
object IJioBaosToData {

  implicit def baosToSelf: IJioBaosToData[ByteArrayOutputStream] = {
    new IJioBaosToData[ByteArrayOutputStream] {
      override def toData(baos: ByteArrayOutputStream) = baos
    }
  }

  implicit def baosToString: IJioBaosToData[String] = {
    new IJioBaosToData[String] {
      override def toData(baos: ByteArrayOutputStream) = baos.toString
    }
  }

  implicit def baosToBytea: IJioBaosToData[Array[Byte]] = {
    new IJioBaosToData[Array[Byte]] {
      override def toData(baos: ByteArrayOutputStream) = baos.toByteArray
    }
  }

}
