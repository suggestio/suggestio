package io.suggest.model

import HTapConversionsBasic._
import scala.concurrent.{ExecutionContext, Future}
import MObject.CF_DSEARCH_PTR
import SioHBaseAsyncClient._
import org.apache.hadoop.hbase.HColumnDescriptor
import org.hbase.async.{PutRequest, GetRequest}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.07.13 10:27
 * Description: Активных индексов может быть несколько, и они могут иметь различные имена.
 * Однако веб-морде нужно быстро узнавать в каком индексе нужно производить поиск.
 * Тут - модель для задания используемых указателей на активные индексы.
 *
 * Указателей у одного dkey может быть несколько, однако использование их является опциональным.
 */

object MDVISearchPtr {

  val COLUMN_PREFIX = "_sp"
  val COLUMN_DFLT: Array[Byte] = COLUMN_PREFIX
  val SEP = ","

  def HTABLE_NAME = MObject.HTABLE_NAME

  def id2column(id: String) = COLUMN_PREFIX + "." + id
  def idOpt2column(id: Option[String]): Array[Byte] = {
    id match {
      case None      => COLUMN_DFLT
      case Some(_id) => id2column(_id)
    }
  }

  /** Прочитать указатель для dkey и id.
   * @param dkey Ключ домена.
   * @return Опциональный распрарсенный экземпляр MDVISearchPtr.
   */
  def getForDkey(dkey:String, idOpt:Option[String] = None)(implicit ec:ExecutionContext): Future[Option[MDVISearchPtr]] = {
    val column: Array[Byte] = idOpt2column(idOpt)
    val getReq = new GetRequest(HTABLE_NAME, dkey).family(CF_DSEARCH_PTR).qualifier(column)
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        None
      } else {
        val v = results.head.value()
        val vins = v.split(SEP).toList
        Some(MDVISearchPtr(dkey=dkey, idOpt=idOpt, vins=vins))
      }
    }
  }

  /** Выдать CF-дескриптор для используемого CF_DSEARCH_PTR
   * @return Новый экземпляр HColumnDescriptor.
   */
  def getCFDescriptor = new HColumnDescriptor(CF_DSEARCH_PTR).setMaxVersions(1)

}

import MDVISearchPtr._

case class MDVISearchPtr(
  dkey: String,
  vins: List[String],
  idOpt: Option[String] = None
) {

  def columnName = idOpt2column(idOpt)

  /** Сохранить в хранилище.
   * @return Пустой фьючерс, который исполняется после реального сохранения данных в БД. Объект внутри фьючерса не имеет
   *         никакого потайного смысла, это просто индикатор из клиента.
   */
  def save(implicit ec:ExecutionContext): Future[AnyRef] = {
    val v = vins.mkString(SEP)
    val putReq = new PutRequest(HTABLE_NAME:Array[Byte], dkey:Array[Byte], CF_DSEARCH_PTR, columnName, v:Array[Byte])
    ahclient.put(putReq)
  }


  /** Выдать экземпляр MVirtualIndex.
   * @return Экземпляр, который не обязательно существует физически.
   */
  def getVirtualIndices = vins.map(MVirtualIndex(_))

}