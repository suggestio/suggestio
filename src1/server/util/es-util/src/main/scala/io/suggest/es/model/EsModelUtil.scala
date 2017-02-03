package io.suggest.es.model

import io.suggest.es.util.SioEsUtil
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.es.util.SioEsUtil._
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.cluster.metadata.{IndexMetaData, MappingMetaData}
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.{ToXContent, XContentFactory}
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.search.SearchHits
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 14:41
 * Description: Общее для elasticsearch-моделей лежит в этом файле. Обычно используется общий индекс для хранилища.
 * 2014.sep.04: Появились child-модели. Произошло разделение api трейтов: статических и немного динамических.
 */
object EsModelUtil extends MacroLogsImpl {

  import LOGGER._


  private def esModelId(esModel: EsModelCommonStaticT): String = {
    s"${esModel.ES_INDEX_NAME}/${esModel.ES_TYPE_NAME}"
  }

  /** Сгенерить InternalError, если хотя бы две es-модели испрользуют одно и тоже хранилище для данных.
    * В сообщении экзепшена будут перечислены конфликтующие модели. */
  def errorIfIncorrectModels(allModels: Iterable[EsModelCommonStaticT]): Unit = {
    // Запускаем проверку, что в моделях не используются одинаковые типы в одинаковых индексах.
    val uniqModelsCnt = allModels.iterator
      .map(esModelId)
      .toSet
      .size
    if (uniqModelsCnt < allModels.size) {
      // Найдены модели, которые испрользуют один и тот же индекс+тип. Нужно вычислить их и вернуть в экзепшене.
      val errModelsStr = allModels
        .map { m => esModelId(m) -> m.getClass.getName }
        .groupBy(_._1)
        .valuesIterator
        .filter { _.size > 1 }
        .map { _.mkString(", ") }
        .mkString("\n")
      throw new InternalError("Two or more es models using same index+type for data store:\n" + errModelsStr)
    }
  }


  /** Отправить маппинги всех моделей в ES. */
  def putAllMappings(models: Seq[EsModelCommonStaticT], ignoreExists: Boolean = false)
                    (implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
    Future.traverse(models) { esModelStatic =>
      val logPrefix = esModelStatic.getClass.getSimpleName + ".putMapping(): "
      val imeFut = if (ignoreExists) {
        Future.successful(false)
      } else {
        esModelStatic.isMappingExists()
      }
      imeFut.flatMap {
        case false =>
          trace(logPrefix + "Trying to push mapping for model...")
          val fut = esModelStatic.putMapping()
          fut.onComplete {
            case Success(true)  =>
              trace(logPrefix + "-> OK" )
            case Success(false) =>
              warn(logPrefix  + "NOT ACK!!! Possibly out-of-sync.")
            case Failure(ex)    =>
              error(s"$logPrefix FAILed to put mapping to ${esModelStatic.ES_INDEX_NAME}/${esModelStatic.ES_TYPE_NAME}:\n-------------\n${esModelStatic.generateMapping.string()}\n-------------\n", ex)
          }
          fut

        case true =>
          trace(logPrefix + "Mapping already exists in index. Skipping...")
          Future successful true
      }
    } map {
      _.reduceLeft { _ && _ }
    }
  }

  /** Сколько раз по дефолту повторять попытку update при конфликте версий. */
  def UPDATE_RETRIES_MAX_DFLT = 5

  /** Имя индекса, который будет использоваться для хранения данных для большинства остальных моделей.
    * Имя должно быть коротким и лексикографически предшествовать именам остальных временных индексов. */
  val DFLT_INDEX        = "-sio"


  // Имена полей в разных хранилищах. НЕЛЬЗЯ менять их значения.
  val PERSON_ID_ESFN    = "personId"
  val KEY_ESFN          = "key"
  val VALUE_ESFN        = "value"
  val IS_VERIFIED_ESFN  = "isVerified"

  def MAX_RESULTS_DFLT = 100
  def OFFSET_DFLT = 0
  def SCROLL_KEEPALIVE_MS_DFLT = 60000L

