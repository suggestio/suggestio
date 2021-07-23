package io.suggest.stat.inx

import java.time.Instant

import io.suggest.es.MappingDsl.Implicits._
import javax.inject.Inject
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.{EsIndexUtil, EsModel}
import io.suggest.stat.m.{MStatIndexes, MStatInxInfo, MStatsModel, MStatsTmp}
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImpl
import org.threeten.extra.Interval
import play.api.inject.Injector
import japgolly.univeq._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 16:16
  * Description: Утиль для работы со stat-индексами.
  */
final class StatIndexUtil @Inject() (
                                      injector          : Injector,
                                    )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mStatsModel = injector.instanceOf[MStatsModel]
  private lazy val mStatIndexes = injector.instanceOf[MStatIndexes]
  implicit private val ec = injector.instanceOf[ExecutionContext]


  /** Максимальное кол-во документов в одном stat-индексе.
    * Если индекс уже перешагивает через это кол-во, то пора делать reNewIndex(). */
  def MAX_DOC_COUNT_PER_INDEX = 1000000

  /** Максимальное число дней с момента последней записи, после которого stat-индекс считается слишком старым. */
  def MAX_INDEX_DAYS = 200


  /** Переключиться на новый stat-индекс. */
  def reNewCurrIndex(): Future[String] = {
    import esModel.api._

    // Подготовить новое имя индекса.
    val newInxName = EsIndexUtil.newIndexName( MStatIndexes.INDEX_ALIAS_NAME )
    lazy val logPrefix = s"reNewCurrIndex() [$newInxName] :"
    LOGGER.info(s"$logPrefix Starting. newIndexName = $newInxName")

    val okFut = for {
      // Создать новый индекс
      _ <- esModel.createIndex(newInxName, mStatIndexes.indexSettingsCreate)

      mStatsTmp = MStatsTmp( newInxName )
      // Залить туда маппинг модели статистики
      _ <- mStatsTmp.putMapping()

      // Подготовка исходного индекса завершена, переставить index alias на новый индекс.
      _ <- esModel.resetAliasToIndex(
        indexName = newInxName,
        aliasName = MStatIndexes.INDEX_ALIAS_NAME,
      )

    } yield {
      LOGGER.info(s"$newInxName Done ok.")
      newInxName
    }

    // При ошибках надо удалять новосозданный индекс.
    for (ex <- okFut.failed) {
      val deleteFut = esModel.deleteIndex( newInxName )
      LOGGER.error(s"$logPrefix Failed to reNew to index. Deleting new index...", ex)
      deleteFut.onComplete { res =>
        LOGGER.info(s"$logPrefix Emergency delete new index completed. Result: $res")
      }
    }

    okFut
  }


  /** Узнать данные по текущему индексу.
    * @return None если текущего индекса внезапно нет.
    *         Some(имя индекса).
    */
  def currIndexInfo(): Future[Option[MStatInxInfo]] = {
    for {
      // Прочитать значение алиасов.
      aliasedNames <- esModel.getAliasedIndexName( MStatIndexes.INDEX_ALIAS_NAME )

      // определить текущее (последнее) имя индекса.
      currInxNameOpt = {
        val aliasesCount = aliasedNames.size
        if (aliasesCount <= 0) {
          None
        } else if (aliasesCount ==* 1) {
          Some( aliasedNames.head )
        } else {
          aliasedNames.toSeq.sorted.lastOption
        }
      }

      // Опционально посчитать кол-во записей в текущем индексе.
      indexDocCountOpt <- FutureUtil.optFut2futOpt(currInxNameOpt) { currInxName =>
        import esModel.api._

        MStatsTmp( currInxName )
          .countAll()
          .map( EmptyUtil.someF )
      }

    } yield {
      LOGGER.trace(s"currIndexInfo(): Done ok:\n Indices: ${aliasedNames.mkString(", ")}\n last index = ${currInxNameOpt.orNull}\n doc count = ${indexDocCountOpt.orNull}")

      for {
        currInxName <- currInxNameOpt
        count       <- indexDocCountOpt
      } yield {
        MStatInxInfo(
          inxName   = currInxName,
          docCount  = count
        )
      }
    }
  }


  /**
    * Узнать, не пора ли переходить на новый индекс? Если да, то выполнить переключение.
    * Метод удобно вызывать по cron'у.
    * @return None -- ничего не сделано.
    *         Some(имя нового индекса) -- произведено обновление индекса.
    */
  def maybeReNewCurrIndex(): Future[Option[String]] = {
    def logPrefix = "maybeReNewCurrIndex():"
    currIndexInfo()
      // Failure(NSEE) - проверки индекса не пройдены, нужно обновлять его.
      // Success(_) если индекс пока обновлять не требуется.
      .filter { infoOpt =>
        // Проверка на нормальность текущей обстановки фейлиться, если:
        // - индекса не найдено вообще (should never happen)
        // - в индексе уже слишком много документов
        val info = infoOpt.get
        val docCountLimitOk = info.docCount < MAX_DOC_COUNT_PER_INDEX
        docCountLimitOk && info.inxName.startsWith( MStatIndexes.INDEX_ALIAS_NAME )
      }
      // Если проверка пройдена, то ничего reNew'ить не требуется.
      .map { infoOpt =>
        LOGGER.trace(s"$logPrefix Nothing to do, index info = $infoOpt")
        None
      }
      // Если одна из проверок была зафейлена, то произвести обновление индекса:
      .recoverWith { case _: NoSuchElementException =>
        LOGGER.debug(s"maybeReNewCurrIndex(): Current index needs to be reNewed...")
        reNewCurrIndex()
          .map( EmptyUtil.someF )
      }
  }


  /** Найти слишком старый индекс статистики. */
  def findTooOldIndex(): Future[Option[String]] = {
    val statIndexNamesFut = mStatIndexes.findStatIndices()
    lazy val logPrefix = s"findTooOldIndex(${System.currentTimeMillis()}):"
    statIndexNamesFut.flatMap { statIndexNames =>

      if (statIndexNames.isEmpty) {
        LOGGER.warn(s"$logPrefix No stat indices found.")
        Future.successful( None )

      } else {
        import mStatsModel.api._

        // При алфавитной аггрегации самый старый индекс будет в начале списка.
        val oldestInxName = statIndexNames.min

        // Проверить дату последней записи в индексе.
        val mStatsTmp = MStatsTmp( oldestInxName )
        for (latestDtOpt <- mStatsTmp.findMaxTimestamp()) yield {
          latestDtOpt.fold [Option[String]] {
            // Пустой старый индекс. Тут два варианта.
            if (statIndexNames.size ==* 1) {
              LOGGER.trace(s"$logPrefix The only index[$oldestInxName] and it is empty. Guessing it as latest fresh index and ignoring.")
              // Это единственный индекс. Значит это самый свежий индекс, просто в него ещё не записали статистики никакой.
              None
            } else {
              // Если же есть ещё другие индексы, то вероятно это некорректный индекс, у которого возникла ошибка при создании, но он не был удалён (например, рестарт системы во время reNew).
              LOGGER.warn(s"$logPrefix Dangling index[$oldestInxName]: empty and the oldest. Guessing as invalid and too old to be alive.")
              Some(oldestInxName)
            }

          } { latestDt =>
            // Индекс не пустой, и на руках есть дата последней записи из него. Проверить её старость.
            val ivl = Interval.of(latestDt.toInstant, Instant.now())
            val daysAgo = ivl.toDuration.toDays
            val maxDaysAgo = MAX_INDEX_DAYS
            def __logMsg(result: String) = s"$logPrefix Index[$oldestInxName] is $result. Latest record is $latestDt. It is $daysAgo days ago, limit is $maxDaysAgo days."
            if (daysAgo > maxDaysAgo) {
              // Индекс слишком старый.
              LOGGER.info( __logMsg("TOO OLD") )
              Some(oldestInxName)
            } else {
              LOGGER.trace( __logMsg("FRESH") )
              None
            }
          }
        }
      }
    }

  }


  /**
    * Поискать слишком старый индекс и удалить его.
    * Метод удобен для вызова по cron.
    * @return Опциональное имя найденного и стёртого индекса.
    */
  def maybeDeleteTooOldIndex(): Future[Option[String]] = {
    // Поискать слишком старый индекс:
    val findTooOldInxFut = findTooOldIndex()
    def logPrefix = s"maybeDeleteTooOldIndex():"

    for {
      inxNameOpt <- findTooOldInxFut

      // Если старый индекс найден, то удалить его.
      _ <- inxNameOpt.fold[Future[_]] {
        LOGGER.trace(s"$logPrefix Nothing to do.")
        Future.successful(None)
      } { oldInxName =>
        LOGGER.info(s"$logPrefix Index $oldInxName will be deleted right now...")
        esModel.deleteIndex( oldInxName )
      }

    } yield {
      // Вернуть результаты поиска старого индекса, которого больше нет.
      inxNameOpt
    }
  }

}


