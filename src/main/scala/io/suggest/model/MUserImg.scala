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

sealed trait MUserImgDeleteByIdStatic {

  /**
   * Удалить из хранилища ранее сохраненное добро.
   * @param idStr строковой id картинки == ключ ряда.
   * @return Фьючерс для синхронизации.
   */
  def deleteById(idStr: String, qs: List[String] = Nil): Future[_] = {
    val id = idStr2Bin(idStr)
    val htb = HTABLE_NAME_BYTES
    val cfb = CF_METADATA.getBytes
    val deleteReq = if(qs.isEmpty) {
      new DeleteRequest(htb, id, cfb)
    } else {
      val qsb = qs.map(_.getBytes).toArray
      new DeleteRequest(htb, id, cfb, qsb)
    }
    ahclient.delete(deleteReq)
  }

}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 18:15
 * Description: Пользовательские картинки. Юзер заливает картинки, а тут хранится всё необходимое.
 * Модель используется совместно с MImgThumb, но она абстрагирована от URL. Есть поле,
 * описывающее тип главного элемента, и есть значение. Если картинка имеет url, то это задаётся в MImgThumb.
 */
object MUserImgMetadata extends MPictSubmodel with MUserImgDeleteByIdStatic {
  
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


  /**
   * Прочитать из базы по id.
   * @param idStr Строковой id картинки.
   * @return Фьючерс с результатом.
   */
  def getById(idStr: String, q: Option[String] = None)(implicit ec: ExecutionContext): Future[Option[MUserImgMetadata]] = {
    val id = idStr2Bin(idStr)
    val q1 = q getOrElse Q_USER_IMG_ORIG
    val getReq = new GetRequest(HTABLE_NAME_BYTES, id, CF_METADATA.getBytes, q1.getBytes)
    ahclient.get(getReq).map { kvs =>
      kvs.headOption.flatMap { kv =>
        deserializeMetaData(kv.value) map { md =>
          MUserImgMetadata(new String(id), md, q1)
        }
      }
    }
  }

}


case class MUserImgMetadata(idStr: String, md: Map[String, String], q: String) {

  def id = idStr2Bin(idStr)

  /** Сохранить метаданные в хранилище. */
  def save: Future[_] = {
    val ser = MUserImgMetadata.serializeMetaData(md)
    val putReq = new PutRequest(HTABLE_NAME_BYTES, id, CF_METADATA.getBytes, q.getBytes, ser)
    ahclient.put(putReq)
  }

  def delete = MUserImgMetadata.deleteById(idStr)
}



/** Модель для оригиналов картинок. Затем также стала хранить и кадрированные версии под динамическими qualifiers. */
object MUserImgOrig extends MPictSubmodel with MUserImgDeleteByIdStatic {

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

}

case class MUserImgOrig(idStr: String, img: Array[Byte], q: String, timestamp: Long = -1L) extends ImgWithTimestamp {

  def id = idStr2Bin(idStr)

  def save: Future[_] = {
    val cfOrigsB = CF_ORIGINALS.getBytes
    val qUserImgOrigB = q.getBytes
    val putReq = if (timestamp > 0) {
      new PutRequest(HTABLE_NAME_BYTES, id, cfOrigsB, qUserImgOrigB, img, timestamp)
    } else {
      new PutRequest(HTABLE_NAME_BYTES, id, cfOrigsB, qUserImgOrigB, img)
    }
    ahclient.put(putReq)
  }

  def delete(implicit ec: ExecutionContext) = MUserImgOrig.deleteById(idStr, qs = List(q))
}

