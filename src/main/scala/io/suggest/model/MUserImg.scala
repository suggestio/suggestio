package io.suggest.model

import io.suggest.util.SerialUtil
import cascading.tuple.Tuple
import scala.concurrent.{Future, ExecutionContext}
import SioHBaseAsyncClient._
import org.hbase.async.{DeleteRequest, GetRequest, PutRequest}
import scala.collection.JavaConversions._
import MPict._
import io.suggest.util.CascadingTupleUtil._
import org.apache.hadoop.hbase.io.ImmutableBytesWritable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 18:15
 * Description: Пользовательские картинки. Юзер заливает картинки, а тут хранится всё необходимое.
 * Модель используется совместно с MImgThumb, но она абстрагирована от URL. Есть поле,
 * описывающее тип главного элемента, и есть значение. Если картинка имеет url, то это задаётся в MImgThumb.
 */
object MUserImgMetadata extends MPictSubmodel {
  
  val deserializeMetaData: PartialFunction[AnyRef, Option[Map[String, String]]] = {
    case null             => None
    case t: Tuple         => Some(convertTupleToMap(t))
    case ab: Array[Byte]  => deserializeMetaData(SerialUtil.deserializeTuple(ab))
    case ibw: ImmutableBytesWritable => deserializeMetaData(ibw.get)
  }

  def serializeMetaData(md: Map[String,String]): Array[Byte] = {
    val t = convertMapToTuple(md)
    SerialUtil.serializeTuple(t)
  }


  def getById(idStr: String)(implicit ec: ExecutionContext): Future[Option[MUserImgMetadata]] = {
    getById(idStr2Bin(idStr))
  }

  /**
   * Прочитать из базы по id.
   * @param id id картинки.
   * @return Фьючерс с результатом.
   */
  def getById(id: Array[Byte])(implicit ec: ExecutionContext): Future[Option[MUserImgMetadata]] = {
    val getReq = new GetRequest(HTABLE_NAME_BYTES, id, CF_METADATA.getBytes, Q_IMG_META.getBytes)
    ahclient.get(getReq).map { kvs =>
      kvs.headOption.flatMap { kv =>
        deserializeMetaData(kv.value) map { md =>
          MUserImgMetadata(new String(id), md)
        }
      }
    }
  }


  def deleteById(idStr: String): Future[_] = {
    deleteById(idStr2Bin(idStr))
  }

  /**
   * Удалить из хранилища ранее сохраненное добро.
   * @param id id картинки == ключ ряда.
   * @return Фьючерс для синхронизации.
   */
  def deleteById(id: Array[Byte]): Future[_] = {
    val deleteReq = new DeleteRequest(HTABLE_NAME_BYTES, id, CF_METADATA.getBytes, Q_IMG_META.getBytes)
    ahclient.delete(deleteReq)
  }

}


case class MUserImgMetadata(idStr: String, md: Map[String, String]) {

  def id = idStr2Bin(idStr)

  /** Сохранить метаданные в хранилище. */
  def save: Future[_] = {
    val ser = MUserImgMetadata.serializeMetaData(md)
    val putReq = new PutRequest(HTABLE_NAME_BYTES, id, CF_METADATA.getBytes, Q_IMG_META.getBytes, ser)
    ahclient.put(putReq)
  }

  def delete = MUserImgMetadata.deleteById(id)
}



/** Оригиналы картинок хранятся в CF-ке метаданных, т.к. далеко не всегда нужно обращаться к оригиналу. */
object MUserImgOrig extends MPictSubmodel {

  def getById(idStr: String)(implicit ec: ExecutionContext): Future[Option[ImgWithTimestamp]] = {
    getById(idStr2Bin(idStr))
  }

  /**
   * Получить оригинал картинки.
   * @param id Ключ картинки.
   * @return Фьючерс с будущем результатом.
   */
  def getById(id: Array[Byte])(implicit ec: ExecutionContext): Future[Option[ImgWithTimestamp]] = {
    val getReq = new GetRequest(HTABLE_NAME_BYTES, id, CF_ORIGINALS.getBytes, Q_USER_IMG_ORIG.getBytes)
    ahclient.get(getReq) map { kvs =>
      if (kvs.isEmpty) {
        None
      } else {
        val cell = kvs.head
        val result = ImgWithTimestamp(cell.value, cell.timestamp)
        Some(result)
      }
    }
  }


  def deleteById(idStr: String): Future[_] = {
    deleteById(idStr2Bin(idStr))
  }

  def deleteById(id: Array[Byte]): Future[_] = {
    val delReq = new DeleteRequest(HTABLE_NAME_BYTES, id, CF_ORIGINALS.getBytes, Q_USER_IMG_ORIG.getBytes)
    ahclient.delete(delReq)
  }

}

case class MUserImgOrig(idStr: String, orig: Array[Byte]) {

  def id = idStr2Bin(idStr)

  def save: Future[_] = {
    val putReq = new PutRequest(HTABLE_NAME_BYTES, id, CF_ORIGINALS.getBytes, Q_USER_IMG_ORIG.getBytes, orig)
    ahclient.put(putReq)
  }

  def delete = MUserImgOrig.deleteById(id)
}

