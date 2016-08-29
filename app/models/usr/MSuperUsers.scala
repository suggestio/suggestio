package models.usr

import com.google.inject.{Inject, Singleton}
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import models.MNode
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.secure.ScryptUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 12:38
 * Description: Модель суперюзеров.
 */
@Singleton
class MSuperUsers @Inject()(
  emailPwIdents   : EmailPwIdents,
  mNodes          : MNodes,
  scryptUtil      : ScryptUtil,
  mCommonDi       : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Список емейлов админов suggest.io.
    * Раньше жил в конфигах, что вызывало больше неудобств, чем пользы. */
  def SU_EMAILS: Seq[String] = {
    Seq(
      "konstantin.nikiforov@cbca.ru",
      //"ilya@shuma.ru",
      "sasha@cbca.ru",
      "alexander.pestrikov@cbca.ru"
    )
  }

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
  def resetSuperuserIds(createIfMissing: Boolean): Future[_] = {
    val logPrefix = s"resetSuperuserIds(create=$createIfMissing): "
    val se = SU_EMAILS
    debug(s"${logPrefix}Let's do it. There are ${se.size} superuser emails: [${se.mkString(", ")}]")
    Future.traverse(se) { email =>
      emailPwIdents.getById(email) flatMap {
        // Суперюзер ещё не сделан, _id неизвестен. Создать person MNode и MPersonIden для текущего email.
        case None =>
          val logPrefix1 = s"$logPrefix[$email] "
          if (createIfMissing) {
            info(logPrefix1 + "Installing new sio superuser...")
            val mperson0 = MNode(
              common = MNodeCommon(
                ntype = MNodeTypes.Person,
                isDependent = false
              ),
              meta = MMeta(
                basic = MBasicMeta(
                  nameOpt = Some(email),
                  langs   = List("ru")
                )
              )
            )

            for {
              personId <- mNodes.save(mperson0)
              mpiId <- {
                val epw = EmailPwIdent(
                  email       = email,
                  personId    = personId,
                  pwHash      = scryptUtil.mkHash(email),
                  isVerified  = true
                )
                emailPwIdents.save(epw)
              }
            } yield {
              info(s"$logPrefix1 New superuser installed as $personId. mpi=$mpiId")
              Some(personId)
            }

          } else {
            warn(logPrefix1 + "Skipping installing missing superuser, because !createIfMissing.")
            Future.successful(None)
          }

        // Суперюзер уже существует. Просто возвращаем его id.
        case Some(mpi) =>
          Future.successful(Some(mpi.personId))
      }
    }.andThen {
      case Success(suPersonIdOpts) =>
        val suPersonIds = suPersonIdOpts.flatten
        SU_IDS = suPersonIds.toSet
        info(s"$logPrefix SU person ids := ${suPersonIds.mkString(", ")}")

      case Failure(ex) =>
        error(logPrefix + "Failed to install superusers", ex)
    }
  }

}
