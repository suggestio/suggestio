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
 *
 */

// TODO В sioutil зачем-то сделана поддержка нескольких vin в рамках alias'a в searchPtr.
//      Надо бы вспомнить зачем...

object MDVISearchPtr {

  val COLUMN_PREFIX = "_sp"
  val COLUMN_DFLT: Array[Byte] = COLUMN_PREFIX
  val COLUMN_ID_SEP = "."

  val COLUMN_ID_PREFIX = COLUMN_PREFIX + COLUMN_ID_SEP
  val VINS_SERIAL_SEP = ","

  def HTABLE_NAME = MObject.HTABLE_NAME

  def id2column(id: String) = COLUMN_ID_PREFIX + id

  def idOpt2column(id: Option[String]): Array[Byte] = {
    id match {
      case None      => COLUMN_DFLT
      case Some(_id) => id2column(_id)
    }
  }

  /**
   * Десериализовать название колонки в idOpt. Антипод к idOpt2column().
   * @param c Сырой column qualifier.
   * @return idOpt.
   */
  def column2idOpt(c: Array[Byte]): Option[String] = {
    if (c == COLUMN_DFLT) {
      None
    } else {
      val cs = new String(c)
      if (cs startsWith COLUMN_ID_PREFIX) {
        Some(cs.substring(COLUMN_ID_PREFIX.length))
      } else {
        throw new IllegalArgumentException("Column qualifier name must start with '%s'." format COLUMN_ID_PREFIX)
      }
    }
  }

  /** Прочитать указатель для dkey и id.
   * @param dkey Ключ домена.
   * @param idOpt Опциональный идентификатор указателя.
   * @return Опциональный распрарсенный экземпляр MDVISearchPtr.
   */
  def getForDkeyId(dkey:String, idOpt:Option[String] = None)(implicit ec:ExecutionContext): Future[Option[MDVISearchPtr]] = {
    val column: Array[Byte] = idOpt2column(idOpt)
    val getReq = new GetRequest(HTABLE_NAME, dkey).family(CF_DSEARCH_PTR).qualifier(column)
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        None
      } else {
        val v = results.head.value()
        val vins = deserializeVins(v)
        Some(MDVISearchPtr(dkey=dkey, idOpt=idOpt, vins=vins))
      }
    }
  }

  /**
   * Выдать все search-указатели для dkey.
   * @param dkey Ключ домена, в рамках котого происходит поиск значений указателей.
   * @return Фьючерс со списком сабжей в произвольном порядке.
   */
  def getAllForDkey(dkey: String)(implicit ec:ExecutionContext): Future[List[MDVISearchPtr]] = {
    val getReq = new GetRequest(HTABLE_NAME, dkey).family(CF_DSEARCH_PTR)
    ahclient.get(getReq) map { results =>
      results.toList.map { kv =>
        val idOpt = column2idOpt(kv.qualifier)
        val vins = deserializeVins(kv.value)
        MDVISearchPtr(dkey=dkey, vins=vins, idOpt=idOpt)
      }
    }
  }

  /** Выдать CF-дескриптор для используемого CF_DSEARCH_PTR
   * @return Новый экземпляр HColumnDescriptor.
   */
  def getCFDescriptor = new HColumnDescriptor(CF_DSEARCH_PTR).setMaxVersions(1)


  def serializeVins(vins: List[String]) = vins.mkString(VINS_SERIAL_SEP).getBytes
  def deserializeVins(v:Array[Byte]) = {
    if (v.length == 0) {
      Nil
    } else {
      v.split(VINS_SERIAL_SEP).toList
    }
  }
}

import MDVISearchPtr._

case class MDVISearchPtr(
  dkey: String,
  vins: List[String],
  idOpt: Option[String] = None
) {
  if (vins.isEmpty)
    throw new IllegalArgumentException("vins must be NON-empty list.")

  def columnName = idOpt2column(idOpt)

  /** Сохранить в хранилище.
   * @return Пустой фьючерс, который исполняется после реального сохранения данных в БД. Объект внутри фьючерса не имеет
   *         никакого потайного смысла, это просто индикатор из клиента.
   */
  def save(implicit ec:ExecutionContext): Future[MDVISearchPtr] = {
    val v = serializeVins(vins)
    val putReq = new PutRequest(HTABLE_NAME:Array[Byte], dkey:Array[Byte], CF_DSEARCH_PTR, columnName, v)
    ahclient.put(putReq).map { _ => this }
  }


  /** Выдать экземпляр MVirtualIndex.
   * @return Экземпляр, который не обязательно существует физически.
   */
  def getVirtualIndices = vins.map(MVirtualIndex(_))

}