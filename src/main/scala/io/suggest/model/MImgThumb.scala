package io.suggest.model

import SioHBaseAsyncClient._
import org.hbase.async.GetRequest
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.{SerialUtil, CryptoUtil, UrlUtil}
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
 */
object MImgThumb extends MPictSubmodel {

  def CF = MPict.CF_IMG_THUMB

  val ID_FN        = fieldName("id")
  val IMAGE_URL_FN = fieldName("imageUrl")
  val THUMB_FN     = fieldName("thumb")
  val TIMESTAMP_FN = fieldName("timestamp")

  val FIELDS      = new Fields(ID_FN, IMAGE_URL_FN, THUMB_FN, TIMESTAMP_FN)
  val FIELDS_DATA = new Fields(IMAGE_URL_FN, THUMB_FN)

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

  /** Десериализация значения в поле ID в бинарь. */
  val deserializeId: PartialFunction[AnyRef, Array[Byte]] = {
    case ibw: ImmutableBytesWritable => ibw.get
    case null => null
    case other => throw new IllegalArgumentException("unexpected input[" + other.getClass.getName + "]: " + other)
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


  /** Сгенерить id картинки на основе её URL. */
  def imgUrl2id(url: String): Array[Byte] = imgUrl2id(new URL(url))
  def imgUrl2id(url: URL): Array[Byte]    = CryptoUtil.sha1(UrlUtil.url2rowKey(url))

  // TODO заюзать implicit?
  def idBin2Str(id: Array[Byte]) = Base64 encodeBase64URLSafeString id
  def idStr2Bin(idStr: String)   = Base64 decodeBase64 idStr

  private def fieldName(fn: String) = BaseDatum.fieldName(getClass, fn)
}


import MImgThumb._

// Интерфейс и общий код экземпляров модели.
class MImgThumb extends BaseDatum(FIELDS) {

  def this(te: TupleEntry) = {
    this()
    setTupleEntry(te)
  }

  def this(t: Tuple) = {
    this()
    setTuple(t)
  }

  def this(id:Array[Byte], imageUrl:String, thumb:Array[Byte]) = {
    this()
    setId(id)
    setImageUrl(imageUrl)
    setThumb(thumb)
  }

  def this(id:ImmutableBytesWritable, imageUrl:String, thumb:ImmutableBytesWritable) = {
    this
    setId(id)
    setImageUrl(imageUrl)
    setThumb(thumb)
  }

  def getId = deserializeId(_tupleEntry getObject ID_FN)
  def setId(id: Array[Byte]) {
    setId(new ImmutableBytesWritable(id))
  }
  def setId(id: ImmutableBytesWritable) {
    _tupleEntry.setObject(ID_FN, id)
  }

  def getIdStr = idBin2Str(getId)
  def setIdStr(idStr: String) = _tupleEntry.setObject(ID_FN, idStr2Bin(idStr))

  def getImageUrl = _tupleEntry.getString(IMAGE_URL_FN)
  def setImageUrl(imageUrl: String) = _tupleEntry.setString(IMAGE_URL_FN, imageUrl)

  def getThumb = _tupleEntry.getObject(THUMB_FN).asInstanceOf[ImmutableBytesWritable].get()
  def setThumb(thumb: Array[Byte]) {
    val ibw = new ImmutableBytesWritable(thumb)
    setThumb(ibw)
  }
  def setThumb(thumbIbw: ImmutableBytesWritable) {
    _tupleEntry.setObject(THUMB_FN, thumbIbw)
  }

  def getTimestamp = _tupleEntry.getLong(TIMESTAMP_FN)
  def setTimestamp(timestamp: Long) = _tupleEntry.setLong(TIMESTAMP_FN, timestamp)

  def dataEntry = _tupleEntry selectEntry FIELDS_DATA
  def dataTuple = _tupleEntry selectTuple FIELDS_DATA

  def serializeDataOnly = MImgThumb serializeDataOnly this
}

