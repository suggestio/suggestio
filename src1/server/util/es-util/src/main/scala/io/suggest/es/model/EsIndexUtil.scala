package io.suggest.es.model


import io.suggest.es.util.SioEsUtil
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.es.util.SioEsUtil._
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.cluster.metadata.{IndexMetaData, MappingMetaData}
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.{ToXContent, XContentFactory}
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.elasticsearch.search.SearchHits

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import org.elasticsearch.client.Client

import scala.concurrent.ExecutionContext

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 12:42
  * Description: Совсем статическая утиль для динамических индексов.
  */
object EsIndexUtil {

  def DELIM = "-"

  /** java8 dt formatter. */
  def dtSuffixFmt = DateTimeFormatter.ofPattern("yyMMdd-HHmmss")

  /** Генерация нового имени скользящего во времени индекса (с точностью до секунды). */
  def newIndexName(prefix: String): String = {
    prefix + DELIM + dtSuffixFmt.format( ZonedDateTime.now() )
  }

}


@Singleton
class EsIndexUtil @Inject()(
                             implicit ec: ExecutionContext, esClient: Client
                           ) {

  // TODO Код ниже уровня моделей унести в classs EsIndexUtil.
  /**
    * Убедиться, что индекс существует.
    *
    * @return Фьючерс для синхронизации работы. Если true, то новый индекс был создан.
    *         Если индекс уже существует, то false.
    */
  def ensureIndex(indexName: String, shards: Int = 5, replicas: Int = 1): Future[Boolean] = {
    for {
      existsResp <- esClient.admin().indices()
        .prepareExists(indexName)
        .executeFut()

      _ <- if (existsResp.isExists) {
        Future.successful(false)
      } else {
        val indexSettings = SioEsUtil.getIndexSettingsV2(shards=shards, replicas=replicas)
        esClient.admin().indices()
          .prepareCreate(indexName)
          .setSettings(indexSettings)
          .executeFut()
          .map { _ => true }
      }
    } yield {
      true
    }
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
                           acc0: List[String] = Nil, keepAliveMs: Long = 60000L): Future[List[String]] = {
    val hits = searchResp.getHits.getHits
    if (!firstReq && hits.isEmpty) {
      Future successful acc0
    } else {
      val nextAccLen = currAccLen + hits.length
      val canContinue = maxAccLen <= 0 || nextAccLen < maxAccLen
      val nextScrollRespFut = if (canContinue) {
        // Лимит длины акк-ра ещё не пробит. Запустить в фоне получение следующей порции результатов...
        esClient
          .prepareSearchScroll(searchResp.getScrollId)
          .setScroll(new TimeValue(keepAliveMs))
          .executeFut()
      } else {
        null
      }
      // Если акк заполнен, то надо запустить очистку курсора на стороне ES.
      if (!canContinue) {
        esClient
          .prepareClearScroll()
          .addScrollId( searchResp.getScrollId )
          .executeFut()
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



  /**
   * Узнать метаданные индекса.
   *
   * @param indexName Название индекса.
   * @return Фьючерс с опциональными метаданными индекса.
   */
  def getIndexMeta(indexName: String): Future[Option[IndexMetaData]] = {
    esClient.admin().cluster()
      .prepareState()
      .setIndices(indexName)
      .executeFut()
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
  def getIndexTypeMeta(indexName: String, typeName: String): Future[Option[MappingMetaData]] = {
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
  def isMappingExists(indexName: String, typeName: String): Future[Boolean] = {
    getIndexTypeMeta(indexName, typeName = typeName)
      .map { _.isDefined }
  }

  /** Прочитать текст маппинга из хранилища. */
  def getCurrentMapping(indexName: String, typeName: String): Future[Option[String]] = {
    getIndexTypeMeta(indexName, typeName = typeName) map {
      _.map { _.source().string() }
    }
  }

}
