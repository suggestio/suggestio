package io.suggest.text.util

import java.security.MessageDigest

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.03.13 10:31
 * Description: Выявление хешей в текстах/строках.
 */
object TextHashUtil {

  def HASH_LEN_MD5_HEX = 32   // == len(MD4)
  def HASH_LEN_SHA1_HEX = 40  // == len(RIPEMD-160)
  def HASH_LEN_SHA224_HEX = 56
  def HASH_LEN_SHA256_HEX = 65
  def HASH_LEN_SHA384_HEX = 96
  def HASH_LEN_SHA512_HEX = 128
  def HASH_LENS = Set(HASH_LEN_MD5_HEX, HASH_LEN_SHA1_HEX, HASH_LEN_SHA224_HEX, HASH_LEN_SHA256_HEX, HASH_LEN_SHA384_HEX, HASH_LEN_SHA512_HEX)
  def HASH_PATTERN = "(?i)[a-f0-9]+".r.pattern

  def isHexHash(qv:String) : Boolean = {
    HASH_LENS.contains(qv.length) && HASH_PATTERN.matcher(qv).matches
  }

  // Делаем эталонные экземпляры хеш-генераторов в конструкторе.
  private def _sha1digestSrc = MessageDigest.getInstance("sha-1").clone().asInstanceOf[MessageDigest]
  _sha1digestSrc.reset()

  private def _md5digestSrc = MessageDigest.getInstance("md5").clone().asInstanceOf[MessageDigest]
  _md5digestSrc.reset()

  def md5Digest = _md5digestSrc.clone().asInstanceOf[MessageDigest]

  /** Сгенерить md5-байты на основе входной строки. */
  def md5(str: String): Array[Byte]    = md5(str.getBytes)
  def md5(a: Array[Byte]): Array[Byte] = md5Digest.digest(a)


  def toHex(digest: Array[Byte]): String = {
    HexBin.encode(digest)
  }

  def sha1Digest = _sha1digestSrc.clone().asInstanceOf[MessageDigest]

  def sha1(str: String): Array[Byte]    = sha1(str.getBytes)
  def sha1(a: Array[Byte]): Array[Byte] = sha1Digest.digest(a)

}
