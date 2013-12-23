package io.suggest.model

import HTapConversionsBasic._
import scala.concurrent.{ExecutionContext, Future}
import MObject.CF_DSEARCH_PTR
import SioHBaseAsyncClient._
import org.apache.hadoop.hbase.HColumnDescriptor
import org.hbase.async.{PutRequest, GetRequest}
import scala.collection.JavaConversions._
import java.util
import io.suggest.util.SioModelUtil

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

// TODO В sioutil зачем-то сделана поддержка нескольких vin в рамках alias'a в searchPtr.
//      Надо бы вспомнить зачем...

object MDVISearchPtr {

  val Q_NONE = "?"
  val VINS_SERIAL_SEP = ","
  def HTABLE_NAME = MObject.HTABLE_NAME


  def id2column(id: String) = id

  def idOpt2column(id: Option[String]): Array[Byte] = {
    id match {
      case None      => Q_NONE
      case Some(_id) => id2column(_id)
    }
  }

  /**
   * Десериализовать название колонки в idOpt. Антипод к idOpt2column().
   * @param c Сырой column qualifier.
   * @return idOpt.
   */
  def column2idOpt(c: Array[Byte]): Option[String] = {
    if (util.Arrays.equals(c, Q_NONE)) {
      None
    } else {
      val cs = new String(c)
      Some(cs)
    }
  }


  /** Прочитать указатель для dkey и id.
   * @param dkey Ключ домена.
   * @param idOpt Опциональный идентификатор указателя.
   * @return Опциональный распрарсенный экземпляр MDVISearchPtr.
   */
  def getForDkeyId(dkey:String, idOpt:Option[String] = None)(implicit ec:ExecutionContext): Future[Option[MDVISearchPtr]] = {
    val rowkey = SioModelUtil.dkey2rowkey(dkey)
    val column: Array[Byte] = idOpt2column(idOpt)
    val getReq = new GetRequest(HTABLE_NAME, rowkey)
      .family(CF_DSEARCH_PTR)
      .qualifier(column)
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
    val rowkey = SioModelUtil.dkey2rowkey(dkey)
    val getReq = new GetRequest(HTABLE_NAME, rowkey)
      .family(CF_DSEARCH_PTR)
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
  def save(implicit ec:ExecutionContext): Future[_] = {
    val v = serializeVins(vins)
    val rowkey = SioModelUtil.dkey2rowkey(dkey)
    val putReq = new PutRequest(HTABLE_NAME:Array[Byte], rowkey, CF_DSEARCH_PTR.getBytes, columnName, v)
    ahclient.put(putReq)
  }


  /** Выдать экземпляр MVirtualIndex.
   * @return Экземпляр, который не обязательно существует физически.
   */
  def getVirtualIndices = vins.map(MVirtualIndex(_))

}