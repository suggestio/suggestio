package io.suggest.model

import org.apache.hadoop.hbase.HColumnDescriptor
import HTableModel._
import org.apache.commons.codec.binary.Base64
import io.suggest.util.{UrlUtil, CryptoUtil}
import java.net.URL
import java.security.MessageDigest
import org.apache.hadoop.hbase.io.ImmutableBytesWritable

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
  val CF_TMP_IMGS     = "c"   // ВрЕменные превьюшки разных размеров сбрасываются сюда. Это относится к MUserImg.
  val CF_ORIGINALS    = "d"   // Оригиналы, большие и толстые оригиналы.

  // MImgThumb
  val Q_THUMB         = "t"   // Картинка-превьюшка. CF_THUMBS
  val Q_IMAGE_URL     = "u"   // Ссылка на исходную картинку, если есть. CF_METADATA
  // MUserImg
  val Q_IMG_META = "m"   // Сериализованный кусок датума MImgThumb. CF_METADATA
  val Q_USER_IMG_ORIG = "o"   // Бинарник, содержащий оригинал изображения. CF_ORIGINALS

  def CFs = Seq(CF_METADATA, CF_THUMBS, CF_TMP_IMGS, CF_ORIGINALS)

  /** Генератор дескрипторов CF'ок. */
  def getColumnDescriptor: PartialFunction[String, HColumnDescriptor] = {
    case cf @ (CF_METADATA | CF_THUMBS | CF_TMP_IMGS | CF_ORIGINALS) =>
      cfDescSimple(cf, 1)
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

}


trait MPictSubmodel {
  def HTABLE_NAME = MPict.HTABLE_NAME
  def HTABLE_NAME_BYTES = MPict.HTABLE_NAME_BYTES
}


case class ImgWithTimestamp(img: Array[Byte], timestamp: Long)

