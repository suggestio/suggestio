package io.suggest.util

import java.security.MessageDigest
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.03.13 10:31
 * Description:
 */
object CryptoUtil {

  private val HASH_LEN_MD5_HEX = 32   // == len(MD4)
  private val HASH_LEN_SHA1_HEX = 40  // == len(RIPEMD-160)
  private val HASH_LEN_SHA224_HEX = 56
  private val HASH_LEN_SHA256_HEX = 65
  private val HASH_LEN_SHA384_HEX = 96
  private val HASH_LEN_SHA512_HEX = 128
  private val HASH_LENS = Set(HASH_LEN_MD5_HEX, HASH_LEN_SHA1_HEX, HASH_LEN_SHA224_HEX, HASH_LEN_SHA256_HEX, HASH_LEN_SHA384_HEX, HASH_LEN_SHA512_HEX)
  private val HASH_PATTERN = "(?i)[a-f0-9]+".r.pattern

  def isHexHash(qv:String) : Boolean = {
    HASH_LENS.contains(qv.length) && HASH_PATTERN.matcher(qv).matches
  }

  def md5Digest = MessageDigest.getInstance("md5")

  /** Сгенерить md5-байты на основе входной строки. */
  def md5(str: String): Array[Byte]    = md5(str.getBytes)
  def md5(a: Array[Byte]): Array[Byte] = md5Digest.digest(a)

  /**
   * Сделать из обычной строки md5-хеш строку капслоком.
   * @param str строка для превращения в md5-хеш.
   * @return
   */
  def md5hex(str:String) : String = {
    val digest = md5(str)
    // TODO Вместо 16-ричного кодирования лучше использовать весь алфавит + алфавит в upper-case.
    //      Тогда хеши будут ощутимо короче. В эрланге использовалось 36-ричное кодирование: [0-9a-z]
    HexBin.encode(digest)
  }


  def sha1Digest = MessageDigest.getInstance("sha-1")

  def sha1(str: String): Array[Byte]    = sha1(str.getBytes)
  def sha1(a: Array[Byte]): Array[Byte] = sha1Digest.digest(a)

}
