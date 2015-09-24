package models.usr

import org.elasticsearch.client.Client
import util.PlayMacroLogsImpl

import scala.concurrent.Future
import scala.util.{Failure, Success}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 12:38
 * Description: Модель суперюзеров.
 */
object SuperUsers extends PlayMacroLogsImpl {

  import LOGGER._

  /** PersonId суперпользователей sio. */
  private var SU_IDS: Set[String] = Set.empty

  /**
   * Принадлежит ли указанный id суперюзеру suggest.io?
   * @param personId Реальный id юзера.
   * @return true, если это админ. Иначе false.
   */
  def isSuperuserId(personId: String): Boolean = {
    SU_IDS.contains(personId)
  }

  /** Выставить в MPerson id'шники суперпользователей. Для этого надо убедится, что все админские MPersonIdent'ы
    * существуют. Затем, сохранить в глобальную переменную в MPerson этот списочек. */
  def resetSuperuserIds(implicit client: Client): Future[_] = {
    val logPrefix = "resetSuperuserIds(): "
    val se = MPersonIdent.SU_EMAILS
    trace(s"${logPrefix}There are ${se.size} superuser emails: [${se.mkString(", ")}]")
    Future.traverse(se) { email =>
      EmailPwIdent.getById(email) flatMap {
        // Суперюзер ещё не сделан. Создать MPerson и MPI для текущего email.
        case None =>
          val logPrefix1 = s"$logPrefix[$email] "
          info(logPrefix1 + "Installing new sio superuser...")
          MPerson(lang = "ru").save.flatMap { personId =>
            val pwHash = MPersonIdent.mkHash(email)
            EmailPwIdent(email=email, personId=personId, pwHash = pwHash).save.map { mpiId =>
              info(logPrefix1 + s"New superuser installed as $personId. mpi=$mpiId")
              personId
            }
          }

        // Суперюзер уже существует. Просто возвращаем его id.
        case Some(mpi) => Future successful mpi.personId
      }
    } andThen {
      case Success(suPersonIds) =>
        SU_IDS = suPersonIds.toSet
        trace(logPrefix + suPersonIds.length + " superusers installed successfully")

      case Failure(ex) =>
        error(logPrefix + "Failed to install superusers", ex)
    }
  }

}
