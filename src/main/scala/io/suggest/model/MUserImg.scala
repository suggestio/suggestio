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



/** Модель для оригиналов картинок. Затем также стала хранить и кадрированные версии под динамическими qualifiers. */
object MUserImgOrig extends MPictSubmodel {

  /**
   * Прочитать из базы по id и qualifier. Если qualifier не указан, то будет использован дефолтовый,
   * т.е. qualifier оригинальной картинки.
   * @param idStr ключ ряда в виде строки.
   * @param q Необязательный qualifier картинки.
   * @return Асинхронный результат.
   */
  def getById(idStr: String, q: Option[String] = None)(implicit ec: ExecutionContext): Future[Option[MUserImgOrig]] = {
    val id = idStr2Bin(idStr)
    val q1 = q getOrElse Q_USER_IMG_ORIG
    val getReq = new GetRequest(HTABLE_NAME_BYTES, id, CF_ORIGINALS.getBytes, q1.getBytes)
    ahclient.get(getReq) map { kvs =>
      if (kvs.isEmpty) {
        None
      } else {
        val cell = kvs.head
        val result = MUserImgOrig(idStr, cell.value, q1, cell.timestamp)
        Some(result)
      }
    }
  }


  /**
   * Удалить картинку из хранилища по ключу ряда и указанных qualifier'ам (qs).
   * Если qs не заданы, то будут удалены все qualifier'ы ряда в рамках CF модели.
   * @param idStr Ключ ряда (ключ картинки) в виде строки.
   * @param qs qualifier'ы.
   * @return Фьючерс для синхронизации.
   */
  def deleteById(idStr: String, qs: List[String] = Nil)(implicit ec: ExecutionContext): Future[_] = {
    val id = idStr2Bin(idStr)
    val tablenameB = HTABLE_NAME_BYTES
    val cfOrigsB = CF_ORIGINALS.getBytes
    if (qs.isEmpty) {
      // Если qualifiers не задан, то удаляются все qualifier'ы ряда.
      val delReq = new DeleteRequest(tablenameB, id, cfOrigsB)
      ahclient.delete(delReq)
    } else {
      Future.traverse(qs) { q =>
        val delReq = new DeleteRequest(tablenameB, id, cfOrigsB, q.getBytes)
        ahclient.delete(delReq)
      }
    }
  }

}

case class MUserImgOrig(idStr: String, img: Array[Byte], q: String, timestamp: Long = -1L) extends ImgWithTimestamp {

  def id = idStr2Bin(idStr)

  def save: Future[_] = {
    val cfOrigsB = CF_ORIGINALS.getBytes
    val qUserImgOrigB = Q_USER_IMG_ORIG.getBytes
    val putReq = if (timestamp > 0) {
      new PutRequest(HTABLE_NAME_BYTES, id, cfOrigsB, qUserImgOrigB, img, timestamp)
    } else {
      new PutRequest(HTABLE_NAME_BYTES, id, cfOrigsB, qUserImgOrigB, img)
    }
    ahclient.put(putReq)
  }

  def delete(implicit ec: ExecutionContext) = MUserImgOrig.deleteById(idStr, qs = List(q))
}

