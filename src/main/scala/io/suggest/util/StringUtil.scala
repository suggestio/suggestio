package io.suggest.util

import annotation.tailrec
import scala.util.Random
import java.nio.ByteBuffer
import org.apache.commons.codec.binary.Base32

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.03.13 16:16
 * Description: Функции для строк.
 */

object StringUtil {

  val LONG_BYTESIZE = 8
  /** Ненужные padding-символы в конце base64 выхлопов. */
  val BASEN_PADDING_BYTE  = '='.toByte
  /** Если нулевые байты, то в начале будут дефисы в случае Base64+ordered. */
  val B32HEX_ZERO_BYTE = '0'.toByte

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


  def getB32Encoder = new Base32(true)
  def getLongBuffer = ByteBuffer.allocate(LONG_BYTESIZE)

  /**
   * Закондировать printable-символами long-значение так, чтобы оно имело кратчайший размер и чтобы соблюдалась
   * сортировка даже в закодированных данных.
   * b32hLc = Base32+HEX LowerCase
   * @param l Исходное long-значение.
   * @param buf Опциональный потоко-небезовасный буффер для хранения данных. Должен иметь длину хотя бы в 8 байт.
   *            Полезен для подавления мусора при массовом использовании.
   * @return Строка, содержащая только символы из URL_SAFE алфавита base32+HEX, относящиеся к значению числа.
   *         Строки можно сравнивать также как и исходные long-значения.
   */
  def longAsB32hLc(l: Long, buf: ByteBuffer = getLongBuffer, encoder: Base32 = getB32Encoder): String = {
    buf.putLong(l)
    val bytes32 = encoder.encode(buf.array())
    // Надо срезать с начала - все дефисы, с конца - все padding'и, т.е. '='
    val startIndex = basenPaddingIndex(0, bytes32, B32HEX_ZERO_BYTE, +1)
    val endIndex = basenPaddingIndex(bytes32.length - 1, bytes32, BASEN_PADDING_BYTE, -1)
    val length = endIndex - startIndex + 1
    bytea2lowerCase(bytes32, startIndex, length)
    // TODO Для подавления лишнего мусора следует вызывать toLowerCase прямо на отрезке массива, а не на строке.
    new String(bytes32, startIndex, length).toLowerCase
  }

  /** Пройти массив с одного края, пока встречается указанный байт. */
  private def basenPaddingIndex(inx: Int, bytes:Array[Byte], padding:Byte, step:Int): Int = {
    if (bytes(inx) == padding)
      basenPaddingIndex(inx + step, bytes, padding, step)
    else
      inx
  }

  private def bytea2lowerCase(bytea: Array[Byte], start: Int, length: Int) {
    if (length > 0) {
      val ch1 = Character.toLowerCase(bytea(start).toChar)
      bytea(start) = ch1.toByte
      bytea2lowerCase(bytea, start + 1, length - 1)
    }
  }

}
