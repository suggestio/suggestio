package io.suggest.model

import SioHBaseAsyncClient._
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin
import org.hbase.async.GetRequest
import HTapConversionsBasic._
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.{SerialUtil, CryptoUtil, UrlUtil}
import java.net.URL
import com.scaleunlimited.cascading.BaseDatum
import cascading.tuple.{Tuple, Fields, TupleEntry}
import io.suggest.util.SerialUtil._
import org.apache.hadoop.hbase.io.ImmutableBytesWritable

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
  val DKEY_FN      = fieldName("dkey")
  val IMAGE_URL_FN = fieldName("imageUrl")
  val THUMB_FN     = fieldName("thumb")

  val FIELDS = new Fields(ID_FN, DKEY_FN, IMAGE_URL_FN, THUMB_FN)
  val FIELDS_DATA = FIELDS subtract new Fields(ID_FN, DKEY_FN)

  /**
   * Прочитать по hex id и dkey.
   * @param id Ключ ряда в виде hex-строки.
   * @param dkey Ключ домена.
   * @return Фьючерс с опциональным результатом.
   */
  def getByIdDkey(id: String, dkey: String)(implicit ec:ExecutionContext): Future[Option[MImgThumb]] = {
    getByIdDkey(HexBin.decode(id), dkey)
  }

  /**
   * Прочитать по бинарном id и dkey.
   * @param idBin Бинарный id (ключ ряда, row key).
   * @param dkey Ключ домена, используемый в качестве колонки.
   * @return Фьючерс с опциональным результом.
   */
  def getByIdDkey(idBin: Array[Byte], dkey: String)(implicit ec:ExecutionContext): Future[Option[MImgThumb]] = {
    val q: Array[Byte] = dkey
    val getReq = new GetRequest(HTABLE_NAME_BYTES, idBin).family(CF).qualifier(q)
    ahclient.get(getReq).map { kvs =>
      kvs.headOption.map { kv =>
        val t = new Tuple(idBin, dkey)
        deserializeTuple(kv.value(), t)
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
  def deserializeDataOnly(id:Array[Byte], dkey:String, data:Array[Byte]): MImgThumb = {
    val t = new Tuple(id, dkey)
    SerialUtil.deserializeTuple(data, t)
    new MImgThumb(t)
  }


  /** Сгенерить id картинки на основе её URL. */
  def imgUrl2id(url: String): Array[Byte] = imgUrl2id(new URL(url))
  def imgUrl2id(url: URL): Array[Byte]    = CryptoUtil.md5(UrlUtil.url2rowKey(url))

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

  def this(id:Array[Byte], dkey:String, imageUrl:String, thumb:Array[Byte]) = {
    this()
    setId(id)
    setImageUrl(imageUrl)
    setThumb(thumb)
  }

  def getId = _tupleEntry.getObject(ID_FN).asInstanceOf[Array[Byte]]
  def setId(id: Array[Byte]) = _tupleEntry.set(ID_FN, id)
  def getIdHex = HexBin.encode(getId)
  def setIdHex(idHex: String) = _tupleEntry.set(ID_FN, HexBin.decode(idHex))

  def getDkey = _tupleEntry.getString(DKEY_FN)
  def setDkey(dkey: String) = _tupleEntry.setString(DKEY_FN, dkey)

  def getImageUrl = _tupleEntry.getString(IMAGE_URL_FN)
  def setImageUrl(imageUrl: String) = _tupleEntry.setString(IMAGE_URL_FN, imageUrl)

  def getThumb = _tupleEntry.getObject(THUMB_FN).asInstanceOf[ImmutableBytesWritable].get()
  def setThumb(thumb: Array[Byte]) = {
    val ibw = new ImmutableBytesWritable(thumb)
    _tupleEntry.set(THUMB_FN, ibw)
  }

  def dataEntry = _tupleEntry selectEntry FIELDS_DATA
  def dataTuple = _tupleEntry selectTuple FIELDS_DATA

  def serializeDataOnly = MImgThumb serializeDataOnly this
}

