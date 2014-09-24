package io.suggest.model

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import io.suggest.util.CascadingFieldNamer
import com.scaleunlimited.cascading.BaseDatum
import cascading.tuple.{Tuple, Fields, TupleEntry}
import MPict.{idStr2Bin, imgUrl2id, deserializeId, deserializeThumb, serializeId, serializeThumb, CF_METADATA, Q_IMAGE_URL, CF_THUMBS, Q_THUMB}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.10.13 14:56
 * Description: Модель доступа к превьюшкам-картинкам, которые исползуются в поисковой выдаче.
 * HBase-Cascading-схема осуществляет запись данных и лежит в сорцах кравлера вместе с остальной MR-логикой.
 * Ключ - md5(imageURL). CF=const
 *
 * 2013.dec.05:
 *  Статические и динамические части этой модели разбиты для дедубликации кода с ImgFetched-датумами siobix.
 *
 * 2014.feb.27:
 *  Реорганизация модели MPict в связи с добавлением MUserImg.
 *  В основном кортеже есть поля для:
 *  - Метаданные (т.е. ссылка) теперь в отдельном поле с отдельным Qualifier'ом.
 *  - thumb в отдельном поле.
 *
 * 2014.sep.22: Четкое разделение backend'а (AsyncHbase) и frontend'ов статики и динамики.
 * Сама модель оставлена для кравлера.
 */

@deprecated("This is a hbase model with random access. User MImgThumb2 instead (cassandra).")
object MImgThumb extends MImgThumbStaticAsyncHBase with MImgThumbStaticFieldsT {
  
  val FIELDS = new Fields(ID_FN, IMAGE_URL_FN, THUMB_FN, TIMESTAMP_FN)

}


/** Обычный код объекта вынесен за скобки для возможности легкого порождения дочерних моделей. */
trait MImgThumbStaticFieldsT extends CascadingFieldNamer {

  val ID_FN        = fieldName("id")
  val IMAGE_URL_FN = fieldName("imageUrl")
  val THUMB_FN     = fieldName("thumb")
  val TIMESTAMP_FN = fieldName("timestamp")

  val FIELDS: Fields

}

/** common-код статической thumb-модели. Backend'ы реализуют эти методы. */
trait MImgThumbStatic {

  /**
   * Прочитать значения поля url со ссылкой на исходную картинку.
   * @param idStr id картинки.
   * @return Фьючерс со строкой, если такая найдена.
   */
  def getUrl(idStr: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    getUrl(idStr2Bin(idStr))
  }

  /**
   * Чтение значение из хранилища по первичному ключу.
   * @param id id двоичный ключ ряда.
   * @return Фьючерс с опциональной ссылкой на картинку внутри.
   */
  def getUrl(id: Array[Byte])(implicit ec: ExecutionContext): Future[Option[String]]


  /**
   * Прочитать по hex id и dkey.
   * @param idStr Ключ ряда в виде hex-строки.
   * @return Фьючерс с опциональным результатом.
   */
  def getFullById(idStr: String)(implicit ec:ExecutionContext): Future[Option[MImgThumb]] = {
    getFullById(idStr2Bin(idStr))
  }

  /**
   * Прочитать по бинарном id и dkey.
   * @param idBin Бинарный id (ключ ряда, row key).
   * @return Фьючерс с опциональным результом.
   */
  def getFullById(idBin: Array[Byte])(implicit ec:ExecutionContext): Future[Option[MImgThumb]] = {
    val getThumbFut = getThumbById(idBin)
    for {
      mdResp        <- getUrl(idBin)
      thumbRespOpt  <- getThumbFut
    } yield {
      thumbRespOpt map { thumbResp =>
        val it = new MImgThumb(idBin)
        // Заливаем thumb в датум
        it.thumb = thumbResp.imgBytes
        it.timestamp = thumbResp.timestampMs
        // Заливаем image url
        if (mdResp.isDefined) {
          it.imageUrl = mdResp.get
        }
        // Вернуть результат
        it
      }
    }
  }


  /**
   * Прочитать thumb по id. Вернуть только thumb. Для веб-морды этого достаточно.
   * @param idStr Строковой id по MPict.
   * @return Фьючерс с опциональным результатом.
   */
  def getThumbById(idStr: String)(implicit ec: ExecutionContext): Future[Option[ImgWithTimestamp]] = {
    getThumbById(idStr2Bin(idStr))
  }

  /**
   * Прочитать thumb по id. Вернуть только thumb. Для веб-морды этого достаточно.
   * @param id Бинарный id по MPict.
   * @return Фьючерс с опциональным результатом (thumb -> timestamp).
   */
  def getThumbById(id: Array[Byte])(implicit ec: ExecutionContext): Future[Option[ImgWithTimestamp]]

}


/** HBase-backend для выполнения операций модели. */
trait MImgThumbStaticAsyncHBase extends MImgThumbStatic with MPictSubmodel {

  import SioHBaseAsyncClient._
  import org.hbase.async.GetRequest

  override def getUrl(id: Array[Byte])(implicit ec: ExecutionContext): Future[Option[String]] = {
    val getUrlReq = new GetRequest(HTABLE_NAME, id)
      .family(CF_METADATA)
      .qualifier(Q_IMAGE_URL)
    ahclient.get(getUrlReq) map { kvs =>
      kvs.headOption.map {
        kv => new String(kv.value)
      }
    }
  }

