package io.suggest.model

import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.Client
import io.suggest.util.SioEsUtil._
import org.apache.lucene.index.IndexNotFoundException
import io.suggest.util.{MacroLogsImpl, StringUtil, SioEsIndexUtil}
import org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._
import java.util.regex.Pattern
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.07.13 11:03
 * Description: Непосредственная информация об индексах в ES обрабатывается через эту модель.
 * Шардинг ES не позволяет соотнести какой-то конкретный тип с конкретной шардой, а routing не позволяет понять,
 * в какую шарду сохранен тот или иной документ, поэтому нужны виртуальные индексы.
 *
 * Вся информация хранится без dkey. Информация проста: кол-во шард. Вся инфа содержится в имени индекса, поэтому
 * не требуется какого-либо хранилища для данных этой модели.
 * abcdefgh3 - означает, что имеет место виртуальный индекс, состоящий из трех шард с именами abcdefgh3_0, abcdefgh3_1 и abcdefgh3_2
 * vin-префикс - это какбы generation на момент создания, состоящий из base32+hex lowercase алфавита и нужен
 * для именования, но также служит для сортировки во времени.
 */
object MVirtualIndex extends MacroLogsImpl {

  /** Отделять префикс vin'а от полей, входящих в название с помощью этого символа. */
  val VIN_PREFIX_SEP = "."

  // Регэксп для парсинга vin на vinPrefix и shardCount.
  val vin2prefixCountRe = {
    val sepQuotedStr = Pattern.quote(VIN_PREFIX_SEP)
    val reStr = "^([a-z0-9]+)" + sepQuotedStr + "([0-9]+)$"
    reStr.r
  }

  val INDEX_REFRESH_INTERVAL = "index.refresh_interval"
  val INDEX_REFRESH_INTERVAL_BULK = Integer.valueOf(-1)
  val INDEX_REFRESH_INTERVAL_SECONDS_DFLT = Integer.valueOf(16)

  /**
   * Генерируем псевдослучайные id-шники, и чтобы сортировка по времени их создания работала исходя из самого префикса.
   * Сортировка нужна, чтобы вычислять какие vin'ы появились раньше, а какие позже.
   * Сортировка позволяет избежать хранения дополнительного указателя на текущий индекс. Так поиск будет работаеть при
   * rebuild'е без использования дополнительных костылей.
   * Используется b32+hex. b64+ordered нельзя, т.к. ES не поддерживает upper-case названия.
   * TODO Через (44*11) лет произойдёт сбой сортировки из-за повышения разрядности b32-идентификатора.
   *      Тогда возможно, будет не работать поиск на веб-морде какое-то небольшое время, если кеш будет включен и будет длинный.
   * @param l Опорный timestamp. По умолчанию - текущий таймштамп.
   * @return lowerCase набор цифр и латинских букв, входящие в алфавит Base32+HEX.
   */
  def generateVinPrefix(l: Long = System.currentTimeMillis) = StringUtil.longAsB32hLc(l)

  /** Сгенерить vin по префиксу и общему числу шард.
   * @param vinPrefix Префикс. Обычно случайная строка из [a-z].
   * @param shardCount Общее кол-во шард в виртуальном индексе.
   * @return Строка, которая может быть использована для сохранения как id индекса.
   */
  def vinFor(vinPrefix: String, shardCount: Int): String = vinPrefix + "." + shardCount

  /** Имя физической es-шарды (es-индекса) на основе данных виртуального индекса и номера шарды.
   * @param vinPrefix Префикс. Обычно случайная строка из [a-z].
   * @param shardCount Общее кол-во шард в виртуальном индексе.
   * @param shardN Номер шарды от 0 до shardCount-1.
   * @return Строка, которая используется для именования индексов в ES.
   */
  def esShardNameFor(vinPrefix: String, shardCount: Int, shardN: Int): String = {
    if (shardN >= shardCount || shardN < 0) {
      throw new IllegalArgumentException("shardN invalid = %s. Should be in [0..%s)." format (shardN, shardCount))
    }
    val vin = vinFor(vinPrefix, shardCount)
    esShardNameFor(vin, shardN)
  }


  /** Сгенерить имя es-индекса (es-шарды) на основе готового vin и номера шарды в индексе.
   * @param vin id виртуального индекса.
   * @param shardN Номер шарды в виртуальном индексе.
   * @return Строка, которая используется для именования индексов в ES.
   */
  def esShardNameFor(vin:String, shardN: Int): String = {
    vin + "_" + shardN
  }