  /** Дефолтовый размер скролла, т.е. макс. кол-во получаемых за раз документов. */
  def SCROLL_SIZE_DFLT = 10

  /** number of actions, после которого bulk processor делает flush. */
  def BULK_PROCESSOR_BULK_SIZE_DFLT = 100


  def SHARDS_COUNT_DFLT   = 5
  def REPLICAS_COUNT_DFLT = 1


  /**
    * Убедиться, что индекс существует.
    *
    * @return Фьючерс для синхронизации работы. Если true, то новый индекс был создан.
    *         Если индекс уже существует, то false.
    */
  def ensureIndex(indexName: String, shards: Int = 5, replicas: Int = 1)
                 (implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    val adm = client.admin().indices()
    adm.prepareExists(indexName).execute().flatMap { existsResp =>
      if (existsResp.isExists) {
        Future.successful(false)
      } else {
        val indexSettings = SioEsUtil.getIndexSettingsV2(shards=shards, replicas=replicas)
        adm.prepareCreate(indexName)
          .setSettings(indexSettings)
          .execute()
          .map { _ => true }
      }
    }
  }

  /** Пройтись по всем ES_MODELS и проверить, что всех ихние индексы существуют. */
  def ensureEsModelsIndices(models: Seq[EsModelCommonStaticT])
                           (implicit ec:ExecutionContext, client: Client): Future[_] = {
    val indices = models.map { esModel =>
      esModel.ES_INDEX_NAME -> (esModel.SHARDS_COUNT, esModel.REPLICAS_COUNT)
    }.toMap
    Future.traverse(indices) {
      case (inxName, (shards, replicas)) =>
        ensureIndex(inxName, shards=shards, replicas=replicas)
    }
  }

  /**
   * Узнать метаданные индекса.
   *
   * @param indexName Название индекса.
   * @return Фьючерс с опциональными метаданными индекса.
   */
  def getIndexMeta(indexName: String)(implicit ec: ExecutionContext, client: Client): Future[Option[IndexMetaData]] = {
    client.admin().cluster()
      .prepareState()
      .setIndices(indexName)
      .execute()
      .map { cs =>
        val maybeResult = cs.getState
          .getMetaData
          .index(indexName)
        Option(maybeResult)
      }
  }

  /**
   * Прочитать метаданные маппинга.
   *
   * @param indexName Название индекса.
   * @param typeName Название типа.
   * @return Фьючерс с опциональными метаданными маппинга.
   */
  def getIndexTypeMeta(indexName: String, typeName: String)
                      (implicit ec: ExecutionContext, client: Client): Future[Option[MappingMetaData]] = {
    getIndexMeta(indexName) map { imdOpt =>
      imdOpt.flatMap { imd =>
        Option(imd.mapping(typeName))
      }
    }
  }

  /**
   * Существует ли указанный маппинг в хранилище? Используется, когда модель хочет проверить наличие маппинга
   * внутри общего индекса.
   *
   * @param typeName Имя типа.
   * @return Да/нет.
   */
  def isMappingExists(indexName: String, typeName: String)
                     (implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    getIndexTypeMeta(indexName, typeName = typeName)
      .map { _.isDefined }
  }

  /** Прочитать текст маппинга из хранилища. */
  def getCurrentMapping(indexName: String, typeName: String)
                       (implicit ec: ExecutionContext, client: Client): Future[Option[String]] = {
    getIndexTypeMeta(indexName, typeName = typeName) map {
      _.map { _.source().string() }
    }
  }

  /** Сериализация набора строк. */
  def asJsonStrArray(strings : Iterable[String]): JsArray = {
    val strSeq = strings.foldLeft [List[JsString]] (Nil) {(acc, e) =>
      JsString(e) :: acc
    }
    JsArray(strSeq)
  }

