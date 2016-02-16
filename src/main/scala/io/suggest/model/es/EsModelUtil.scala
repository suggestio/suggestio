package io.suggest.model.es

import java.{lang => jl, util => ju}

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.util.SioConstants._
import io.suggest.util.SioEsUtil._
import io.suggest.util._
import io.suggest.ym.model.stat._
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.Client
import org.elasticsearch.cluster.metadata.{IndexMetaData, MappingMetaData}
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.{ToXContent, XContentFactory}
import org.elasticsearch.search.SearchHits
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, ReadableInstant}
import play.api.libs.json.Reads.IsoDateReads
import play.api.libs.json._

import scala.collection.JavaConversions._
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

  /** Список ES-моделей. Нужен для удобства массовых maintance-операций. Расширяется по мере роста числа ES-моделей. */
  def ES_MODELS = List[EsModelCommonStaticT] (
    MAdStat
  )


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
  def putAllMappings(models: Seq[EsModelCommonStaticT] = ES_MODELS, ignoreExists: Boolean = false)
                    (implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
    Future.traverse(models) { esModelStatic =>
      val logPrefix = esModelStatic.getClass.getSimpleName + ".putMapping(): "
      val imeFut = if (ignoreExists) {
        Future successful false
      } else {
        esModelStatic.isMappingExists
      }
      imeFut flatMap {
        case false =>
          trace(logPrefix + "Trying to push mapping for model...")
          val fut = esModelStatic.putMapping(ignoreExists)
          fut onComplete {
            case Success(true)  => trace(logPrefix + "-> OK" )
            case Success(false) => warn(logPrefix  + "NOT ACK!!! Possibly out-of-sync.")
            case Failure(ex)    => error(logPrefix + "FAILed", ex)
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
  val UPDATE_RETRIES_MAX_DFLT = MyConfig.CONFIG.getInt("es.model.update.retries.max.dflt") getOrElse 5

  /** Имя индекса, который будет использоваться для хранения данных для большинства остальных моделей.
    * Имя должно быть коротким и лексикографически предшествовать именам остальных временных индексов. */
  val DFLT_INDEX        = "-sio"
  /** Имя индекса, куда сваливается всякий частоменяющийся/часторастущий хлам. */
  val GARBAGE_INDEX     = "-siostat"


  // Имена полей в разных хранилищах. НЕЛЬЗЯ менять их значения.
  val PERSON_ID_ESFN    = "personId"
  val KEY_ESFN          = "key"
  val VALUE_ESFN        = "value"
  val IS_VERIFIED_ESFN  = "isVerified"

  val MAX_RESULTS_DFLT = 100
  val OFFSET_DFLT = 0
  val SCROLL_KEEPALIVE_MS_DFLT = 60000L

  /** Дефолтовый размер скролла, т.е. макс. кол-во получаемых за раз документов. */
  val SCROLL_SIZE_DFLT = 10

  /** number of actions, после которого bulk processor делает flush. */
  val BULK_PROCESSOR_BULK_SIZE_DFLT = 100

  /** Тип аккамулятора, который используется во [[EsModelPlayJsonT]].writeJsonFields(). */
  type FieldsJsonAcc = List[(String, JsValue)]

  /** Отрендерить экземпляр модели в JSON, обёрнутый в некоторое подобие метаданных ES (без _index и без _type). */
  def toEsJsonDoc(e: EsModelCommonT): String = {
     var kvs = List[String] (s""" "$FIELD_SOURCE": ${e.toJson}""")
    if (e.versionOpt.isDefined)
      kvs ::= s""" "$FIELD_VERSION": ${e.versionOpt.get}"""
    if (e.id.isDefined)
      kvs ::= s""" "$FIELD_ID": "${e.id.get}" """
    kvs.mkString("{",  ",",  "}")
  }

  /** Отрендерить экземпляры моделей в JSON. */
  def toEsJsonDocs(e: Traversable[EsModelCommonT]): String = {
    e.map { toEsJsonDoc }
      .mkString("[",  ",\n",  "]")
  }

  private def _parseEx(as: String, v: Any = null) = {
    throw new IllegalArgumentException(s"unable to parse '$v' as $as.")
  }

  // ES-выхлопы страдают динамической типизацией, поэтому нужна коллекция парсеров для примитивных типов.
  // Следует помнить, что любое поле может быть списком значений.
  val intParser: PartialFunction[Any, Int] = {
    case n @ null => _parseEx("int")
    case is: jl.Iterable[_] =>
      intParser(is.head.asInstanceOf[AnyRef])
    case i: Integer => i.intValue()
  }
  val longParser: PartialFunction[Any, Long] = {
    case n @ null =>
      _parseEx("long")
    case ls: jl.Iterable[_] =>
      longParser(ls.head.asInstanceOf[AnyRef])
    case l: jl.Number => l.longValue()
  }
  val floatParser: PartialFunction[Any, Float] = {
    case null =>
      _parseEx("float")
    case fs: jl.Iterable[_] =>
      floatParser(fs.head.asInstanceOf[AnyRef])
    case f: jl.Number => f.floatValue()
  }
  val doubleParser: PartialFunction[Any, Double] = {
    case n @ null =>
      _parseEx("double", n)
    case fs: jl.Iterable[_] =>
      doubleParser(fs.head.asInstanceOf[AnyRef])
    case f: jl.Number => f.doubleValue()
  }
  val stringParser: PartialFunction[Any, String] = {
    case null =>
      _parseEx("string")
    case strings: jl.Iterable[_] =>
      stringParser(strings.head.asInstanceOf[AnyRef])
    case s: String  => s
  }
  val booleanParser: PartialFunction[Any, Boolean] = {
    case null =>
      _parseEx("bool")
    case bs: jl.Iterable[_] =>
      booleanParser(bs.head.asInstanceOf[AnyRef])
    case b: jl.Boolean => b.booleanValue()
  }
  val dateTimeParser: PartialFunction[Any, DateTime] = {
    case null => null
    case dates: jl.Iterable[_] =>
      dateTimeParser(dates.head.asInstanceOf[AnyRef])
    case s: String           => new DateTime(s)
    case d: ju.Date          => new DateTime(d)
    case d: DateTime         => d
    case ri: ReadableInstant => new DateTime(ri)
  }


  /** Неявные парсеры собраны тут и импортируются при необходимости. */
  object Implicits {

    /** Стандартный joda DateTime парсер в play разработан через одно место, поэтому тут собираем свой. */
    implicit val jodaDateTimeReads: Reads[DateTime] = {
      IsoDateReads
        .map { new DateTime(_) }
    }

    implicit val jodaDateTimeWrites: Writes[DateTime] = new Writes[DateTime] {
      def df = ISODateTimeFormat.dateTime()
      def writes(d: DateTime): JsValue = {
        JsString( d.toString(df) )
      }
    }


    implicit val jodaDateTimeFormat: Format[DateTime] = {
      Format(jodaDateTimeReads, jodaDateTimeWrites)
    }

  }


  /** Парсер json-массивов. */
  val iteratorParser: PartialFunction[Any, Iterator[Any]] = {
    case null =>
      Iterator.empty
    case l: jl.Iterable[_] =>
      l.iterator()
  }

  /** Парсер список строк. */
  val strListParser: PartialFunction[Any, List[String]] = {
    iteratorParser andThen { iter =>
      iter.foldLeft( List.empty[String] ) {
        (acc,e) => EsModelUtil.stringParser(e) :: acc
      }.reverse
    }
  }
  
  
  val SHARDS_COUNT_DFLT   = MyConfig.CONFIG.getInt("es.model.shards.count.dflt") getOrElse 5
  val REPLICAS_COUNT_DFLT = MyConfig.CONFIG.getInt("es.model.replicas.count.dflt") getOrElse 1


  /**
   * Убедиться, что индекс существует.
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
  def ensureEsModelsIndices(models: Seq[EsModelCommonStaticT] = ES_MODELS)
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
      Future successful acc0
    } else {
      // Запустить в фоне получение следующей порции результатов
      LOGGER.trace(s"$logPrefix has ${hits.getHits.length} hits, total = ${hits.getTotalHits}")
      // Убеждаемся, что scroll выставлен. Имеет смысл проверять это только на первом запросе.
      if (firstReq)
        assert(scrollId != null && !scrollId.isEmpty, "Scrolling looks like disabled. Cannot continue.")
      val nextScrollRespFut = client.prepareSearchScroll(scrollId)
        .setScroll(new TimeValue(keepAliveMs))
        .execute()
      // Синхронно залить результаты текущего реквеста в аккамулятор
      val acc1Fut = f(acc0, hits)
      // Асинхронно перейти на следующую итерацию, дождавшись новой порции результатов.
      nextScrollRespFut flatMap { searchResp2 =>
        acc1Fut flatMap { acc1 =>
          foldSearchScroll(searchResp2, acc1, firstReq = false, keepAliveMs)(f)
        }
      }
    }
  }

  // Сериализация дат
  val dateFormatterDflt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)

  def date2str(dateTime: ReadableInstant): String = dateFormatterDflt.print(dateTime)
  def date2JsStr(dateTime: ReadableInstant): JsString = JsString( date2str(dateTime) )


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


  /** Общий код моделей, которые занимаются resave'ом. */
  def resaveBase(getFut: Future[Option[EsModelCommonT]])
                (implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Option[String]] = {
    getFut flatMap {
      case Some(e) =>
        e.save map { Some.apply }
      case None =>
        Future successful None
    }
  }

}


/**
 * Результаты работы метода Model.copyContent() возвращаются в этом контейнере.
 * @param success Кол-во успешных документов, т.е. для которых выполнена чтене и запись.
 * @param failed Кол-во обломов.
 */
case class CopyContentResult(success: Long, failed: Long)



/** Интерфейс для стирания данных, относящихся только к текущему экземпляру модели, но хранящимися в других моделях. */
trait EraseResources {

  /** Вызывалка стирания ресурсов. Позволяет переопределить логику вызова doEraseResources(). */
  def eraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    doEraseResources
  }
  
  /** Логика стирания ресурсов, относящихся к этой модели. Например, картинок, на которые ссылкаются поля этой модели. */
  protected def doEraseResources(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    Future successful None
  }

}

