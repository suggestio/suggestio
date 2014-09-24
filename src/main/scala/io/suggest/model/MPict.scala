package io.suggest.model

import org.apache.hadoop.hbase.HColumnDescriptor
import HTableModel._
import org.apache.commons.codec.binary.Base64
import io.suggest.util.{UrlUtil, CryptoUtil}
import java.net.URL
import java.security.MessageDigest
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import java.util.Random
import scala.concurrent.Future
import SioHBaseAsyncClient._
import org.hbase.async.DeleteRequest

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.10.13 15:23
 * Description: Таблица для хранения картинок.
 */
object MPict extends HTableModel {

  val HTABLE_NAME = "pict"

  val CF_METADATA     = "a"   // Метаданные картинок. Маленькие, с переменным доступом.
  val CF_THUMBS       = "b"   // Превьюшки к картинкам. Имеют регулярный доступ.
  val CF_ORIGINALS    = "c"   // Оригиналы, большие и толстые оригиналы.

  // MImgThumb
  val Q_THUMB         = "t"   // Картинка-превьюшка. CF_THUMBS
  val Q_IMAGE_URL     = "u"   // Ссылка на исходную картинку, если есть. CF_METADATA
  // MUserImg
  val Q_USER_IMG_ORIG = "o"   // Бинарник, содержащий оригинал изображения. CF_ORIGINALS

  private val rnd = {
    val _rnd = new Random
    _rnd.setSeed(System.currentTimeMillis)
    _rnd
  }

  def CFs = Seq(CF_METADATA, CF_THUMBS, CF_ORIGINALS)

  /** Генератор дескрипторов CF'ок. */
  def getColumnDescriptor: PartialFunction[String, HColumnDescriptor] = {
    case cf @ (CF_METADATA | CF_THUMBS | CF_ORIGINALS) =>
      cfDescSimple(cf, 1)
  }
  
  /** Длина рандомного id картинки. У sha1 длина 20, желательно чтобы длина рандома была иной. */
  val RANDOM_ID_BYTELEN = 18

  /** Генерация рандомного id картинки. */
  def randomId: Array[Byte] = {
    val b = Array.ofDim[Byte](RANDOM_ID_BYTELEN)
    rnd.nextBytes(b)
    b
  }

  /** Тип контекста кешируемого генератора. Используется для функций с внешним кешируемым генератором img id. */
  type ImgIdGen_t = MessageDigest

  /** Сгенерить id картинки на основе её URL. Используется одноразовый MessageDigest. */
  def imgUrl2id(url: String): Array[Byte] = imgUrl2id(new URL(url))
  def imgUrl2id(url: URL): Array[Byte]    = CryptoUtil.sha1(UrlUtil.url2rowKey(url))

  /** Сгенерить id картинки с помощью закешированного хешера, полученного через getIdGen.
    * Такое полезно при массовом выставлении id для снижения размеров мусора до минимума. */
  def imgUrl2idGen(url: String, gen:ImgIdGen_t): Array[Byte] = imgUrl2idGen(new URL(url), gen)
  def imgUrl2idGen(url: URL,    gen:ImgIdGen_t): Array[Byte] = gen.digest(UrlUtil.url2rowKey(url).getBytes)

  def getIdGen: ImgIdGen_t = CryptoUtil.sha1Digest

  // TODO заюзать implicit?
  def idBin2Str(id: Array[Byte]) = Base64 encodeBase64URLSafeString id
  def idStr2Bin(idStr: String)   = Base64 decodeBase64 idStr

  def isStrIdValid(idStr: String): Boolean = {
    try {
      idStr2Bin(idStr)
      true
    } catch {
      case ex: Exception => false
    }
  }

  // Десериализаторы для работы напрямую с полями кортежа
  def deserializeId(id: AnyRef) = deserializeBytes(id)
  def deserializeThumb(thumb: AnyRef) = deserializeBytes(thumb)

  // Сериализаторы для работы напрямую с полями кортежа
  def serializeId(id: AnyRef) = serializeBytes(id)
  def serializeThumb(thumb: AnyRef) = serializeBytes(thumb)
  protected val serializeBytes: PartialFunction[AnyRef, ImmutableBytesWritable] = {
    case null                         => null
    case ar: Array[Byte]              => new ImmutableBytesWritable(ar)
    case ibw: ImmutableBytesWritable  => ibw
  }

  /** Десериализация значения в поле ID в бинарь. */
  val deserializeBytes: PartialFunction[AnyRef, Array[Byte]] = {
    case ibw: ImmutableBytesWritable => ibw.get   // 2013.dec.05: Оставлено для совместимости. Можно удалить перед первым релизом.
    case a: Array[Byte] => a
    case null  => null
    case other => throw new IllegalArgumentException("unexpected input[" + other.getClass.getName + "]: " + other)
  }

  /**
   * Удалить весь ряд целиком из таблицы.
   * @param idStr строковой id ряда
   * @return Фьючерс для синхронизации.
   */
  def deleteFully(idStr: String): Future[_] = deleteFully(idStr2Bin(idStr))

  /**
   * Удалить весь ряд с картинкой из таблицы.
   * @param id id ряда.
   * @return Фьючерс для синхронизации.
   */
  def deleteFully(id: Array[Byte]): Future[_] = {
    val delReq = new DeleteRequest(HTABLE_NAME_BYTES, id)
    ahclient.delete(delReq)
  }

}


trait MPictSubmodel {
  def HTABLE_NAME = MPict.HTABLE_NAME
  def HTABLE_NAME_BYTES = MPict.HTABLE_NAME_BYTES
}


trait ImgWithTimestamp {
  def imgBytes: Array[Byte]
  def timestampMs: Long
}


/** JMX MBean интерфейс */
trait MPictJmxMBean extends HBaseModelJMXBeanCommon

/** JMX MBean реализация. */
final class MPictJmx extends HBaseModelJMXBase with MPictJmxMBean {
  def companion = MPict
}