  /**
   * Прочитать файл из хранилища.
   * @param vin базовое имя индекса.
   * @return Экземпляр сабжа, даже если он на деле не существует.
   */
  def apply(vin: String): MVirtualIndex = MVirtualIndexVin(vin)
  def apply(vinPrefix:String, shardCount:Int): MVirtualIndex = MVirtualIndexPrefixCount(vinPrefix, shardCount)

  /**
   * Прочитать кол-во реплик для указанного виртуального индекса, состоящего из натуральных ES-индексов.
   * Выдает целое. Если перечисленные индексы имеют изменяющееся число шард, то будет экзепшен.
   * @param indices список индексов, подлежащих вычитыванию.
   * @return Кол-во реплик.
   */
  def getReplicasCountFor(indices: Seq[String])(implicit client:Client, executor:ExecutionContext): Future[Int] = {
    client.admin().cluster()
      .prepareState()
      .setIndices(indices: _*)
      .setMetaData(true)
      .execute()
      .flatMap { resp =>
        val md = resp.getState.getMetaData
        val results = indices map { shardName =>
          md.index(shardName) match {
            case null => throw new IndexNotFoundException(shardName)
            case imd  => imd.getSettings.getAsInt(SETTING_NUMBER_OF_REPLICAS, 1).toInt
          }
        }
        results.distinct match {
          case seq if seq.length == 1 =>
            Future.successful(seq.head)

          case _ =>
            val msg = "Unsupported variable replicas count: for shards %s => %s" format (indices, results)
            val ex  = new UnsupportedOperationException(msg)
            Future.failed(ex)
        }
      }
  }


  /**
   * Удалить эти индексы вообще.
   * @return true, если всё ок.
   */
  def deleteThese(indices:Seq[String])(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    // TODO Нужно проконтроллировать, что удаление существующих индексов будет выполнено, даже если одного или нескольких
    // индексов нет в наличии.
    client.admin().indices()
      .prepareDelete(indices: _*)
      .execute()
      .map(_.isAcknowledged)
  }

  /**
   * Создать шарды.
   * @param indices индексы.
   */
  def ensureIndices(indices:Seq[String], replicasCount:Int = 1)(implicit client:Client, executor:ExecutionContext) = {
    Future.traverse(indices) { inx => createIndex(inx, replicas=replicasCount) }
  }


  /**
   * Короткая функция для обновления произвольных настроек индексов.
   * @param settings Карта новых сеттингов.
   * @param indices Индексы, подлежащие изменению настроек.
   * @return Фьючерс с малополезным результатом.
   */
  def updateInxSettings(settings:Map[String, AnyRef], indices:Seq[String])(implicit client:Client): Future[UpdateSettingsResponse] = {
    client.admin().indices()
      .prepareUpdateSettings(indices : _*)
      .setSettings(settings)
      .execute()
  }

  /**
   * Выставить refresh interval.
   * @param newRefreshInterval Новый интервал в секундах.
   * @param indices Шарды.
   * @return Фьючерс для синхронизации с каким-то мусором внутри.
   */
  def setRefreshInterval(newRefreshInterval: Integer, indices: Seq[String])(implicit client:Client) = {
    val settings = Map(INDEX_REFRESH_INTERVAL -> newRefreshInterval)
    updateInxSettings(settings, indices)
  }

}


import MVirtualIndex._

/**
 * Динамическая часть модели отражает созданный mvi с соответствующей информацией.
 * @param vinPrefix Базовое имя индекса. vin-идентификатор генерится на основе этого всего.
 * @param shardCount Кол-во шард.
 */
case class MVirtualIndexPrefixCount(vinPrefix: String, shardCount: Int) extends MVirtualIndex {
  @JsonIgnore
  lazy val vin: String = vinFor(vinPrefix, shardCount)
}

/** Экземпляр индекса по vin.
 * @param vin vin, включающий префикс и кол-во шард.
 */
case class MVirtualIndexVin(@JsonIgnore vin: String) extends MVirtualIndex {
  lazy val (vinPrefix, shardCount) = {
    val vin2prefixCountRe(_vinPrefix, _shardCountStr) = vin
    _vinPrefix -> _shardCountStr.toInt
  }
}


trait MVirtualIndex extends Serializable {

  import MVirtualIndex.LOGGER._


  @JsonIgnore def vinPrefix:  String
  @JsonIgnore def shardCount: Int
  @JsonIgnore def vin: String

  @JsonIgnore def head = esShardNameFor(vin, 0)

  def getShardIds: Seq[Int] = 0 until shardCount

