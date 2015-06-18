package util.health

import io.suggest.ym.model.MAdnNode
import models.usr.MPersonIdent
import models.{CronTask, ICronTask, AdnShownTypes}
import models.msys.NodeProblem
import org.elasticsearch.client.Client
import org.joda.time.{DateTimeZone, DateTime}
import play.api.Application
import play.api.i18n.Messages
import util.mail.IMailerWrapper
import util.{PlayLazyMacroLogsImpl, TplFormatUtilT, CronTasksProvider}
import util.showcase.ShowcaseNodeListUtil
import play.api.Play.{current, configuration}
import scala.concurrent.duration._
import views.html.sys1.debug.geo.parent._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 17:18
 * Description: Поиск проблем в сети узлов.
 */
object AdnGeoParentsHealth extends CronTasksProvider with PlayLazyMacroLogsImpl with TplFormatUtilT {

  import LOGGER._

  /** Включено ли автоматическое тестирование узлов? */
  val GEO_PARENTS_AUTO = configuration.getBoolean("health.tests.adn.geo.parent.periodical") getOrElse false

  // TODO Интегрировать с cron, чтобы раз в день шла проверка и слалось письмо при проблеме.

  /** Список задач, которые надо вызывать по таймеру. */
  override def cronTasks(app: Application): TraversableOnce[ICronTask] = {
    var acc: List[ICronTask] = Nil

    if (GEO_PARENTS_AUTO) {
      trace("GeoParent self-testing enabled")
      val tz = DateTimeZone.forID("Europe/Moscow")
      val h24 = 24
      val t = CronTask(
        // Тестировать надо ночью или утром наверное. Тест тяжеловат по нагрузке.
        startDelay  = (h24 - DateTime.now().withZone(tz).getHourOfDay).hours,
        every       = h24.hours,
        displayName = "testAllGeoParents()"
      )(testAllPerdiodic())
      acc ::= t
    }

    acc
  }


  /**
   * Запуск тестирования узлов, имеющих direct geo parents.
   * Тест проверяет возможность узла на рендер внутри списка узлов.
   * @return Фьючерс со списком обнаруженных проблем.
   */
  def testAll()(implicit ec: ExecutionContext, client: Client, msgs: Messages): Future[List[NodeProblem]] = {
    // TODO Фильтровать узлы по наличию directGeoParent. Сейчас фильтрация идёт через if внутри тела функции-предиката.
    MAdnNode.foldLeftAsync(acc0 = List.empty[NodeProblem]) { (acc0Fut, mnode) =>
      if (mnode.geo.directParentIds.isEmpty) {
        // Узел не привязан к другим узлам географически, поэтому просто пропускаем его.
        acc0Fut

      } else {
        val testsFut = AdnShownTypes.withName(mnode.adn.shownTypeId).ngls.map { ngl =>
          // Для проверки валидности пропихиваем этот узел в ShowcaseNodeListUtil и анализируем результат.
          ShowcaseNodeListUtil.collectLayers(geoMode = None, currNode = mnode, currNodeLayer = ngl)
            .filter { _.nonEmpty }
            .map { _ => Option.empty[NodeProblem] }
            .recover { case ex: Throwable =>
              Some( NodeProblem(mnode, ex) )
            }
        }
        Future.fold(testsFut)(acc0Fut) { (_acc0Fut, testResOpt) =>
          testResOpt.fold(_acc0Fut) { testRes =>
            _acc0Fut.map { testRes :: _ }
          }
        }
        .flatMap(identity)
      }
    }
  }


  /** Запуск тестирования по крону. Собрать параметры для вызова, организовать рассылку сообщений о найденных ошибках. */
  def testAllPerdiodic(): Unit = {
    debug("Starting periodical self-testing of adn geo.parent consistency...")

    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    import util.SiowebEsUtil.client
    import play.api.i18n.Lang.defaultLang
    import play.api.i18n.Messages.Implicits.applicationMessages

    testAll().onComplete {
      case Success(nil) if nil.isEmpty =>
        debug("Test finished without problems.")

      // Или есть проблемы, или возникла ошибка при тесте. В обоих случаях надо уведомить.
      case tryRes =>
        warn("Selt-test problems detected: " + tryRes)
        current.injector
          .instanceOf[IMailerWrapper]
          .instance
          .setFrom("health@suggest.io")
          .setRecipients(MPersonIdent.SU_EMAILS : _*)
          .setSubject("Suggest.io: Обнаружены проблемы геосвязности узлов")
          .setHtml( suProblemsEmailTpl(tryRes) )
          .send()
    }
  }

}
