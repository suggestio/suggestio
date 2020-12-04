package util.cron

import akka.actor.{ActorSystem, Cancellable}
import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.mcron.MCronTask
import play.api.inject.{ApplicationLifecycle, Injector}
import util.billing.cron.BillingCronTasks
import util.geo.IpGeoBaseImport
import util.img.cron.{LocalImgsDeleteEmptyDirs, LocalImgsDeleteNotExistingInPermanent}
import util.stat.StatCronTasks
import io.suggest.common.empty.OptionUtil.BoolOptOps
import japgolly.univeq._
import play.api.Configuration
import util.adv.geo.tag.GeoTagsUtilJmx
import util.up.CleanStaleUploads

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.13 11:42
 * Description: Запускалка периодических задач, некий cron, запускающий указанные функции по таймеру.
 *
 * Реализация происходит через akka scheduler и статический набор события расписания.
 * По мотивам http://stackoverflow.com/a/13469308
 *
 * Чтобы cron-классы не висели в памяти постоянно (при их нужности раз в час-день),
 * тут активно используется инжекция по class-тегам, а инстансы не хранятся нигде.
 */

// TODO Вынести cron в отдельный пакет, на крайняк в util или ещё куда-нибудь. Чтобы список модулей на вход получал через reference.conf.

case class Crontab @Inject() (
                               injector     : Injector,
                             )
  extends MacroLogsImpl
{

  def CRON_TASKS_ENABLED: Boolean = {
    injector
      .instanceOf[Configuration]
      .getOptional[Boolean]("cron.enabled")
      .getOrElseTrue
  }

  def TASK_CLASSES: LazyList[ClassTag[_ <: ICronTasksProvider]] = {
    implicitly[ClassTag[BillingCronTasks]] #::
    implicitly[ClassTag[IpGeoBaseImport]] #::
    implicitly[ClassTag[StatCronTasks]] #::
    implicitly[ClassTag[LocalImgsDeleteEmptyDirs]] #::
    implicitly[ClassTag[LocalImgsDeleteNotExistingInPermanent]] #::
    implicitly[ClassTag[CleanStaleUploads]] #::
    implicitly[ClassTag[GeoTagsUtilJmx]] #::
    LazyList.empty
  }


  // Constructor -------------------------------

  // akka-2.5+: Чтобы избегать экзепшенов прямо в конструкторе, запуск таймеров скидываем в отдельный тред.
  if (CRON_TASKS_ENABLED) {
    implicit val ec = injector.instanceOf[ExecutionContext]

    Future {
      startTimers()
    }.andThen {
      case Success(startedTimers) =>
        // Убрать таймеры по завершению:
        injector
          .instanceOf[ApplicationLifecycle]
          .addStopHook { () =>
            Future {
              for (c <- startedTimers) {
                try {
                  c.cancel()
                } catch {
                  case ex: Throwable =>
                    LOGGER.warn(s"Cannot stop cron task $c", ex)
                }
              }
              LOGGER.trace(s"Stopped all ${startedTimers.length} crontab tasks.")
            }
          }

      case Failure(ex) =>
        LOGGER.error("startTimers() totally failed! Crontab doesn't work at all.", ex)
    }

  } else {
    LOGGER.warn(s"${getClass.getSimpleName} DISABLED in configuration/code.")
  }


  // API ---------------------------------------

  def startTimers()(implicit ec: ExecutionContext): List[Cancellable] = {
    lazy val sched = injector.instanceOf[ActorSystem].scheduler

    (for {
      cronTaskProvCt <- TASK_CLASSES
      clazz = injector.instanceOf( cronTaskProvCt )
      if clazz.isEnabled
      task  <- clazz.cronTasks()
    } yield {
      LOGGER.trace(s"Adding cron task ${clazz.getClass.getSimpleName}/${task.displayName}: delay=${task.startDelay}, every=${task.every}")
      sched.scheduleWithFixedDelay(task.startDelay, task.every) {
        // Замкнуть Runnable на Crontab, чтобы по сигналу заново искался и инжектировался инстанс необходимого класса, таски пролистывались?
        _mkRunnable(
          clazzName = clazz.getClass.getName,
          taskDisplayName = task.displayName
        )
      }
    })
      .toList
  }


  /** Сборка runnable для описываемого задания.
    * Избегаем классов и тасков в scope, чтобы их можно было выкинуть из памяти.
    */
  private def _mkRunnable(clazzName: String, taskDisplayName: String): Runnable = {
    new Runnable {
      override def run(): Unit = {
        try {
          LOGGER.trace(s"Executing task $taskDisplayName ...")
          _onTaskTimer(
            clazzName       = clazzName,
            taskDisplayName = taskDisplayName,
          )
        } catch {
          case ex: Throwable =>
            LOGGER.error(s"Cron task $clazzName/'$taskDisplayName' failed to complete", ex)
        }
      }
    }
  }


  /** Срабатывание какого-то таймера. Заново найти задачу среди task-классов. */
  private def _onTaskTimer(clazzName: String, taskDisplayName: String): Unit = {
    (for {
      clazzCt <- TASK_CLASSES.iterator
      if clazzCt.runtimeClass.getName ==* clazzName
      clazz = injector.instanceOf( clazzCt )
      task <- clazz.cronTasks()
      if task.displayName ==* taskDisplayName
    } yield
      task
    )
      // По идее, может быть только одна задача с указанным идентификатором. Поэтому headOption и только первый элемент.
      .next()
      .run()
  }

}


/** Интерфейс для модулей, предоставляющих периодические задачи. */
trait ICronTasksProvider {

  def isEnabled: Boolean = true

  /** Список задач, которые надо вызывать по таймеру. */
  def cronTasks(): Iterable[MCronTask]

}
