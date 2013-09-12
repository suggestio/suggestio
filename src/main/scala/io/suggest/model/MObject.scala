package io.suggest.model

import org.apache.hadoop.hbase.client.{Get, Put}
import scala.concurrent.{Future, future}
import org.apache.hadoop.hbase.{HTableDescriptor, HColumnDescriptor}
import HTapConversionsBasic._
import io.suggest.util.SioConstants.DOMAIN_QI_TTL_SECONDS
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.09.13 10:37
 * Description: Модель мелких объектов suggest.io: домеенов, юзеров, новостей и всего остального.
 * Мелкие немногочисленные бессвязные вещи хранятся в одной таблице в разных CF-ках чтобы избежать передозировки
 * неэффективно используемых регионов и сопутствуюих проблем.
 */


// Бывшая MDomain. Используется для хранения мелких объектов типа метаданным по объектам, юзерам и т.д.
// Для разграничения используются CF'ки.
object MObject {

  val HTABLE_NAME = "obj"

  // Column family для хранения произвольных k-v колонок для доменов (ключ - dkey). В значениях идём по алфавиту.
  val CF_DPROPS      = "a".getBytes   // Пропертисы всякие. js-install и reindex info в частности.
  val CF_DINX_ACTIVE = "b".getBytes
  val CF_DSEARCH_PTR = "c".getBytes
  // Для моделей веб-морды:
  val CF_DDATA       = "d".getBytes   // DomainData - сохранение json'а настроек (веб-морда)
  val CF_DPUBLISH    = "e".getBytes   // Отметки о публикации доменов в галерее.
  val CF_DQI         = "f".getBytes   // QI-отметки, не привязаные к юзеру. Имеют ttl.

  // Инфа по юзерам. Access pattern - веб-морда.
  val CF_UPROPS      = "g".getBytes   // Юзеры. Тут например хранится предпочтительный язык UI и прочие вещи. Аналог DPROPS.
  val CF_UAUTHZ      = "h".getBytes   // Данные по авторизациям юзеров в доменах.

  // Другое (веб-морда).
  val CF_BLOG        = "i".getBytes   // Блог-записи/новости (веб-морда, ключ = некий id).

  // /!\ При добавлении новых CF-записей нужно также обновлять/запиливать функции createTable() и updateTable().


  /** Одноразовый клиент для этой таблицы. Нужно вызывать close() по завершению его работы.
   * @return Фьючерс с клиентом. Если пул клиентов исчерпан, то фьючерс будет исполнен через некоторое время.
   */
  def hclient = SioHBaseSyncClient.clientForTable(HTABLE_NAME)


  /** Асинхронно создать таблицу. Полезно при первом запуске. Созданная таблица относится и к подчиненным моделям.
   * @return Пустой фьючерс, который исполняется при наступлении эффекта созданной таблицы.
   */
  def createTable: Future[Unit] = {
    val tableDesc = new HTableDescriptor(HTABLE_NAME)
    val cfs = List(
      // Object props
      hcd(CF_DPROPS, 3),
      // utils-модели
      MDVIActive.getCFDescriptor,       // MDVIActive
      MDVISearchPtr.getCFDescriptor,    // MDVISearchPtr
      // остальные (внешние) модели
      hcd(CF_DDATA, 2),
      hcd(CF_DPUBLISH, 1),
      hcd(CF_DQI, 1).setTimeToLive(DOMAIN_QI_TTL_SECONDS),
      hcd(CF_UPROPS, 1),
      hcd(CF_UAUTHZ, 2),
      hcd(CF_BLOG, 1)
    )
    cfs foreach tableDesc.addFamily
    future {
      SioHBaseSyncClient.admin.createTable(tableDesc)
    }
  }

  private def hcd(name: Array[Byte], maxVersions:Int) = new HColumnDescriptor(name).setMaxVersions(maxVersions)


  /** Выставить произвольнон значение для произвольной колонки в CF_PROPS. По идее должно использоваться из других моделей,
   * занимающихся сериализацией.
   * @param dkey ключ домена
   * @param key ключ
   * @param value сериализованное значение
   * @return Пустой фьючерс для опциональной синхронизации.
   */
  def setProp(dkey:String, key:String, value: Array[Byte]): Future[Unit] = {
    val putReq = new Put(dkey).add(CF_DPROPS, key, value)
    hclient map { _client =>
      try {
        _client.put(putReq)
      } finally {
        _client.close()
      }
    }
  }


  /** Прочитать провертис, выставленный через setProp().
   * @param dkey Ключ домена.
   * @param key Ключ.
   * @return Фьючерс с опциональным значением, если такое найдено.
   */
  def getProp(dkey: String, key: String): Future[Option[Array[Byte]]] = {
    val column: Array[Byte] = key
    val getReq = new Get(dkey).addColumn(CF_DPROPS, column)
    hclient map { _client =>
      val result = try {
        _client.get(getReq)
      } finally {
        _client.close()
      }
      if (result.isEmpty) {
        None
      } else {
        Some(result.getColumnLatest(CF_DPROPS, column).getValue)
      }
    }
  }

}

