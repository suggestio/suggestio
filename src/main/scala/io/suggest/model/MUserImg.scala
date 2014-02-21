package io.suggest.model

import io.suggest.util.{SerialUtil, CascadingFieldNamer}
import cascading.tuple.{TupleEntry, Tuple, Fields}
import com.scaleunlimited.cascading.BaseDatum
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import scala.concurrent.{Future, ExecutionContext}
import SioHBaseAsyncClient._
import org.hbase.async.{DeleteRequest, GetRequest, PutRequest}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.02.14 18:15
 * Description:
 */
object MUserImg extends CascadingFieldNamer with MPictSubmodel {

  def CF = MPict.CF_USER_IMG

  val ID_FN  = fieldName("id")
  val IMG_FN = fieldName("img")

  val FIELDS = new Fields(ID_FN, IMG_FN)
  val FIELDS_DATA = new Fields(IMG_FN)

  val deserializeImg: PartialFunction[AnyRef, Array[Byte]] = {
    case null => null
    case ibw: ImmutableBytesWritable => ibw.get()
    case a: Array[Byte] => a
  }

  /**
   * Прочитать из базы по id.
   * @param id id картинки.
   * @return Фьючерс с результатом.
   */
  def getById(id: String)(implicit ec: ExecutionContext): Future[Option[MUserImg]] = {
    val col = CF.getBytes
    val getReq = new GetRequest(HTABLE_NAME_BYTES, id.getBytes, col, col)
    ahclient.get(getReq).map { kvs =>
      kvs.headOption.map { kv =>
        val t = new Tuple(id) append SerialUtil.deserializeTuple(kv.value)
        new MUserImg(t)
      }
    }
  }

  /**
   * Удалить из хранилища ранее сохраненное добро.
   * @param id id картинки == ключ ряда.
   * @return Фьючерс для синхронизации.
   */
  def deleteById(id: String): Future[_] = {
    val col = CF.getBytes
    val deleteReq = new DeleteRequest(HTABLE_NAME_BYTES, id.getBytes, col, col)
    ahclient.delete(deleteReq)
  }

}

import MUserImg._

class MUserImg extends BaseDatum(FIELDS) {

  def this(t: Tuple) = {
    this()
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this()
    setTupleEntry(te)
  }

  def this(id: String, img: ImmutableBytesWritable) = {
    this()
    this.id = id
    this.img = img
  }


  def id = _tupleEntry getString ID_FN
  def id_=(id: String) = _tupleEntry.setString(ID_FN, id)

  def img = deserializeImg(_tupleEntry getObject IMG_FN)
  def img_=(img: Array[Byte]) {
    val ibw = new ImmutableBytesWritable(img)
    this.img = ibw
  }
  def img_=(ibw: ImmutableBytesWritable) {
    _tupleEntry.setObject(IMG_FN, ibw)
  }

  def dataTuple = _tupleEntry selectTuple FIELDS_DATA

  def save(implicit ec: ExecutionContext): Future[_] = {
    val v = SerialUtil.serializeTuple(dataTuple)
    val col = CF.getBytes
    val putReq = new PutRequest(HTABLE_NAME_BYTES, id.getBytes, col, col, v)
    ahclient.put(putReq)
  }

  def delete: Future[_] = {
    val maybeId = id
    if (maybeId == null)
      throw new IllegalStateException("id field is not set")
    else
      deleteById(maybeId)
  }
}