  /**
   * Собрать указанные значения id'шников в аккамулятор-множество.
   *
   * @param searchResp Экземпляр searchResponse.
   * @param acc0 Начальный акк.
   * @param keepAliveMs keepAlive для курсоров на стороне сервера ES в миллисекундах.
   * @return Фьчерс с результирующим аккамулятором-множеством.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html#scrolling]]
   */
  def searchScrollResp2ids(searchResp: SearchResponse, maxAccLen: Int, firstReq: Boolean, currAccLen: Int = 0,
                           acc0: List[String] = Nil, keepAliveMs: Long = 60000L)
                          (implicit ec: ExecutionContext, client: Client): Future[List[String]] = {
    val hits = searchResp.getHits.getHits
    if (!firstReq && hits.isEmpty) {
      Future successful acc0
    } else {
      val nextAccLen = currAccLen + hits.length
      val canContinue = maxAccLen <= 0 || nextAccLen < maxAccLen
      val nextScrollRespFut = if (canContinue) {
        // Лимит длины акк-ра ещё не пробит. Запустить в фоне получение следующей порции результатов...
        client.prepareSearchScroll(searchResp.getScrollId)
          .setScroll(new TimeValue(keepAliveMs))
          .execute()
      } else {
        null
      }
      // Если акк заполнен, то надо запустить очистку курсора на стороне ES.
      if (!canContinue) {
        client.prepareClearScroll().addScrollId(searchResp.getScrollId).execute()
      }
      // Синхронно залить результаты текущего реквеста в аккамулятор
      val accNew = hits.foldLeft[List[String]] (acc0) { (acc1, hit) =>
        hit.getId :: acc1
      }
      if (canContinue) {
        // Асинхронно перейти на следующую итерацию, дождавшись новой порции результатов.
        nextScrollRespFut flatMap { searchResp2 =>
          searchScrollResp2ids(searchResp2, maxAccLen, firstReq = false, currAccLen = nextAccLen, acc0 = accNew, keepAliveMs = keepAliveMs)
        }
      } else {
        // Пробит лимит аккамулятора по maxAccLen - вернуть акк не продолжая обход.
        Future successful accNew
      }
    }
  }


  /** Рекурсивная асинхронная сверстка скролл-поиска в ES.
    * Перед вызовом функции надо выпонить начальный поисковый запрос, вызвав с setScroll() и,
    * по возможности, включив SCAN.
    *
    * @param searchResp Результат выполненного поиского запроса с активным scroll'ом.
    * @param acc0 Исходное значение аккамулятора.
    * @param firstReq Флаг первого запроса. По умолчанию = true.
    *                 В первом и последнем запросах не приходит никаких результатов, и их нужно различать.
    * @param keepAliveMs Значение keep-alive для курсора на стороне ES.
    * @param f fold-функция, генереящая на основе результатов поиска и старого аккамулятора новый аккамулятор типа A.
    * @tparam A Тип аккамулятора.
    * @return Данные по результатам операции, включающие кол-во удач и ошибок.
    */
  def foldSearchScroll[A](searchResp: SearchResponse, acc0: A, firstReq: Boolean = true, keepAliveMs: Long = SCROLL_KEEPALIVE_MS_DFLT)
                         (f: (A, SearchHits) => Future[A])
                         (implicit ec: ExecutionContext, client: Client): Future[A] = {
    val hits = searchResp.getHits
    val scrollId = searchResp.getScrollId
    lazy val logPrefix = s"foldSearchScroll($scrollId, 1st=$firstReq):"
    if (!firstReq  &&  hits.getHits.isEmpty) {
      LOGGER.trace(s"$logPrefix no more hits.")
      Future.successful(acc0)
    } else {
      // Запустить в фоне получение следующей порции результатов
      LOGGER.trace(s"$logPrefix has ${hits.getHits.length} hits, total = ${hits.getTotalHits}")
      // Убеждаемся, что scroll выставлен. Имеет смысл проверять это только на первом запросе.
      if (firstReq)
        assert(scrollId != null && !scrollId.isEmpty, "Scrolling looks like disabled. Cannot continue.")
      val nextScrollRespFut: Future[SearchResponse] = {
        client.prepareSearchScroll(scrollId)
          .setScroll(new TimeValue(keepAliveMs))
          .execute()
      }
      // Синхронно залить результаты текущего реквеста в аккамулятор
      val acc1Fut = f(acc0, hits)
      // Асинхронно перейти на следующую итерацию, дождавшись новой порции результатов.
      nextScrollRespFut.flatMap { searchResp2 =>
        acc1Fut.flatMap { acc1 =>
          foldSearchScroll(searchResp2, acc1, firstReq = false, keepAliveMs)(f)
        }
      }
    }
  }


