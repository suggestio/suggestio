package models.usr

import io.suggest.common.empty.OptionUtil

import javax.inject.{Inject, Singleton}
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.sec.util.ScryptUtil
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.text.StringUtil
import play.api.{Configuration, Environment, Mode}
import play.api.inject.Injector

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 12:38
 * Description: SuperUsers model.
 */
@Singleton
final class MSuperUsers @Inject()(
                                   injector        : Injector,
                                 )
  extends MacroLogsImpl
{
  private def configuration = injector.instanceOf[Configuration]

  /** List of superuser emails from application.conf */
  def SU_EMAILS: Set[String] = {
    configuration
      .getOptional[Seq[String]]( "superusers.emails" )
      .fold( Set.empty[String] )( _.toSet )
  }

  /** Person NodeIDs of superusers. */
  private var SU_IDS: Set[String] = Set.empty

  // Constructor
  onAppStart()

  /** onAppStart hook, called from constructor during play initialization. To initialize superuser nodes in DB. */
  private def onAppStart(): Future[_] = {
    val suEmails = SU_EMAILS

    val createIfMissing = configuration
      .getOptional[Boolean]( "superusers.autocreate" )
      .getOrElseFalse

    val fut = resetSuperuserIds( suEmails, createIfMissing )

    // If app.mode == dev, block current thread for some time, until resetSuperuserIds become completed.
    // TODO Need async analog for isSuperuserId(), to remove await here.
    if ( injector.instanceOf[Environment].mode == Mode.Dev ) {
      LOGGER.trace("Dev mode, awaiting superuser ids...")
      Try( Await.ready( fut, 5.seconds ) )
    }

    if (suEmails.isEmpty || !createIfMissing)
      LOGGER.debug(s"Does NOT ensuring superusers in permanent models: empty SU emails list or !autocreate")

    fut
  }


  /**
   * Is person nodeId - contained in superusers set?
   * @param personId Person node id.
   * @return true, if user is superuser. False overwise.
   */
  // TODO Need to become async somehow.
  def isSuperuserId(personId: String): Boolean = {
    SU_IDS contains personId
  }

  /** Reset SU_IDS, possibly initialize neede person nodes. */
  def resetSuperuserIds(suEmails: Set[String], createIfMissing: Boolean): Future[_] = {
    val mNodes = injector.instanceOf[MNodes]
    val scryptUtil = injector.instanceOf[ScryptUtil]
    val esModel = injector.instanceOf[EsModel]
    implicit val ec = injector.instanceOf[ExecutionContext]

    import esModel.api._

    val logPrefix = s"resetSuperuserIds(#${suEmails.size}, $createIfMissing): "
    LOGGER.debug(s"${logPrefix}Let's do it. There are ${suEmails.size} superuser emails: [${suEmails.mkString(", ")}]")

    val resFut = for {

      // Find existing person nodes, related to superuser emails.
      userNodes <- mNodes.dynSearch {
        new MNodeSearch {
          override val nodeTypes = MNodeTypes.Person :: Nil
          override val outEdges: MEsNestedSearch[Criteria] = {
            val cr = Criteria(
              predicates        = MPredicates.Ident.Email :: Nil,
              nodeIds           = suEmails.toSeq,
              nodeIdsMatchAll   = false,
              flag              = OptionUtil.SomeBool.someTrue,
            )
            MEsNestedSearch(
              clauses = cr :: Nil,
            )
          }
        }
      }

      // Collect missing person nodes:
      email2nodeMap = (for {
        userNode <- userNodes.iterator
        email    <- userNode.edges.withPredicateIterIds( MPredicates.Ident.Email )
      } yield {
        email -> userNode
      })
        .toMap

      createdPersonIdOpts <- Future.traverse {
        suEmails.filterNot( email2nodeMap.contains )
      } { email =>
        if (createIfMissing) {
          // Initialize superuser with email and random password.
          LOGGER.debug(s"$logPrefix Installing new superuser for $email ...")
          val password = StringUtil.randomId(20)

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
                  nodeIds   = Set.empty + email,
                  info = MEdgeInfo(
                    flag = Some(true)
                  )
                )
                val pwIdentEdge = MEdge(
                  predicate = MPredicates.Ident.Password,
                  info = MEdgeInfo(
                    textNi = Some( scryptUtil.mkHash( password ) ),
                    // For future: flag to ask user to reset password after login.
                    flag = Some(false)
                  ),
                )
                emailIdentEdge :: pwIdentEdge :: Nil
              }
            )
          )

          for {
            meta <- mNodes.save( mperson0 )
          } yield {
            LOGGER.info(s"$logPrefix New superuser created as node#${meta.id.orNull}\n *** login: $email\n *** password: $password")
            meta.id
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
