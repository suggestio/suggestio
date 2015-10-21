package models.usr

import models.MNode
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

  /** Выставить в MNode id'шники суперпользователей.
    * Для этого надо убедится, что все админские MPersonIdent'ы
    * существуют. Затем, сохранить в глобальную переменную в MPerson этот списочек. */
  def resetSuperuserIds(createIfMissing: Boolean)(implicit client: Client): Future[_] = {
    val logPrefix = s"resetSuperuserIds(create=$createIfMissing): "
    val se = MPersonIdent.SU_EMAILS
    debug(s"${logPrefix}Let's do it. There are ${se.size} superuser emails: [${se.mkString(", ")}]")
    Future.traverse(se) { email =>
      EmailPwIdent.getById(email) flatMap {
        // Суперюзер ещё не сделан, _id неизвестен. Создать person MNode и MPersonIden для текущего email.
        case None =>
          val logPrefix1 = s"$logPrefix[$email] "
          if (createIfMissing) {
            info(logPrefix1 + "Installing new sio superuser...")
            MNode.applyPerson(lang = "ru").save.flatMap { personId =>
              val pwHash = MPersonIdent.mkHash(email)
              EmailPwIdent(email=email, personId=personId, pwHash = pwHash).save.map { mpiId =>
                info(s"$logPrefix1 New superuser installed as $personId. mpi=$mpiId")
                Some(personId)
              }
            }
          } else {
            warn(logPrefix1 + "Skipping installing missing superuser, because !createIfMissing.")
            Future successful None
          }

        // Суперюзер уже существует. Просто возвращаем его id.
        case Some(mpi) =>
          Future successful Some(mpi.personId)
      }
    } andThen {
      case Success(suPersonIdOpts) =>
        val suPersonIds = suPersonIdOpts.flatten
        SU_IDS = suPersonIds.toSet
        trace(logPrefix + suPersonIds.length + " superusers installed successfully")

      case Failure(ex) =>
        error(logPrefix + "Failed to install superusers", ex)
    }
  }

}