  /**
   * Выдать имена всех шард.
   * @return Последовательность шард в порядке возрастания индекса.
   */
  @JsonIgnore
  lazy val getShards: Seq[String] = {
    getShardIds map { esShardNameFor(vin, _) }
  }

  /**
   * Прочитать кол-во реплик из метаданных ES. Считаем для всех шард. Если число реплик у разных шард отличается,
   * то будет ошибка.
   * @return Фьючерс с кол-вом доступных шард.
   */
  def getReplicasCount(implicit client:Client, executor:ExecutionContext): Future[Int] = getReplicasCountFor(getShards)

  /**
   * Выставить новое число реплик.
   * @param replicasCount новое число реплик.
   * @return true, если всё ок. false по сути никогда и не возвращает.
   */
  def setReplicasCount(replicasCount:Int, shards:Seq[String] = getShards)(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
    SioEsIndexUtil.setReplicasCountFor(shards, replicasCount)
  }


  /**
   * Удалить индекс вообще. И файл индекса в след за ним.
   * @return true, если всё ок. Фьючерс исполняется, когда всё сделано.
   */
  def eraseShards(implicit client:Client, executor:ExecutionContext) = deleteThese(getShards)


  /**
   * Создать все необходимые для текущего экземпляра индексы.
   * @param replicasCount кол-во реплик.
   * @return Фьючерс.
   */
  def ensureShards(replicasCount: Int = 1)(implicit client:Client, executor:ExecutionContext): Future[Unit] = {
    val shards = getShards
    ensureIndices(shards, replicasCount=replicasCount)
      .flatMap { boolSeq =>
        boolSeq.distinct match {
          case s if s.length == 1 =>
            // Убедится, что replicalCount соответствует нужному числу реплик.
            setReplicasCount(replicasCount, shards)

          case other =>
            val msg = "Cannot create some of requested shards: %s => %s. Rollbacking..." format (shards, boolSeq)
            error(msg)
            val createdIndexes = (shards zip boolSeq) filter(_._2) map(_._1)
            deleteThese(createdIndexes) flatMap { _ =>
              Future.failed(new RuntimeException(msg))
            }
        }
      }
  }


  /**
   * Закрыть все шарды, выгрузив их из памяти. Типа заморозка.
   * @return Список isAcknowledged.
   */
  def close(implicit client:Client, ec:ExecutionContext): Future[Seq[Boolean]] = {
    Future.traverse(getShards) { closeIndex }
  }

  /**
   * Открыть все шарды этого индекса.
   * @return Список результатов от шард.
   */
  def open(implicit client:Client, ec:ExecutionContext): Future[Seq[Boolean]] = {
    Future.traverse(getShards) { openIndex }
  }

  /** Узнать, все ли необходимые шарды вирт.индекса существуют в ES?
   * @return true, если всё существует. Иначе false.
   */
  def isExist(implicit client:Client, ec:ExecutionContext): Future[Boolean] = {
    client.admin().indices()
      .prepareExists(getShards : _*)
      .execute()
      .map { _.isExists }
  }


  /** Выдать домены, официально использующие этот индекс. Ресурсоемкая операция, перебирающая всю таблицу.
   * @return Коллекция сабжей, в т.ч. пустая.
   */
  def getUsers(implicit ec: ExecutionContext) = MDVIActive.getAllForVin(vin)


  /**
   * Отключить автообновление индекса, перейдя тем самым в режим bulk-работы.
   * @return Фьючерс для синхронизации.
   */
  def setBulkIndexing(implicit client:Client) = setRefreshInterval(INDEX_REFRESH_INTERVAL_BULK, getShards)

  /**
   * Включить асинхронное автообновление индекса, вернувшись в нормальный режим работы индекса.
   * @return Фьючерс для синхронизации.
   */
  def unsetBulkIndexing(implicit client:Client) = setRefreshInterval(INDEX_REFRESH_INTERVAL_SECONDS_DFLT, getShards)


  /**
   * Прооптимизировать все подчиненные шарды.
   * @param maxNumSegments Макс.число сегментов в результатирующем индексе.
   * @param flush Флушить? Почти всегда - true.
   * @return Фьючерс с бесполезным содержимым.
   */
  def optimize(maxNumSegments:Int = 1, flush:Boolean = true)(implicit client:Client): Future[AnyRef] = {
    client.admin().indices()
      .prepareOptimize(getShards : _*)
      .setFlush(flush)
      .setMaxNumSegments(maxNumSegments)
      .execute()
  }

}
