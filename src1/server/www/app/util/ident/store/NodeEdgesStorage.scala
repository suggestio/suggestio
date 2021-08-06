package util.ident.store

import io.suggest.auth.UserProfile
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.{EsModel, IMust, MEsNestedSearch}
import io.suggest.ext.svc.MExtService
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicate, MPredicates}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.sec.util.ScryptUtil
import io.suggest.text.Validators
import io.suggest.util.logs.MacroLogsImpl
import play.api.inject.Injector
import japgolly.univeq._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/** Credentials storage  */
final class NodeEdgesStorage @Inject() (
                                         injector: Injector,
                                       )
  extends ICredentialsStorage
  with MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val scryptUtil = injector.instanceOf[ScryptUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import esModel.api._

  private def _emailEdge(flag: Boolean, emails: String*): MEdge = {
    MEdge(
      predicate = MPredicates.Ident.Email,
      nodeIds = emails.toSet,
      info = MEdgeInfo(
        flag = OptionUtil.SomeBool( flag ),
      ),
    )
  }

  private def _passwordEdge(password: String): MEdge = {
    MEdge(
      predicate = MPredicates.Ident.Password,
      info = MEdgeInfo(
        textNi = Some( scryptUtil.mkHash( password ) ),
      )
    )
  }

  private def _phoneEdge(phoneNumber: String): MEdge = {
    MEdge(
      predicate = MPredicates.Ident.Phone,
      nodeIds   = Set.empty + phoneNumber,
      info = MEdgeInfo(
        flag = OptionUtil.SomeBool.someTrue,
      )
    )
  }


  override def updateEmailPw(personNode: MNode, regContext: MRegContext, emailFlag: Boolean): Future[MNode] = {
    lazy val logPrefix = s"updateCredentials(${regContext.email.getOrElse("")}, ${regContext.phoneNumber}, node#${personNode.id.orNull}):"

    // Уже существует узел. Организовать сброс пароля.
    val nodeId = personNode.id.get
    LOGGER.trace(s"$logPrefix phone#[${regContext.phoneNumber.orNull}] email#${regContext.email.orNull}")

    // TODO Update meta.basic.lang from regContext.lang?

    val mnode1 = MNode.edges
      .composeLens( MNodeEdges.out )
      .modify { edges0 =>
        // Убрать гарантировано ненужные эджи:
        val edges1 = edges0
          .iterator
          .filter { e0 =>
            e0.predicate match {
              // Эджи пароля удаляем безусловно
              case MPredicates.Ident.Password =>
                LOGGER.trace(s"$logPrefix Drop edge Password $e0 of node#$nodeId\n $e0")
                false
              // Эджи почты: неподтвердённые - удалить.
              case MPredicates.Ident.Email if !e0.info.flag.contains(true) =>
                LOGGER.trace(s"$logPrefix Drop edge Email#${e0.nodeIds.mkString(",")} with flag=false on node#$nodeId\n $e0")
                false
              // Остальные эджи - пускай живут.
              case _ => true
            }
          }
          .to( LazyList )

        // Отработать email-эдж. Если он уже есть для текущей почты, то оставить как есть. Иначе - создать новый, неподтверждённый эдж.
        val edges2 = regContext.email.fold( edges1 ) { email =>
          edges1
            .find { e =>
              (e.predicate ==* MPredicates.Ident.Email) &&
              regContext.email.exists( e.nodeIds.contains )
            }
            .fold {
              // Add new email-edge
              val emailEdge = _emailEdge( emailFlag, email )
              LOGGER.trace(s"$logPrefix Inserting Email-edge#$email on node#$nodeId\n $emailEdge")
              emailEdge #:: edges1
            } { emailExistsEdge =>
              LOGGER.trace(s"$logPrefix Keep edge Email#$email as-is on node#$nodeId\n $emailExistsEdge")
              edges1
            }
        }

        MNodeEdges.edgesToMap1( _passwordEdge(regContext.password) #:: edges2 )
      }( personNode )

    mNodes.saveReturning( mnode1 )
  }


  override def signUp(regContext: MRegContext, nodeTechName: Option[String]): Future[MNode] = {
    lazy val logPrefix = s"signUp(${regContext.email.getOrElse("")}, ${regContext.phoneNumber}):"

    // Как и ожидалось, такого юзера нет в базе. Собираем нового юзера:
    LOGGER.debug(s"$logPrefix For phone[${regContext.phoneNumber}] no user exists. Creating new...")

    // Собрать узел для нового юзера.
    val mperson0 = MNode(
      common = MNodeCommon(
        ntype       = MNodeTypes.Person,
        isDependent = false,
        isEnabled   = true,
      ),
      meta = MMeta(
        basic = MBasicMeta(
          techName = nodeTechName,
          langs    = regContext.lang.toList,
        ),
      ),
      edges = MNodeEdges(
        out = {
          var edges0: List[MEdge] = (
            _passwordEdge( regContext.password ) ::
            Nil
          )

          for (phoneNumber <- regContext.phoneNumber)
            edges0 ::= _phoneEdge( phoneNumber )
          for (email <- regContext.email)
            edges0 ::= _emailEdge( false, email )

          MNodeEdges.edgesToMap1( edges0 )
        }
      ),
    )

    // Создать узел для юзера:
    for {
      docMetaSaved <- mNodes.save( mperson0 )
    } yield {
      LOGGER.info(s"$logPrefix Created new user#${docMetaSaved.id.orNull} for creds[${regContext.phoneNumber} / ${regContext.email.orNull}]")
      mNodes.withDocMeta( mperson0, docMetaSaved )
    }
  }


  def checkPasswordSync(personNode: MNode, password: String): Boolean = {
    personNode.edges
      .withPredicateIter( MPredicates.Ident.Password )
      .toList
      .exists { e =>
        e.info.textNi
          .exists( scryptUtil.checkHash(password, _) )
      }
  }


  override def checkPassword(personNode: MNode, password: String): Future[Boolean] = {
    val isPasswordCorrect = checkPasswordSync( personNode, password )
    Future.successful( isPasswordCorrect )
  }


  override def resetPassword(personNode: MNode, password: String): Future[MNode] = {
    lazy val logPrefix = s"passwordChange(#${personNode.id.orNull}, ${password.hashCode}):"

    val pwEdge = MEdge(
      predicate = MPredicates.Ident.Password,
      info = MEdgeInfo(
        textNi = Some( scryptUtil.mkHash( password ) ),
      )
    )
    LOGGER.trace(s"$logPrefix Will replace all current password-edges with:\n $pwEdge")

    val mnodeModF = MNode.edges.modify { edges0 =>
      MNodeEdges.out.set(
        MNodeEdges.edgesToMap1(
          edges0
            .withoutPredicateIter( MPredicates.Ident.Password )
            .++ {
              Iterator.single( pwEdge )
            }
        )
      )(edges0)
    }

    mNodes
      .tryUpdate(personNode)( mnodeModF )
  }


  override def findByEmailPhoneWithPw(emailOrPhone: String, password: String): Future[Seq[MNode]] = {
    for {
      nodesWithLogin <- mNodes.dynSearch {
        new MNodeSearch {
          // По идее, тут не более одного.
          override def limit = 2
          override val testNode = OptionUtil.SomeBool.someFalse
          override val isEnabled = OptionUtil.SomeBool.someTrue
          override val nodeTypes = MNodeTypes.Person :: Nil
          override val outEdges: MEsNestedSearch[Criteria] = {
            val must = IMust.MUST
            // Есть проверенный email:
            val emailCr = Criteria(
              predicates  =
                MPredicates.Ident.Email ::
                  MPredicates.Ident.Phone ::
                  Nil,
              nodeIds     = (
                Validators.normalizeEmail(emailOrPhone) ::
                  Validators.normalizePhoneNumber(emailOrPhone) ::
                  Nil
                )
                .filter(_.nonEmpty)
                .distinct,
              flag        = OptionUtil.SomeBool.someTrue,
              must        = must,
              nodeIdsMatchAll = false,
            )
            // И есть пароль
            val pwCr = Criteria(
              predicates  = MPredicates.Ident.Password :: Nil,
              must        = must,
            )
            MEsNestedSearch.plain( emailCr, pwCr )
          }
        }
      }
    } yield {
      // Filter by password:
      val r = nodesWithLogin.filter { mnode =>
        checkPasswordSync( mnode, password )
      }
      LOGGER.trace(s"findByEmailPhoneWithPw($emailOrPhone, ${password.hashCode}):\n For '$emailOrPhone' found ${nodesWithLogin.length} nodes: [${nodesWithLogin.iterator.flatMap(_.id).mkString(" ")}]\n With valid password filtered ${r.length} nodes: [${r.iterator.flatMap(_.id).mkString(" ")}]")
      r
    }
  }


  override def findByExtServiceUserId(extService: MExtService, remoteUserId: String): Future[Seq[MNode]] = {
    val msearch = new MNodeSearch {
      override val nodeTypes = MNodeTypes.Person :: Nil
      override def limit = 2
      override val outEdges: MEsNestedSearch[Criteria] = {
        val cr = Criteria(
          predicates  = MPredicates.Ident.Id :: Nil,
          nodeIds     = remoteUserId :: Nil,
          extService  = Some(extService :: Nil),
        )
        MEsNestedSearch.plain( cr )
      }
    }
    mNodes.dynSearch( msearch )
  }


  override def signUpExtService(extService: MExtService, profile: UserProfile, lang: Option[String] = None): Future[MNode] = {
    val mperson0 = MNode(
      common = MNodeCommon(
        ntype       = MNodeTypes.Person,
        isDependent = false
      ),
      meta = MMeta(
        basic = MBasicMeta(
          nameOpt   = profile.fullName,
          techName  = Some(profile.providerId + ":" + profile.userId),
          langs     = lang.toList,
        ),
        person  = MPersonMeta(
          nameFirst   = profile.firstName,
          nameLast    = profile.lastName,
          extAvaUrls  = profile.avatarUrl.toList,
        )
        // Ссылку на страничку юзера в соц.сети можно генерить на ходу через ident'ы и костыли самописные.
      ),
      edges = MNodeEdges(
        out = {
          val extIdentEdge = MEdge(
            predicate = MPredicates.Ident.Id,
            nodeIds   = Set.empty + profile.userId,
            info      = MEdgeInfo(
              extService = Some( extService )
            )
          )
          var identEdgesAcc: List[MEdge] = extIdentEdge :: Nil

          def _maybeAddTrustedIdents(pred: MPredicate, keys: Iterable[String]) = {
            if (keys.nonEmpty) {
              identEdgesAcc ::= MEdge(
                predicate = pred,
                nodeIds   = keys.toSet,
                info = MEdgeInfo(
                  flag = Some(true)
                )
              )
            }
          }

          _maybeAddTrustedIdents( MPredicates.Ident.Email, profile.emails )
          _maybeAddTrustedIdents( MPredicates.Ident.Phone, profile.phones )

          MNodeEdges.edgesToMap1( identEdgesAcc )
        }
      )
    )

    LOGGER.debug(s"signUpExtService(${profile.userId}@$extService) Registering new user via service#${extService}:\n $profile ...")
    mNodes.saveReturning(mperson0)
  }


  override def hasAnyExtServiceCreds(personId: String): Future[Boolean] = {
    mNodes.dynExists {
      new MNodeSearch {
        override val withIds = personId :: Nil
        override val nodeTypes = MNodeTypes.Person :: Nil
        override val outEdges: MEsNestedSearch[Criteria] = {
          val cr = Criteria(
            predicates = MPredicates.Ident.Id :: Nil,
            extService = Some(Nil)
          )
          MEsNestedSearch.plain( cr )
        }
        override def limit = 1
      }
    }
  }


  override def findByEmail(emails: String*): Future[Seq[MNode]] = {
    require( emails.nonEmpty, "Email addresses must be non empty" )

    mNodes.dynSearch {
      new MNodeSearch {
        override val nodeTypes = MNodeTypes.Person :: Nil
        override val outEdges: MEsNestedSearch[Criteria] = {
          val cr = Criteria(
            predicates        = MPredicates.Ident.Email :: Nil,
            nodeIds           = emails,
            nodeIdsMatchAll   = false,
            flag              = OptionUtil.SomeBool.someTrue,
          )
          MEsNestedSearch.plain( cr )
        }
      }
    }
  }

}

