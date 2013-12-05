package io.suggest.model

import SioHBaseAsyncClient._
import org.hbase.async.GetRequest
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.{CascadingFieldNamer, SerialUtil, CryptoUtil, UrlUtil}
import java.net.URL
import com.scaleunlimited.cascading.BaseDatum
import cascading.tuple.{Tuple, Fields, TupleEntry}
import io.suggest.util.SerialUtil._
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.commons.codec.binary.Base64

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.10.13 14:56
 * Description: Модель доступа к превьюшкам-картинкам, которые исползуются в поисковой выдаче.
 * HBase-Cascading-схема осуществляет запись данных и лежит в сорцах кравлера вместе с остальной MR-логикой.
 * Ключ - md5(imageURL). Qualifier = dkey. CF=const
 *
 * 2013.dec.05: Статические и динамические части этой модели разбиты для дедубликации кода с ImgFetched-датумами siobix.
 */
object MImgThumb extends MImgThumbStaticT with MPictSubmodel {

  /**
   * Прочитать по hex id и dkey.
   * @param idStr Ключ ряда в виде hex-строки.
   * @return Фьючерс с опциональным результатом.
   */
  def getById(idStr: String)(implicit ec:ExecutionContext): Future[Option[MImgThumb]] = getById(idStr2Bin(idStr))

  /**
   * Прочитать по бинарном id и dkey.
   * @param idBin Бинарный id (ключ ряда, row key).
   * @return Фьючерс с опциональным результом.
   */
  def getById(idBin: Array[Byte])(implicit ec:ExecutionContext): Future[Option[MImgThumb]] = {
    val q = CF
    val getReq = new GetRequest(HTABLE_NAME, idBin).family(CF).qualifier(q)
    ahclient.get(getReq).map { kvs =>
      kvs.headOption.map { kv =>
        val t = new Tuple(idBin) append deserializeTuple(kv.value())
        t addLong kv.timestamp
        new MImgThumb(t)
      }
    }
  }


  /** Сериализовать только полезные данные (без dkey и id, которые будут хранится в ключе и колонке). */
  def serializeDataOnly(t: MImgThumb) = {
    val data = t.getTupleEntry.selectEntry(FIELDS_DATA).getTuple
    SerialUtil.serializeTuple(data)
  }

  /** Десериализовать данные, собранные в serializeDataOnly(). */
  def deserializeDataOnly(id:Array[Byte], data:Array[Byte]): MImgThumb = {
    val t = new Tuple(id)
    SerialUtil.deserializeTuple(data, t)
    new MImgThumb(t)
  }

  val FIELDS      = new Fields(ID_FN, IMAGE_URL_FN, THUMB_FN, TIMESTAMP_FN)
  val FIELDS_DATA = new Fields(IMAGE_URL_FN, THUMB_FN)
}


/** Обычный код объекта вынесен за скобки для возможности легкого порождения дочерних моделей. */
trait MImgThumbStaticT extends CascadingFieldNamer {

  def CF = MPict.CF_IMG_THUMB

  val ID_FN        = fieldName("id")
  val IMAGE_URL_FN = fieldName("imageUrl")
  val THUMB_FN     = fieldName("thumb")
  val TIMESTAMP_FN = fieldName("timestamp")

  val FIELDS: Fields

  // Десериализаторы для работы напрямую с полями кортежа
  def deserializeId(id: AnyRef) = deserializeBytes(id)
  def deserializeThumb(thumb: AnyRef) = deserializeBytes(thumb)

  // Сериализаторы для работы напрямую с полями кортежа
  def serializeId(id: Array[Byte]) = serializeBytes(id)
  val serializeThumb: PartialFunction[AnyRef, ImmutableBytesWritable] = {
    case ar: Array[Byte]              => serializeBytes(ar)
    case ibw: ImmutableBytesWritable  => ibw
  }

  protected val serializeBytes:PartialFunction[Array[Byte], ImmutableBytesWritable] = {
    case null           => null
    case b: Array[Byte] => new ImmutableBytesWritable(b)
  }

  /** Десериализация значения в поле ID в бинарь. */
  protected val deserializeBytes: PartialFunction[AnyRef, Array[Byte]] = {
    case ibw: ImmutableBytesWritable => ibw.get   // 2013.dec.05: Оставлено для совместимости. Можно удалить перед первым релизом.
    case a: Array[Byte] => a
    case null  => null
    case other => throw new IllegalArgumentException("unexpected input[" + other.getClass.getName + "]: " + other)
  }

  /** Сгенерить id картинки на основе её URL. */
  def imgUrl2id(url: String): Array[Byte] = imgUrl2id(new URL(url))
  def imgUrl2id(url: URL): Array[Byte]    = CryptoUtil.sha1(UrlUtil.url2rowKey(url))

  // TODO заюзать implicit?
  def idBin2Str(id: Array[Byte]) = Base64 encodeBase64URLSafeString id
  def idStr2Bin(idStr: String)   = Base64 decodeBase64 idStr
}


/** Основной экземпляр модели. С ним происходит работа и на веб-морде, и в кравлере. */
class MImgThumb extends MImgThumbAbstract(MImgThumb) {
  import MImgThumb.FIELDS_DATA

  def this(te: TupleEntry) = {
    this()
    setTupleEntry(te)
  }

  def this(t: Tuple) = {
    this()
    setTuple(t)
  }

  def this(id:Array[Byte], imageUrl:String, thumb:Array[Byte], timestamp:Long = System.currentTimeMillis) = {
    this()
    setId(id)
    setImageUrl(imageUrl)
    setThumb(thumb)
    setTimestamp(timestamp)
  }

  def serializeDataOnly = MImgThumb serializeDataOnly this

  def dataEntry = _tupleEntry selectEntry FIELDS_DATA
  def dataTuple = _tupleEntry selectTuple FIELDS_DATA
}


/** Базовый код экземпляра модели и её родственников, отвязанный от своего объекта-компаньона.
  * @param companionObject Экземпляр объекта-компаньона. Это позволяет управлять статическими именами полей и сериализацией.
  */
abstract class MImgThumbAbstract(companionObject: MImgThumbStaticT) extends BaseDatum(companionObject.FIELDS) {

  import companionObject._

  def getId = deserializeId(_tupleEntry getObject ID_FN)
  def setId(id: Array[Byte]) = {
    _tupleEntry.setObject(ID_FN, serializeId(id))
    this
  }

  def getIdStr = idBin2Str(getId)
  def setIdStr(idStr: String) = _tupleEntry.setObject(ID_FN, idStr2Bin(idStr))

  def getImageUrl = _tupleEntry.getString(IMAGE_URL_FN)
  def setImageUrl(imageUrl: String) = _tupleEntry.setString(IMAGE_URL_FN, imageUrl)

  def getThumb = deserializeThumb(_tupleEntry.getObject(THUMB_FN))
  def setThumb(thumb: Array[Byte]) = {
    _tupleEntry.setObject(THUMB_FN, serializeThumb(thumb))
    this
  }

  def getTimestamp = _tupleEntry.getLong(TIMESTAMP_FN)
  def setTimestamp(timestamp: Long) = _tupleEntry.setLong(TIMESTAMP_FN, timestamp)
}

