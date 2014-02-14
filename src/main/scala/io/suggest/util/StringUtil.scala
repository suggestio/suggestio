package io.suggest.util

import annotation.tailrec
import scala.util.Random
import java.nio.ByteBuffer
import net.iharder.Base64

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.03.13 16:16
 * Description: Функции для строк.
 */

object StringUtil {

  val LONG_BYTESIZE = 8
  /** Ненужные padding-символы в конце base64 выхлопов. */
  val B64_PADDING_BYTE  = '='.toByte
  /** Если нулевые байты, то в начале будут дефисы в случае Base64+ordered. */
  val B64_ORD_ZERO_BYTE = '-'.toByte

  private val rnd = new Random(System.currentTimeMillis())


  /**
   * Сгенерить случайны id-шник вида "64srGaf345TfQw34fa"
   * @param len Опциональная длина выходной строки.
   * @return Случайная строка.
   */
  def randomId(len:Int = 10) = 1 to len map {_ => randomIdChar} mkString


  /** Сгенерить случайный символ из диапазона 0-9 a-z A-Z
   * @return случайный alphanumeric символ.
   */
  @tailrec
  def randomIdChar : Char = rnd.nextPrintableChar() match {
    case c if c>='0' && c<='9' || c>='A' && c<='Z' || c>='a' && c<='z'  => c
    case _ => randomIdChar
  }

  /** Генерация одного случайного lower-case символа латиницы.
   * @return Символ от 'a' до 'z'.
   */
  @tailrec
  def randomIdLatLcChar: Char = rnd.nextPrintableChar() match {
    case c if c>='a' && c<='z'  => c
    case c if c>='A' && c<='Z'  => c.toLower
    case _                      => randomIdLatLcChar
  }

  /** Генерация случайно строки из латинских символов от 'a' до 'z'.
   * @param len Длина результирующей строки.
   * @return Случайная строка вида "asdftbhdb" длины len.
   */
  def randomIdLatLc(len: Int = 10) = 1 to len map { _ => randomIdLatLcChar } mkString


  /**
   * Закондировать printable-символами long-значение так, чтобы оно имело кратчайший размер и чтобы соблюдалась
   * сортировка.
   * @param l Исходное long-значение.
   * @param buf Опциональный потоко-небезовасный буффер для хранения данных. Должен иметь длину хотя бы в 8 байт.
   *            Полезен для подавления мусора при массовом использовании.
   * @return Строка, содержащая только символы из URL_SAFE алфавита base64, относящиеся к значению числа.
   *         Строки можно сравнивать также как и исходные long-значения.
   */
  def longAsBase64ordered(l: Long, buf: ByteBuffer = ByteBuffer.allocate(LONG_BYTESIZE)): String = {
    buf.putLong(l)
    val bytes64 = Base64.encodeBytesToBytes(buf.array(), 0, LONG_BYTESIZE, Base64.ORDERED)
    // Надо срезать с начала - все дефисы, с конца - все padding'и, т.е. '='
    val startIndex = b64PaddingIndex(0, bytes64, B64_ORD_ZERO_BYTE, +1)
    val endIndex = b64PaddingIndex(bytes64.length - 1, bytes64, B64_PADDING_BYTE, -1)
    val length = endIndex - startIndex
    new String(bytes64, startIndex, length)
  }

  private def b64PaddingIndex(inx: Int, bytes:Array[Byte], padding:Byte, step:Int): Int = {
    if (bytes(inx) == padding)
      b64PaddingIndex(inx + step, bytes, padding, step)
    else
      inx
  }

}