  override def getThumbById(id: Array[Byte])(implicit ec: ExecutionContext): Future[Option[ImgWithTimestamp]] = {
    val getReq = new GetRequest(HTABLE_NAME_BYTES, id)
      .family(CF_THUMBS)
      .qualifier(Q_THUMB)
    ahclient.get(getReq).map { kvs =>
      if (kvs.isEmpty) {
        None
      } else {
        val cell = kvs.head
        // Наверное надо какой-то нормальный экземпляр модели сделать?
        val result = new ImgWithTimestamp {
          val imgBytes = cell.value
          val timestampMs = cell.timestamp
        }
        Some(result)
      }
    }
  }

}


/** Основной экземпляр модели. С ним происходит работа и на веб-морде, и в кравлере. */
@deprecated("This is a hbase model with random access. User MImgThumb2 instead (cassandra).")
class MImgThumb extends MImgThumbAbstract(MImgThumb) with MImgThumbSaverAsyncHBase {

  def this(te: TupleEntry) = {
    this()
    setTupleEntry(te)
  }

  def this(t: Tuple) = {
    this()
    setTuple(t)
  }

  def this(id: Array[Byte]) = {
    this()
    this.id = id
  }

  def this(id:Array[Byte], imageUrl:String, thumb:Array[Byte], timestamp:Long = System.currentTimeMillis) = {
    this(id)
    this.imageUrl = imageUrl
    this.thumb = thumb
    this.timestamp = timestamp
  }

  def this(imageUrl:String, thumb:Array[Byte]) = {
    this(imgUrl2id(imageUrl), imageUrl, thumb)
  }

  override def toString = {
    val getTimestampHoursAgo = ((System.currentTimeMillis - timestamp) milliseconds).toHours
    s"${getClass.getSimpleName}($idStr, ${thumb.length} bytes, $getTimestampHoursAgo hours ago, $imageUrl)"
  }
}



/** Базовый код экземпляра модели и её родственников, отвязанный от своего объекта-компаньона.
  * @param companionObject Экземпляр объекта-компаньона. Это позволяет управлять статическими именами полей и сериализацией.
  */
abstract class MImgThumbAbstract(val companionObject: MImgThumbStaticFieldsT) extends BaseDatum(companionObject.FIELDS) {

  import companionObject._
  import cascading.tuple.coerce.Coercions.LONG

  def id = deserializeId(_tupleEntry getObject ID_FN)
  def id_=(id: Array[Byte]) {
    _tupleEntry.setObject(ID_FN, serializeId(id))
  }

  def idStr = MPict idBin2Str id
  def idStr_=(idStr: String) {
    _tupleEntry.setObject(ID_FN, MPict.idStr2Bin(idStr))
  }

  def imageUrl = _tupleEntry getString IMAGE_URL_FN
  def imageUrl_=(imageUrl: String) {
    _tupleEntry.setString(IMAGE_URL_FN, imageUrl)
  }
  def imageUrlOpt = Option(imageUrl)

  def thumb = deserializeThumb(_tupleEntry getObject THUMB_FN)
  def thumb_=(thumb: Array[Byte]) {
    _tupleEntry.setObject(THUMB_FN, serializeThumb(thumb))
  }

  def timestamp = _tupleEntry getLong TIMESTAMP_FN
  def timestamp_=(timestamp: Long) {
    _tupleEntry.setLong(TIMESTAMP_FN, timestamp)
  }
  def timestampOpt = {
    _tupleEntry.getObject(TIMESTAMP_FN) match {
      case null   => None
      case tstamp => Some(LONG coerce tstamp)
    }
  }
}


/** Подмешиваемая функция сохранения MImgThumb. Не для всех потомков MImgThumbAbstract это необходимо. */
trait MImgThumbSaver {

  def id: Array[Byte]
  def thumb: Array[Byte]
  def imageUrl: String

  def saveThumb: Future[_]

  def maybeSaveImgUrl: Future[_]

  /** Сохранить в таблицу. */
  def save(implicit ec: ExecutionContext): Future[_] = {
    val saveImgUrlFut = maybeSaveImgUrl
    saveThumb flatMap { _ =>
      saveImgUrlFut
    }
  }

}


/** Код сохранения экземпляра модели MImgThumb в HBase через драйвер AsyncHBase. */
trait MImgThumbSaverAsyncHBase extends MImgThumbSaver with MPictSubmodel {

  import SioHBaseAsyncClient._
  import org.hbase.async.PutRequest

  override def saveThumb: Future[_] = {
    val qT = Q_THUMB.getBytes
    val thumbPutReq = new PutRequest(HTABLE_NAME_BYTES, id, CF_THUMBS.getBytes, qT, thumb)
    ahclient.put(thumbPutReq)
  }

  override def maybeSaveImgUrl: Future[_] = {
    val maybeUrl = imageUrl
    if (maybeUrl != null) {
      val qIT = Q_IMAGE_URL.getBytes
      val iuPutReq = new PutRequest(HTABLE_NAME_BYTES, id, CF_METADATA.getBytes, qIT, maybeUrl.getBytes)
      ahclient.put(iuPutReq)
    } else {
      Future successful null
    }
  }

}

