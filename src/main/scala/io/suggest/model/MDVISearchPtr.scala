package io.suggest.model

import HTapConversionsBasic._
import scala.concurrent.Future
import MDomain.{hclient, CF_SEARCH_PTR}
import org.apache.hadoop.hbase.client.{Put, Get}
import org.apache.hadoop.hbase.HColumnDescriptor
import scala.concurrent.ExecutionContext.Implicits.global // TODO Используем, пока не появится нормальный async hbase клиент.

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
  def getForDkey(dkey:String, idOpt:Option[String] = None): Future[Option[MDVISearchPtr]] = {
    val column: Array[Byte] = idOpt2column(idOpt)
    val getReq = new Get(dkey).addColumn(CF_SEARCH_PTR, column)
    hclient map { _client =>
      val result = try {
        _client.get(getReq)
      } finally {
        _client.close()
      }
      // Разобрать полученный результат
      if (result.isEmpty) {
        None
      } else {
        val v = result.getColumnLatest(CF_SEARCH_PTR, column).getValue
        val vins = v.split(SEP).toList
        Some(MDVISearchPtr(dkey=dkey, idOpt=idOpt, vins=vins))
      }
    }
  }

  /** Выдать CF-дескриптор для используемого CF_SEARCH_PTR
   * @return Новый экземпляр HColumnDescriptor.
   */
  def getCFDescriptor = new HColumnDescriptor(CF_SEARCH_PTR).setMaxVersions(1)

}

import MDVISearchPtr._

case class MDVISearchPtr(
  dkey: String,
  vins: List[String],
  idOpt: Option[String] = None
) {

  def columnName = idOpt2column(idOpt)

  /** Сохранить в хранилище.
   * @return Пустой фьючерс, который исполняется после реального сохранения данных в БД.
   */
  def save: Future[Unit] = {
    val v = vins.mkString(SEP)
    val putReq = new Put(dkey).add(CF_SEARCH_PTR, columnName, v)
    hclient map { _client =>
      try {
        _client.put(putReq)

      } finally {
        _client.close()
      }
    }
  }


  /** Выдать экземпляр MVirtualIndex.
   * @return Экземпляр, который не обязательно существует физически.
   */
  def getVirtualIndices = vins.map(MVirtualIndex(_))

}