  /** Десериализовать тело документа внутри GetResponse в строку. */
  def deserializeGetRespBodyRawStr(getResp: GetResponse): Option[String] = {
    if (getResp.isExists) {
      val result = getResp.getSourceAsString
      Some(result)
    } else {
      None
    }
  }


  /** Десериализовать документ всырую, вместе с _id, _type и т.д. */
  def deserializeGetRespFullRawStr(getResp: GetResponse): Option[String] = {
    if (getResp.isExists) {
      val xc = getResp.toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS)
      val result = xc.string()
      Some(result)
    } else {
      None
    }
  }

  /**
    * Обновление какого-то элемента с использованием es save и es optimistic locking.
    * В отличии от оригинального [[EsModelStaticT]].tryUpdate(), здесь обновляемые данные не обязательно
    * являются элементами той же модели, а являются контейнером для них..
    *
    * @param data0 Обновляемые данные.
    * @param maxRetries Максимальное кол-во попыток [5].
    * @param updateF Обновление
    * @tparam D Тип обновляемых данных.
    * @return Удачно-сохраненный экземпляр data: T.
    */
  def tryUpdate[X <: EsModelCommonT, D <: ITryUpdateData[X, D]]
               (companion: EsModelCommonStaticT { type T = X }, data0: D, maxRetries: Int = 5)
               (updateF: D => Future[D])
               (implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[D] = {
    lazy val logPrefix = s"tryUpdate(${System.currentTimeMillis}):"

    val data1Fut = updateF(data0)

    if (data1Fut == null) {
      LOGGER.debug(logPrefix + " updateF() returned `null`, leaving update")
      Future.successful(data0)

    } else {
      data1Fut.flatMap { data1 =>
        val m2 = data1._saveable
        if (m2 == null) {
          LOGGER.debug(logPrefix + " updateF() data with `null`-saveable, leaving update")
          Future.successful(data1)
        } else {
          // TODO Спилить обращение к companion, принимать статическую модель в аргументах
          companion
            .save(m2)
            .map { _ => data1 }
            .recoverWith {
              case exVsn: VersionConflictEngineException =>
                if (maxRetries > 0) {
                  val n1 = maxRetries - 1
                  LOGGER.warn(s"$logPrefix Version conflict while tryUpdate(). Retry ($n1)...")
                  data1._reget.flatMap { data2 =>
                    tryUpdate[X, D](companion, data2, n1)(updateF)
                  }
                } else {
                  val ex2 = new RuntimeException(s"$logPrefix Too many save-update retries failed", exVsn)
                  Future.failed(ex2)
                }
            }
        }
      }
    }
  }

}


/** Интерфейс контейнера данных для вызова [[EsModelUtil]].tryUpdate(). */
trait ITryUpdateData[X <: EsModelCommonT, TU <: ITryUpdateData[X, TU]] {

  /** Экземпляр, пригодный для сохранения. */
  def _saveable: X

  /** Данные для сохранения вызвали конфликт и возможно потеряли актуальность,
    * подготовить новые данные для сохранени. */
  def _reget: Future[TU]

}


/**
 * Результаты работы метода Model.copyContent() возвращаются в этом контейнере.
 *
 * @param success Кол-во успешных документов, т.е. для которых выполнена чтене и запись.
 * @param failed Кол-во обломов.
 */
case class CopyContentResult(success: Long, failed: Long)
