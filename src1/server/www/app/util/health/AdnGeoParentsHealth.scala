package util.health

import java.time.{ZoneId, ZonedDateTime}

import javax.inject.{Inject, Singleton}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.MNodes
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.common.empty.OptionUtil.BoolOptOps
import models.AdnShownTypes
import models.mcron.{ICronTask, MCronTask}
import models.mctx.ContextUtil
import models.mproj.ICommonDi
import models.msys.NodeProblem
import models.usr.MSuperUsers
import play.api.i18n.Lang
import util.mail.IMailerWrapper
import util.showcase.ShowcaseNodeListUtil
import util.cron.ICronTasksProvider
import views.html.sys1.debug.geo.parent._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 17:18
 * Description: Поиск проблем в сети узлов.
 */
@Singleton
class AdnGeoParentsHealth @Inject() (
  mailer                : IMailerWrapper,
  mSuperUsers           : MSuperUsers,
  mNodes                : MNodes,
  scNlUtil              : ShowcaseNodeListUtil,
  ctxUtil               : ContextUtil,
  mCommonDi             : ICommonDi
)
  extends ICronTasksProvider
  with MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Включено ли автоматическое тестирование узлов? */
  private val GEO_PARENTS_AUTO = configuration.getOptional[Boolean]("health.tests.adn.geo.parent.periodical").getOrElseFalse


  /** Список задач, которые надо вызывать по таймеру. */
  override def cronTasks(): TraversableOnce[ICronTask] = {
    var acc: List[ICronTask] = Nil

    val logVerb = if (GEO_PARENTS_AUTO) {
      val tz = ZoneId.of("Europe/Moscow")
      val h24 = 24
      val t = MCronTask(
        // Тестировать надо ночью или утром наверное. Тест тяжеловат по нагрузке.
        startDelay  = (h24 - ZonedDateTime.now().withZoneSameLocal(tz).getHour).hours,
        every       = h24.hours,
        displayName = "testAllGeoParents()"
      )(testAllPerdiodic())
      acc ::= t
      "enabled"

    } else {
      "disabled"
    }
    info("GeoParent self-testing is " + logVerb + " on this node.")

    acc
  }


  /**
   * Запуск тестирования узлов, имеющих direct geo parents.
   * Тест проверяет возможность узла на рендер внутри списка узлов.
   *
   * @return Фьючерс со списком обнаруженных проблем.
   */
  def testAll(): Future[List[NodeProblem]] = {
    implicit val msgs = messagesApi.preferred( Seq(Lang.defaultLang) )
    // TODO Фильтровать узлы по наличию directGeoParent. Сейчас фильтрация идёт через if внутри тела функции-предиката.
    mNodes.foldLeftAsync(acc0 = List.empty[NodeProblem]) { (acc0Fut, mnode) =>
      val directParentsIter = mnode.edges
        .withPredicateIter( MPredicates.GeoParent.Direct )
      if (directParentsIter.isEmpty) {
        // Узел не привязан к другим узлам географически, поэтому просто пропускаем его.
        acc0Fut

      } else {
        val ast = mnode.extras.adn
          .flatMap(_.shownTypeIdOpt)
          .flatMap(AdnShownTypes.maybeWithName)
          .getOrElse(AdnShownTypes.default)
        val testsFut = ast.ngls.map { ngl =>
          // Для проверки валидности пропихиваем этот узел в ShowcaseNodeListUtil и анализируем результат.
          scNlUtil.collectLayers(geoMode = None, currNode = mnode, currNodeLayer = ngl)
            .filter { _.nonEmpty }
            .map { _ => Option.empty[NodeProblem] }
            .recover { case ex: Throwable =>
              Some( NodeProblem(mnode, ex) )
            }
        }
        Future.foldLeft(testsFut)(acc0Fut) { (_acc0Fut, testResOpt) =>
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

    testAll().onComplete {
      case Success(nil) if nil.isEmpty =>
        debug("Test finished without problems.")

      // Или есть проблемы, или возникла ошибка при тесте. В обоих случаях надо уведомить.
      case tryRes =>
        warn("Selt-test problems detected: " + tryRes)

        mailer.instance
          .setRecipients(mSuperUsers.SU_EMAILS : _*)
          .setSubject("Suggest.io: Обнаружены проблемы геосвязности узлов")
          .setHtml {
            htmlCompressUtil.html4email {
              suProblemsEmailTpl(tryRes, ctxUtil.LK_URL_PREFIX)
            }
          }
          .send()
    }
  }

}
