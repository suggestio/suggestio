package util.stat

import java.io.ByteArrayOutputStream
import java.{util => ju}
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import play.api.Play.{current, configuration}
import io.suggest.util.UuidUtil._

import org.apache.commons.codec.binary.Base64
import play.api.mvc.{Cookie, Result, RequestHeader}
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.14 18:33
 * Description: Утиль для сбора разной статистики. Для мониторинга юзеров используются куки,
 * живущие до конца сессии. Они содержат некоторый id юзера и HMAC для защиты suggest.io от проблем с подставными
 * куками.
 * 2014.sep.22: uuid-конвертеры вынесены в sioutil/io.suggest.util.UuidUtil.
 */
object StatUtil extends PlayMacroLogsImpl {

  import LOGGER._

  /** Строка секрета для генерации подписи к кукису.
    * Длина ключа HMAC 32 байта, поэтому ASCII-строка в 64 байта будет вполне ок. */
  private val SECRET_UID_STR = configuration.getString("stat.uid.cookie.secret") getOrElse {
    """zPuQ=Zztfy@8Ic'ga9KK8Dj4{@MveN|2M<eV_g_yZ`<&-F|vMzw\\Q6vJPZe)5f-^"""
  }

  /** Названия куки, которое содержит id клиента для сбора статистики. */
  val STAT_UID_COOKIE_NAME = configuration.getString("stat.uid.cookie.name") getOrElse "statid"

  /** maxAge для куки. По умолчанию - до конца сессии. */
  val STAT_UID_COOKIE_MAXAGE_SECONDS: Option[Int] = configuration.getInt("stat.uid.cookie.maxAge.seconds")

  /** Используемый крипто-провайдера. Дефолтовый в частности. */
  private val CRYPTO_PROVIDER_STR = configuration.getString("stat.uid.crypto.provider")

  /** Длина uid в байтах. Т.к. используются uuid, то тут константа как бы. */
  val UID_BYTE_LEN = 16

  /** Длина выхлопа MAC в байтах. */
  val MAC_BYTE_LEN = 20

  private def getMac = {
    val mac = CRYPTO_PROVIDER_STR.map(p => Mac.getInstance("HmacSHA1", p)).getOrElse(Mac.getInstance("HmacSHA1"))
    mac.init(new SecretKeySpec(SECRET_UID_STR.getBytes, "HmacSHA1"))
    mac
  }
  
  /** Сборка нового значения для кукиса, содержащего id юзера, используемый для учёта статистики. */
  def mkUidCookieValue(uuid: UUID = UUID.randomUUID()): String = {
    val mac = getMac
    val uidBytes = uuidToBytes(uuid)
    val macBytes = mac.doFinal(uidBytes)
    val macLen = macBytes.length
    assert(macLen == MAC_BYTE_LEN, s"MAC length must be $MAC_BYTE_LEN bytes, but $macLen bytes found.")
    serializeUidAndMac(uidBytes, macBytes)
  }

  /** Сериализовать uid и MAC-ЭЦП в URL-safe строку.
    * @param uid Байты, содержащие id юзера.
    * @param mac Байты MAC-digest'а.
    * @return Строка, пригодная для отправки в значение кукиса.
    */
  def serializeUidAndMac(uid: Array[Byte], mac: Array[Byte]): String = {
    val uidLen = uid.length
    assert(uidLen == UID_BYTE_LEN, "UID must be 16 bytes length.")
    val baos = new ByteArrayOutputStream(uidLen + mac.length)
    baos.write(uid)
    baos.write(mac)
    Base64.encodeBase64URLSafeString(baos.toByteArray)
  }

  /** Десериализация значения кукиса, созданного через mkUidCookieValue().
    * @param s Строка со значением кукиса.
    * @return UUID если всё ок. Или былинный отказ, если не ок.
    */
  def deserializeCookieValue(s: String): Option[UUID] = {
    lazy val logPrefix = s"deserialize($s): "
    try {
      val bytes = Base64.decodeBase64(s)
      val expectedLen = UID_BYTE_LEN + MAC_BYTE_LEN
      if (bytes.length != expectedLen) {
        // Длина не совпадает.
        warn(s"${logPrefix}Unexpected base64 data length: ${bytes.length}, expected $expectedLen bytes.")
        None
      } else {
        // Проверяем значение MAC
        val mac = getMac
        mac.update(bytes, 0, UID_BYTE_LEN)
        val macBytesExpected = mac.doFinal()
        val macBytesReal = ju.Arrays.copyOfRange(bytes, UID_BYTE_LEN, expectedLen)
        if ( ju.Arrays.equals(macBytesExpected, macBytesReal) ) {
          // Десериализовать UUID
          Some(bytesToUuid(bytes, 0, len = UID_BYTE_LEN))

        } else {
          warn(s"${logPrefix}MAC invalid:\n real[${macBytesReal.length}] = ${formatArray(macBytesReal)}\n expected[${macBytesExpected.length}] = ${formatArray(macBytesExpected)}\n invalid_uuid = ${bytesToUuid(bytes, 0, len = UID_BYTE_LEN)}")
          None
        }

      }
    } catch {
      case ex: Exception =>
        warn(s"${logPrefix}Failed to decode uid", ex)
        None
    }
  }

  /** Отрендерить byte-array в виде HEX-строки. */
  private def formatArray(a: Array[Byte]): String = {
    a.map("%02X" format _).mkString
  }

  /** Распаковать из запроса содержимое stat uid. */
  def getFromRequest(implicit request: RequestHeader): Option[UUID] = {
    request.cookies
      .get(STAT_UID_COOKIE_NAME)
      .flatMap { cookie => deserializeCookieValue(cookie.value) }
  }

  /** Есть ли в реквесте хоть какой-то кукис, относящийся к stat uid? */
  def requestHasAnyCookie(implicit request: RequestHeader): Boolean = {
    request.cookies
      .exists(_.name == STAT_UID_COOKIE_NAME)
  }
  
  /** Добавить stat-куку в результат запроса. */
  def resultWithStatCookie(result: Result)(implicit request: RequestHeader): Result = {
    if (StatUtil.requestHasAnyCookie) {
      result
    } else {
      val statUid = mkUidCookieValue()
      resultWithStatCookie(statUid)(result)
    }
  }

  /** Добавить указанную stat-куку в результат запроса. */
  def resultWithStatCookie(statUid: String)(result: Result): Result = {
    val statCookie = Cookie(
      name = STAT_UID_COOKIE_NAME,
      value = statUid,
      maxAge = STAT_UID_COOKIE_MAXAGE_SECONDS,
      httpOnly = true
    )
    result.withCookies(statCookie)
  }

}
