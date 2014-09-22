package io.suggest.util

import java.io.{DataInputStream, ByteArrayInputStream, DataOutputStream, ByteArrayOutputStream}
import java.util.UUID
import org.apache.commons.codec.binary.Base64

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.14 17:06
 * Description: Статическая утилья для работы с UUID.
 * Код и тесты вынесены из web21/util.stat.StatUtil.
 */
object UuidUtil {


  /** Конвертануть uuid в набор байт. */
  def uuidToBytes(uuid: UUID = UUID.randomUUID()): Array[Byte] = {
    val baos = new ByteArrayOutputStream(16)
    val os = new DataOutputStream(baos)
    os.writeLong(uuid.getMostSignificantBits)
    os.writeLong(uuid.getLeastSignificantBits)
    os.flush()
    baos.toByteArray
  }

  /** Завернуть uuid в base64. */
  def uuidToBase64(uuid: UUID): String = {
    val uuidBytes = uuidToBytes(uuid)
    Base64.encodeBase64URLSafeString(uuidBytes)
  }

  /** Декодировать uuid, закодированный в base64. */
  def base64ToUuid(b64s: String): UUID = {
    val bytes = Base64.decodeBase64(b64s)
    bytesToUuid(bytes)
  }


  def bytesToUuid(data: Array[Byte], start: Int = 0, len: Int = 16): UUID = {
    val bais = new ByteArrayInputStream(data, start, len)
    val is = new DataInputStream(bais)
    new UUID(is.readLong(), is.readLong())
  }


}
