package io.suggest.model

import org.apache.hadoop.hbase.client.{Get, Put}
import scala.concurrent.{ExecutionContext, Future, future}
import org.apache.hadoop.hbase.{HTableDescriptor, HColumnDescriptor}
import HTapConversionsBasic._
import io.suggest.util.SioConstants.DOMAIN_QI_TTL_SECONDS
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import org.hbase.async.{GetRequest, PutRequest}
import SioHBaseAsyncClient._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.09.13 10:37
 * Description: Модель мелких объектов suggest.io с random-чтением и записью: доменов, юзеров, новостей и всего остального.
 * Мелкие немногочисленные бессвязные вещи хранятся в одной таблице в разных CF-ках чтобы избежать передозировки
 * неэффективно используемых регионов и сопутствуюих проблем.
 */


// Бывшая MDomain. Используется для хранения мелких объектов типа метаданным по объектам, юзерам и т.д.
// Для разграничения используются CF'ки.
object MObject {

  val HTABLE_NAME = "obj"
  val HTABLE_NAME_BYTES = HTABLE_NAME.getBytes

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
  val CF_DOMAIN      = "j".getBytes   // Записи MDomain.
  //val CF_DKNOWLEDGE  = "k".getBytes   // Записи MDomainKnowledge, заполняемые кравлером. qualifier является ключом, а value - значением.

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
      hcd(CF_BLOG, 1),
      hcd(CF_DOMAIN, 2)
      //hcd(CF_DKNOWLEDGE, 1)
    )
    cfs foreach tableDesc.addFamily
    future {
      SioHBaseSyncClient.admin.createTable(tableDesc)
    }
  }

  private def hcd(name: Array[Byte], maxVersions:Int) = new HColumnDescriptor(name).setMaxVersions(maxVersions)


  /** Существует ли таблица с именем obj?
   * @return true, если таблица с именем obj существует. Иначе false.
   */
  def isTableExists: Future[Boolean] = {
    future {
      SioHBaseSyncClient.admin.tableExists(HTABLE_NAME)
    }
  }

  /** Убедиться, что таблица существует.
   * TODO Сделать, чтобы был updateTable при необходимости (если схема таблицы слегка устарела).
   */
  def ensureTableExists: Future[Unit] = {
    isTableExists flatMap {
      case false => createTable
      case true  => Future.successful(())
    }
  }

  /** Выставить произвольнон значение для произвольной колонки в CF_PROPS. По идее должно использоваться из других моделей,
   * занимающихся сериализацией.
   * @param dkey Ключ домена.
   * @param key Ключ.
   * @param value сериализованное значение
   * @return Фьючерс для опциональной синхронизации. Любые данные внутри возвращаемого фьючерса не имеют смысла.
   */
  def setProp(dkey:String, key:String, value: Array[Byte]): Future[AnyRef] = {
    val putReq = new PutRequest(HTABLE_NAME:Array[Byte], dkey:Array[Byte], CF_DPROPS, key:Array[Byte], value)
    ahclient.put(putReq)
  }


  /** Прочитать провертис, выставленный через setProp().
   * @param dkey Ключ домена.
   * @param key Ключ.
   * @return Фьючерс с опциональным значением, если такое найдено.
   */
  def getProp(dkey: String, key: String)(implicit ec: ExecutionContext): Future[Option[Array[Byte]]] = {
    val column: Array[Byte] = key
    val getReq = new GetRequest(HTABLE_NAME, dkey) family CF_DPROPS qualifier column
    ahclient.get(getReq) map { results =>
      if (results.isEmpty) {
        None
      } else {
        Some(results.head.value)
      }
    }
  }

}