sealed trait StatIndexUtilJmxMBean {

  def reNewCurrIndex(): String

  def currIndexInfo(): String

  def maybeReNewCurrIndex(): String

  def findTooOldIndex(): String

  def maybeDeleteTooOldIndex(): String

}


final class StatIndexUtilJmx @Inject() (
                                         injector: Injector,
                                       )
  extends JmxBase
  with StatIndexUtilJmxMBean
{

  private def statIndexUtil = injector.instanceOf[StatIndexUtil]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  import io.suggest.util.JmxBase._

  override def _jmxType = Types.STAT

  private def _usingToStringFut(f: () => Future[AnyRef]): String = {
    val fut = for {
      result <- f()
    } yield {
      result.toString
    }
    awaitString(fut)
  }


  override def reNewCurrIndex(): String = {
    _usingToStringFut {
      statIndexUtil.reNewCurrIndex _
    }
  }

  override def currIndexInfo(): String = {
    _usingToStringFut {
      statIndexUtil.currIndexInfo _
    }
  }

  override def maybeReNewCurrIndex(): String = {
    _usingToStringFut {
      statIndexUtil.maybeReNewCurrIndex _
    }
  }

  override def findTooOldIndex(): String = {
    _usingToStringFut {
      statIndexUtil.findTooOldIndex _
    }
  }

  override def maybeDeleteTooOldIndex(): String = {
    _usingToStringFut {
      statIndexUtil.maybeDeleteTooOldIndex _
    }
  }

}
