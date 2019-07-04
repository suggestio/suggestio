package models.usr

import javax.inject.{Inject, Singleton}
import io.suggest.es.model.EsModel
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.sec.util.ScryptUtil
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.model.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 12:38
 * Description: Модель суперюзеров.
 */
@Singleton
class MSuperUsers @Inject()(
                             injector        : Injector,
                           )
  extends MacroLogsImpl
{

  /** Список емейлов админов suggest.io.
    * Раньше жил в конфигах, что вызывало больше неудобств, чем пользы. */
  val SU_EMAILS: Seq[String] = {
    Seq(
      "konstantin.nikiforov@cbca.ru",
      "sasha@cbca.ru",
      "alexander.pestrikov@cbca.ru"
    )
  }

  /** PersonId суперпользователей sio. */
  private var SU_IDS: Set[String] = Set.empty

  // Constructor
  onAppStart()

  /** Логика реакции на запуск приложения: нужно создать суперюзеров в БД. */
  def onAppStart(): Future[_] = {
    // Если в конфиге явно не включена поддержка проверки суперюзеров в БД, то не делать этого.
    // Это также нужно было при миграции с MPerson на MNode, чтобы не произошло повторного создания новых
    // юзеров в MNode, при наличии уже существующих в MPerson.
    val ck = "start.ensure.superusers"
    val createIfMissing = injector.instanceOf[Configuration].getOptional[Boolean](ck).getOrElseFalse
    val fut = resetSuperuserIds(createIfMissing)
    if (!createIfMissing)
      LOGGER.debug("Does not ensuring superusers in permanent models: " + ck + " != true")
    fut
  }


  /**
   * Принадлежит ли указанный id суперюзеру suggest.io?
   * @param personId Реальный id юзера.
   * @return true, если это админ. Иначе false.
   */
  def isSuperuserId(personId: String): Boolean = {
    // TODO Нужно, чтобы было Future[Boolean]. Иначе есть проблема, что при запуске суперюзеры отсутствуют на небольшой момент времени.
    SU_IDS.contains(personId)
  }

  /** Выставить в MNode id'шники суперпользователей.
    * Для этого надо убедится, что все админские MPersonIdent'ы
    * существуют. Затем, сохранить в глобальную переменную в MPerson этот списочек. */
  def resetSuperuserIds(createIfMissing: Boolean): Future[_] = {
    val mNodes = injector.instanceOf[MNodes]
    val scryptUtil = injector.instanceOf[ScryptUtil]
    val esModel = injector.instanceOf[EsModel]
    implicit val ec = injector.instanceOf[ExecutionContext]

    import esModel.api._

    val logPrefix = s"resetSuperuserIds(create=$createIfMissing): "
    val suEmails = SU_EMAILS
    LOGGER.debug(s"${logPrefix}Let's do it. There are ${suEmails.size} superuser emails: [${suEmails.mkString(", ")}]")

    val resFut = for {

      // Найти текущие узлы, ассоциированные с указанными мыльниками:
      userNodes <- mNodes.dynSearch {
        new MNodeSearchDfltImpl {
          override val nodeTypes = MNodeTypes.Person :: Nil
          override val outEdges: Seq[Criteria] = {
            val cr = Criteria(
              predicates        = MPredicates.Ident.Email :: Nil,
              nodeIds           = suEmails,
              nodeIdsMatchAll   = false,
              flag              = Some(true),
            )
            cr :: Nil
          }
        }
      }

      // Интересуют только отсутствующие юзеры. Уже существующие трогать не требуется.
      email2nodeMap = {
        val iter = for {
          userNode <- userNodes.iterator
          email    <- userNode.edges.withPredicateIterIds( MPredicates.Ident.Email )
        } yield {
          email -> userNode
        }
        iter.toMap
      }

      createdPersonIdOpts <- Future.traverse {
        suEmails.filterNot( email2nodeMap.contains )
      } { email =>
        if (createIfMissing) {
          LOGGER.trace(s"$logPrefix Installing new superuser for $email ...")
          val mperson0 = MNode(
            common = MNodeCommon(
              ntype = MNodeTypes.Person,
              isDependent = false
            ),
            meta = MMeta(
              basic = MBasicMeta(
                nameOpt = Some(email),
                langs   = "ru" :: Nil,
              )
            ),
            edges = MNodeEdges(
              out = {
                val emailIdentEdge = MEdge(
                  predicate = MPredicates.Ident.Email,
                  nodeIds   = Set(email),
                  info = MEdgeInfo(
                    flag = Some(true)
                  )
                )
                val pwIdentEdge = MEdge(
                  predicate = MPredicates.Ident.Password,
                  info = MEdgeInfo(
                    textNi = Some( scryptUtil.mkHash(email + System.currentTimeMillis().millis.toHours.toLong) ),
                    // На будущее - флаг необходимости смены пароля, чтобы напомнить юзеру.
                    flag = Some(false)
                  )
                )
                emailIdentEdge :: pwIdentEdge :: Nil
              }
            )
          )

          for {
            personId <- mNodes.save(mperson0)
          } yield {
            LOGGER.info(s"$logPrefix New superuser installed as $personId for $email")
            Some( personId )
          }

        } else {
          LOGGER.warn(s"logPrefix Skipping installing missing superuser [$email], because !createIfMissing.")
          Future.successful(None)
        }
      }

    } yield {
      val createdPersonIds = createdPersonIdOpts.iterator.flatten.toSet
      val existingPersonIds = email2nodeMap.valuesIterator.flatMap(_.id).toSet
      LOGGER.trace(s"$logPrefix successfully installed ${createdPersonIds.size} superUsers: ${createdPersonIds.mkString(", ")}\n previosly existed personIds = ${existingPersonIds.mkString(", ")}")
      SU_IDS = createdPersonIds ++ existingPersonIds
    }

    for (ex <- resFut.failed)
      LOGGER.error(s"$logPrefix Failure", ex)

    resFut
  }

}
