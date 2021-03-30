package controllers

import java.time.OffsetDateTime

import javax.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.adn.{MAdnRight, MAdnRights}
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill.tf.daily.{ITfDailyMode, MTfDailyInfo}
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.{EsModel, MEsUuId}
import io.suggest.init.routed.MJsInitTargets
import io.suggest.lk.nodes._
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.n2.edge.{MEdge, MEdgeFlagData, MEdgeFlags, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.mlk.nodes.{MLkAdNodesTplArgs, MLkNodesTplArgs}
import models.req.{IReq, IReqHdr, MAdOptProdReq, MAdProdReq, MReq}
import util.acl._
import util.adn.NodesUtil
import util.billing.{Bill2Util, TfDailyUtil}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.ctx.CtxData
import io.suggest.n2.bill.MNodeBilling
import io.suggest.n2.bill.tariff.MNodeTariffs
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.req.ReqUtil
import util.lk.nodes.LkNodesUtil
import views.html.lk.nodes._
import io.suggest.scalaz.ScalazUtil.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import japgolly.univeq._
import scalaz.{EphemeralStream, Tree}
import util.TplDataFormatUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.02.17 16:06
  * Description: Контроллер для системы управления деревом узлов.
  * Появился в контексте необходимости системы управления собственными маячками.
  * Маячки -- очень частный случай под-узла, поэтому тут скорее управление ресиверами.
  *
  * Ещё есть необходимость размещаться в маячках. Форма может работать как в контексте карточки,
  * так и в контексте узла.
  *
  * Контроллер также должен препятствовать нежелательной деятельности пользователя:
  * - массового создания маячков с целью занять чужие id'шники.
  */
final class LkNodes @Inject() (
                                sioControllerApi          : SioControllerApi,
                              )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import mCommonDi.current.injector

  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val bill2Util = injector.instanceOf[Bill2Util]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val lkNodesUtil = injector.instanceOf[LkNodesUtil]
  private lazy val canFreelyAdvAdOnNode = injector.instanceOf[CanFreelyAdvAdOnNode]
  private lazy val canAdvAd = injector.instanceOf[CanAdvAd]
  private lazy val tfDailyUtil = injector.instanceOf[TfDailyUtil]
  private lazy val nodesUtil = injector.instanceOf[NodesUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val canChangeNodeAvailability = injector.instanceOf[CanChangeNodeAvailability]
  private lazy val bruteForceProtect = injector.instanceOf[BruteForceProtect]
  private lazy val canCreateSubNode = injector.instanceOf[CanCreateSubNode]
  private lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]

  private lazy val nodesTpl = injector.instanceOf[NodesTpl]
  private lazy val adNodesTpl = injector.instanceOf[AdNodesTpl]

  import mCommonDi._
  import esModel.api._


  /** Сборка options-карты по поводу размещения карточки в узле.
    *
    * @param nodeId id узла-ресивера.
    * @param madOpt Рекламная карточка, если есть.
    * @return options Map.
    */
  private def _hasAdv2(nodeId: String, madOpt: Option[MNode]): Map[MLknOpKey, MLknOpValue] = {
    madOpt
      .map { mad =>
        val rcvrEdges = mad.edges
          .withNodePred( nodeId, MPredicates.Receiver )
          .to( LazyList )
        val edgeOpt = rcvrEdges.headOption

        Map[MLknOpKey, MLknOpValue](
          // Включено размещение в узле?
          MLknOpKeys.AdvEnabled -> MLknOpValue(
            bool = OptionUtil.SomeBool( rcvrEdges.nonEmpty ),
          ),
          // Постоянная раскрытость карточки?
          MLknOpKeys.ShowOpened -> MLknOpValue(
            bool = OptionUtil.SomeBool(
              edgeOpt
                .fold(false)( _.info.flagsMap contains MEdgeFlags.AlwaysOpened )
            ),
          ),
          // Постоянная обводка карточки:
          MLknOpKeys.AlwaysOutlined -> MLknOpValue(
            bool = OptionUtil.SomeBool(
              edgeOpt.exists(
                _.info.flagsMap contains MEdgeFlags.AlwaysOutlined
              )
            )
          ),
        )
      }
      .getOrElse( Map.empty )
  }


  // Общий код сборки инфы по одному узлу.
  private def _mkLknNode(mnode: MNode, madOpt: Option[MNode] = None, isDetailed: Option[Boolean] = None,
                         isAdmin: Option[Boolean] = None, tf: Option[MTfDailyInfo] = None): MLknNode = {
    val nodeId = mnode.id.get
    val ntyp = mnode.common.ntype
    MLknNode(
      id                    = nodeId,
      name                  = mnode.guessDisplayNameOrId,
      ntype                 = Some( ntyp ),
      isAdmin               = isAdmin,
      options               = {
        val map1 = madOpt.fold {
           Map.empty[MLknOpKey, MLknOpValue]
        } { _ =>
          _hasAdv2( nodeId, madOpt )
        }
        // Вернуть node enabled для mnode:
        map1 + {
          val v = MLknOpValue(bool = OptionUtil.SomeBool( mnode.common.isEnabled ))
          val k = MLknOpKeys.NodeEnabled
          (k -> v)
        }
      },
      tf                    = tf,
      isDetailed            = isDetailed,
    )
  }

  private def _subNodesFor(mnode: MNode, madOpt: Option[MNode])
                          (implicit ctx: Context): Future[Tree[MLknNode]] = {
    import ctx.request

    val nodeId = mnode.id.get

    // Запустить поиск узлов.
    val subNodesFut = mNodes.dynSearch {
      lkNodesUtil.subNodesSearch(nodeId)
    }

    // Можно ли менять доступность узла?
    val isAdminFut = canChangeNodeAvailability.adminCanChangeAvailabilityOf(mnode)

    val tfDailyInfoOptFut = madOpt.fold [Future[Option[MTfDailyInfo]]] {
      for {
        tfDaily0 <- tfDailyUtil.getTfInfo(mnode)(ctx)
      } yield {
        val tfDaily2 = MTfDailyInfo.clauses.modify { clauses0 =>
          clauses0
            .view
            .mapValues {
              TplDataFormatUtil.setFormatPrice(_)(ctx)
            }
            .toMap
        }(tfDaily0)
        Some( tfDaily2 )
      }
    } { _ =>
      Future.successful(None)
    }

    // Рендер найденных узлов в данные для модели формы.
    for {
      subNodes                <- subNodesFut
      isAdmin                 <- isAdminFut
      tfDailyInfoOpt          <- tfDailyInfoOptFut
    } yield {
      Tree.Node[MLknNode](
        root = _mkLknNode(
          mnode,
          madOpt      = madOpt,
          isDetailed  = OptionUtil.SomeBool.someTrue,
          isAdmin     = OptionUtil.SomeBool(isAdmin),
          tf          = tfDailyInfoOpt
        ),
        forest = {
          val someFalse = OptionUtil.SomeBool.someFalse
          subNodes
            .toEphemeralStream
            .map { chNode =>
              // canChangeAvailability: На уровне под-узлов это значение не важно, т.к. для редактирования надо зайти в под-узел и там будет уже нормальный ответ на вопрос.
              val lkn = _mkLknNode(
                chNode,
                madOpt = madOpt,
                isDetailed = someFalse,
              )
              Tree.Leaf( lkn )
            }
        },
      )
    }
  }


  /** Поиск прямо-подчинённых для текущего юзера.
    *
    * @param withoutIds Кроме указанных id.
    * @param madOpt В контексте указанной рекламной карточки.
    * @param request Текущий реквест.
    * @return Фьючерс с поддеревом узлов текущего пользователя.
    */
  private def _personNodes(withoutIds: Seq[String] = Nil, madOpt: Option[MNode] = None)
                          (implicit request: IReqHdr): Future[Tree[MLknNode]] = {
    // Поискать под-узлы текущего пользователя:
    val personSubNodesFut = request.user
      .personIdOpt
      .fold [Future[List[Tree[MLknNode]]]] (Future.successful(Nil)) { personId =>
        val msearch = nodesUtil.personNodesSearch(
          personId    = personId,
          withoutIds1 = withoutIds,
        )
        for {
          mnodes <- mNodes.dynSearch( msearch )
        } yield {
          val someTrue = OptionUtil.SomeBool.someTrue
          val someFalse = OptionUtil.SomeBool.someFalse

          (for {
            mnode <- mnodes.iterator
          } yield {
            val mLknNode = _mkLknNode(
              mnode,
              madOpt      = madOpt,
              isDetailed  = someFalse,
              isAdmin     = someTrue,
              // tf: для других узлов не особо важен, т.к. будет запрошен при открытии.
            )
            Tree.Leaf( mLknNode )
          })
            .toList
        }
      }

    // Отрендерить инфу по узлу текущего пользователя:
    val personNodeFut = for {
      personNodeOpt <- request.user.personNodeOptFut
    } yield {
      val personNode = personNodeOpt getOrElse {
        LOGGER.warn(s"_lknFormPrep(): Person node#${request.user.personIdOpt.orNull} missing/invalid. Make ephemeral...")
        MNode(
          id = request.user.personIdOpt,
          common = MNodeCommon(
            ntype = MNodeTypes.Person,
            isDependent = false,
          ),
        )
      }
      _mkLknNode(
        personNode,
        madOpt = madOpt,
        isDetailed = OptionUtil.SomeBool.someTrue,
      )
    }

    for {
      personSubNodes <- personSubNodesFut
      personNode     <- personNodeFut
    } yield {
      Tree.Node(
        root   = personNode,
        forest = EphemeralStream.fromList[Tree[MLknNode]]( personSubNodes ),
      )
    }
  }


  /** Внутренняя модель данных по форме и контексту.
    *
    * @param formState Строка с данными формы для инициализации на клиенте.
    * @param ctx Обычный контекст.
    */
  private case class _MLknFormPrep(
                                    formState         : String,
                                    implicit val ctx  : Context
                                  )

  /** Дедубликация кода экшена HTML-рендера формы на узле и для карточки.
    * Задача: Собрать модель данных инициализации формы с начальным состоянием формы. Сериализовать для рендера в html-шаблоне.
    *
    * @param onNode Текущий узел ADN или продьюсер карточки.
    * @param madOpt Рекламная карточки, если известна.
    * @param request Исходный HTTP-реквест.
    * @return Подготовленные данные по форме и контексту.
    */
  private def _lknFormPrep(onNode: MNode, madOpt: Option[MNode] = None)
                          (implicit request: IReq[_]): Future[_MLknFormPrep] = {
    // Поискать пользовательские узлы верхнего уровня, кроме текущего. TODO А что будет, если текущий узел не является прямо-подчинённым текущему юзеру? Каша из узлов?
    val personTreeFut = _personNodes(
      withoutIds    = onNode.id.toList,
      madOpt        = madOpt,
    )

    val ctx0 = implicitly[Context]

    // Собрать и сериализовать начальное состояние системы.
    val lknNodeRespFut = for {
      // Запустить поиск под-узлов для текущего узла.
      subNodesResp      <- _subNodesFor(onNode, madOpt = madOpt)(ctx0)
      personTree        <- personTreeFut
    } yield {
      MLknNodeResp(
        subTree = Tree.Node(
          root   = personTree.rootLabel,
          forest = subNodesResp ##:: personTree.subForest,
        )
      )
    }

    // Пока подготовить контекст рендера шаблона
    val lkCtxFut = for {
      lkCtxData <- request.user.lkCtxDataFut
    } yield {
      implicit val lkCtxData2 = CtxData.jsInitTargetsAppendOne( MJsInitTargets.LkNodesForm )(lkCtxData)
      ctx0.withData( lkCtxData2 )
    }

    // Отрендерить и вернуть HTML-шаблон со страницей для формы.
    for {
      lknNodeResp     <- lknNodeRespFut
      lkCtx           <- lkCtxFut
    } yield {
      val minit = MLknFormInit(
        conf  = MLknConf(
          onNodeId = onNode.id,
          adIdOpt  = madOpt.flatMap(_.id)
        ),
        // Собрать дефолтовый ответ сервера для текущего узла, чтобы не было лишнего запроса к серверу после инициализации.
        resp0 = lknNodeResp,
      )
      val formState = Json
        .toJson( minit )
        .toString()
      _MLknFormPrep( formState, lkCtx )
    }
  }


  /**
    * Рендер страницы с формой управления подузлами текущего узла.
    * Сама форма реализована через react, тут у нас лишь страничка-обёртка.
    *
    * @param nodeIdU id текущей узла, т.е. узла с которым идёт взаимодействие.
    * @return 200 + HTML, если у юзера достаточно прав для управления узлом.
    */
  def nodesOf(nodeIdU: MEsUuId) = csrf.AddToken {
    val nodeId = nodeIdU: String
    isNodeAdmin(nodeId, U.Lk, U.PersonNode).async { implicit request =>
      for {
        res               <- _lknFormPrep(request.mnode)
      } yield {
        val args = MLkNodesTplArgs(
          formState = res.formState,
          mnode     = request.mnode
        )
        Ok( nodesTpl(args)(res.ctx) )
      }
    }
  }


  /** Конвертация фьючерса с поддеревом в JSON-ответ клиенту. */
  private def _treeToJsonResp( subTreeFut: Future[Tree[MLknNode]] ): Future[Result] = {
    for (subTree <- subTreeFut) yield {
      val resp = MLknNodeResp( subTree )
      val respJson = Json.toJson( resp )
      Ok( respJson )
    }
  }

  /** Универсальный экшен чтения кусков дерева узлов.
    * Экшен поддерживает чтение корня дерева или сегментов его ветвей.
    * Экшен поддерживает работу как в контексте карточки, так и без оной.
    *
    * @param onNodeRk RcvrKey узла в дереве.
    *                 None - корень дерева юзера.
    * @param adIdOpt id рекламной карточки, если форма работает в режиме размещения.
    * @return Экшен, возвращающий ответ с запрашиваемым поддеревом.
    */
  def subTree(onNodeRk: Option[RcvrKey], adIdOpt: Option[MEsUuId]): Action[AnyContent] = csrf.Check {
    // TODO onNode: надо ли доп.фильтрацию для пустого path? Или play сам справляется?
    val onNodeRkOpt = onNodeRk
      .map(_.filter(_.nonEmpty))
      .filter(_.nonEmpty)

    // В зависимости от параметров, надо выбирать разные ACL-проверки.
    adIdOpt.fold {
      // Нет карточки - управление узлами.
      onNodeRkOpt.fold {
        // Форма не знает ничего о дереве - вернуть начальное дерево.
        isAuth().async { implicit request =>
          _treeToJsonResp( _personNodes() )
        }

      } { onNodeRk =>
        // Чтение поддерева указанного узла. personNodes не нужны.
        isNodeAdmin( onNodeRk ).async { implicit request =>
          val subNodesFut = _subNodesFor( mnode = request.mnode, madOpt = None )
          _treeToJsonResp( subNodesFut )
        }
      }

    } { adId =>
      onNodeRkOpt.fold {
        // Инициализация формы размещения карточки: форма запрашивает начальное дерево.
        canAdvAd( adId.id, U.PersonNode ).async { implicit request =>
          val personNodes = _personNodes( madOpt = Some(request.mad) )
          _treeToJsonResp( personNodes )
        }

      } { onNodeRk =>
        // Запрос поддерева в рамках уже инициалированной формы размещения карточки в узлах.
        canFreelyAdvAdOnNode(adId, onNodeRk).async { implicit request =>
          val subNodesFut = _subNodesFor( request.mnode, madOpt = Some(request.mad) )
          _treeToJsonResp( subNodesFut )
        }
      }
    }
  }


  /** BodyParser для тела запроса по созданию/редактированию узла. */
  private def _mLknNodeReqBP = parse.json[MLknNodeReq]


  /** Создать новый узел (маячок) с указанными параметрами.
    * POST-запрос с данными добавля
    *
    * @param parentRk id родительского узла.
    * @return 200 OK + данные созданного узла.
    *         409 Conflict - id узла уже занят кем-то.
    */
  def createSubNodeSubmit(parentRk: RcvrKey) = csrf.Check {
    bruteForceProtect {
      canCreateSubNode(parentRk).async(_mLknNodeReqBP) { implicit request =>

        lazy val logPrefix = s"addSubNodeSubmit($parentRk)[${System.currentTimeMillis()}]:"

        lkNodesUtil.validateNodeReq( request.body, isEdit = false ).fold(
          {violations =>
            // Конвертим violations в нормальную коллекцию, чтобы просто её отформатировать без боли.
            val violsList = violations.asIterable
            LOGGER.debug(s"$logPrefix Failed to validate data:\n ${request.body}\n Violations: ${violsList.mkString(", ")}")
            errorHandler.onClientError(request, NOT_ACCEPTABLE, s"Invalidated: ${violsList.mkString("\n", ",\n", "")}")
          },

          // Данные по создаваемому узлу приняты и выверены. Произвести создание нового узла.
          {addNodeInfo =>
            LOGGER.trace(s"$logPrefix Validated: $addNodeInfo")

            // Нужно проверить, не занят ли указанный id каким-то другим узлом.
            val currIdNodeOptFut = FutureUtil.optFut2futOpt( addNodeInfo.id ) { newNodeId =>
              mNodes.getByIdCache(newNodeId)
            }
            val nodeWithIdNotExistsFut = currIdNodeOptFut.filter(_.isEmpty)

            val ntype = MNodeTypes.BleBeacon
            val isEnabled = true

            // Параллельно собираем инстанс нового узла. Крайне маловерятно, что это будет пустой работой.
            val newNode = MNode(
              common = MNodeCommon(
                ntype       = ntype,
                isDependent = false,
                isEnabled   = isEnabled
              ),

              meta = MMeta(
                basic = {
                  val nameOpt = Some( addNodeInfo.name )
                  MBasicMeta(
                    nameOpt       = nameOpt,
                    nameShortOpt  = nameOpt
                  )
                }
              ),

              edges = MNodeEdges(
                out = {
                  // Эдж до юзера-создателя узла.
                  val eCreator = MEdge(
                    predicate = MPredicates.CreatedBy,
                    nodeIds   = Set.empty[String] + request.user.personIdOpt.get,
                  )

                  val parentNodeIds = Set.empty[String] + parentRk.last
                  // Эдж до родительского узла.
                  val eParent = MEdge(
                    predicate = MPredicates.OwnedBy,
                    nodeIds   = parentNodeIds
                  )
                  val ePlacedIn = MEdge(
                    predicate = MPredicates.PlacedIn,
                    nodeIds   = parentNodeIds
                  )

                  // Вернуть все эджи:
                  eCreator :: eParent :: ePlacedIn :: Nil
                }
              ),

              // Это узел-ресивер, поэтому заполняем профиль ADN-узла:
              extras = MNodeExtras(
                adn = Some(MAdnExtra(
                  rights      = Set.empty[MAdnRight] + MAdnRights.RECEIVER,
                  isUser      = !request.user.isSuper,
                ))
              ),

              id = addNodeInfo.id
            )


            // Запустить сохранение нового узла, если id позволяет:
            val newNodeIdFut = nodeWithIdNotExistsFut.flatMap { _ =>
              LOGGER.trace(s"$logPrefix Node id ${addNodeInfo.id} looks free. Creating new node...")
              mNodes.save( newNode )
            }

            // В фоне собрать инфу по тарифу текущему.
            val tfDailyInfoFut = tfDailyUtil.getTfInfo( request.mnode )

            // Дождаться окончания сохранения нового узла.
            val respFut = for {
              newNodeId     <- newNodeIdFut
              mnode2 = (MNode.id set Some(newNodeId))(newNode)
              tfDailyInfo   <- tfDailyInfoFut
            } yield {
              val someTrue = OptionUtil.SomeBool.someTrue
              // Собираем ответ, сериализуем, возвращаем...
              val mResp = _mkLknNode(
                mnode2,
                isDetailed  = someTrue,
                isAdmin     = someTrue,
                tf          = Some(tfDailyInfo),
              )
              Ok( Json.toJson(mResp) )
            }

            respFut.recover {
              // Если NSEE, значит отработал filter by id, и узел с указанным id уже существует.
              case ex: NoSuchElementException =>
                val newNodeId = addNodeInfo.id.orNull
                LOGGER.warn(s"$logPrefix Node id $newNodeId looks busy, refusing to conflict.", ex)
                Conflict(s"Conflicted: Node $newNodeId already exists.")
            }
          }
        )
      }
    }
  }


  /** Обновление флага isEnabled у какого-то узла.
    *
    * @param nodeId id узла.
    * @param isEnabled Новое значение флага.
    * @return 200 OK + обновлённый узел.
    */
  private def setNodeEnabled(nodeId: String, isEnabled: Boolean) = csrf.Check {
    bruteForceProtect {
      canChangeNodeAvailability(nodeId).async { implicit request =>

        // Запустить обновление узла.
        val saveFut = mNodes.tryUpdate(request.mnode)(
          MNode.common
            .composeLens( MNodeCommon.isEnabled )
            .set( isEnabled ) andThen
          MNode.meta
            .composeLens( MMeta.basic )
            .composeLens( MBasicMeta.dateEdited )
            .set( Some(OffsetDateTime.now()) )
        )

        // В фоне собрать инфу по тарифу текущему.
        val tfDailyInfoFut = tfDailyUtil.getTfInfo( request.mnode )

        // Когда сохранение будет выполнено, то вернуть данные по обновлённому узлу.
        for {
          mnode2        <- saveFut
          tfDailyInfo   <- tfDailyInfoFut
        } yield {
          LOGGER.debug(s"setNodeEnabled($nodeId): enabled => $isEnabled")
          val someTrue = OptionUtil.SomeBool.someTrue
          val mLknNode = _mkLknNode(
            mnode2,
            isDetailed  = someTrue,
            isAdmin     = someTrue,
            tf          = Some( tfDailyInfo ),
          )
          Ok( Json.toJson(mLknNode) )
        }
      }
    }
  }


  /** Команда к удалению какого-то узла.
    *
    * @param nodeId id узла.
    * @return 200 OK - узел удалён.
    */
  def deleteNode(nodeId: String) = csrf.Check {
    // Защита от брутфорса, хз зачем. Может пригодиться...
    bruteForceProtect {
      canChangeNodeAvailability(nodeId).async { implicit request =>
        val isDeletedFut = mNodes.deleteById(nodeId)
        for {
          isDeleted <- isDeletedFut
        } yield {
          LOGGER.info(s"deleteNode($nodeId): Delete node by user#${request.user.personIdOpt.orNull} request: isDeleted?$isDeleted")
          Ok( isDeleted.toString )
        }
      }
    }
  }


  /** Самбит данных редактирования узла.
    * На вход подаётся данные по узлу (без id, т.к. он неизменяем).
    *
    * @param nodeId id узла.
    * @return 200 с обновлёнными данными по узлу | 404 | 406 | BFP.
    */
  def editNode(nodeId: String) = csrf.Check {
    bruteForceProtect {
      canChangeNodeAvailability(nodeId).async(_mLknNodeReqBP) { implicit request =>

        lazy val logPrefix = s"editNode($nodeId):"

        lkNodesUtil.validateNodeReq( request.body, isEdit = true ).fold(
          {violations =>
            LOGGER.debug(s"$logPrefix Failed to bind form: ${violations.iterator.mkString(", ")}")
            errorHandler.onClientError(request, NOT_ACCEPTABLE, "Invalid name.")
          },

          {binded =>
            LOGGER.trace(s"$logPrefix Binded: $binded")

            // Запустить апдейт узла.
            val updateFut = mNodes.tryUpdate( request.mnode ) {
              val nameOpt = Some( binded.name )
              MNode.meta
                .composeLens( MMeta.basic )
                .modify {
                  _.copy(
                    nameOpt       = nameOpt,
                    nameShortOpt  = nameOpt,
                    dateEdited    = Some( OffsetDateTime.now() )
                  )
                }
            }

            // В фоне собрать инфу по тарифу текущему.
            val tfDailyInfoFut = tfDailyUtil.getTfInfo( request.mnode )

            // Когда будет сохранено, отрендерить свежую инфу по узлу.
            for {
              mnode         <- updateFut
              tfDailyInfo   <- tfDailyInfoFut
            } yield {
              LOGGER.debug(s"$logPrefix Ok, nodeVsn => ${mnode.versionOpt.orNull}")
              val someTrue = OptionUtil.SomeBool.someTrue
              val m = _mkLknNode(
                mnode,
                isDetailed  = someTrue,
                isAdmin     = someTrue,
                tf          = Some(tfDailyInfo),
              )
              Ok( Json.toJson(m) )
            }
          }
        )

      }
    }
  }


  /** Открыть форму узлов для рекламной карточки.
    *
    * @param adIdU id рекламной карточки.
    * @return 200 + HTML с данными формы узлов в контексте карточки.
    */
  def nodesForAd(adIdU: MEsUuId) = csrf.AddToken {
    val adId = adIdU: String
    canAdvAd(adId, U.PersonNode).async { implicit request =>
      // Сборка дерева узлов. Интересует узел-продьюсер.
      for {
        res <- _lknFormPrep(request.producer, madOpt = Some(request.mad))
      } yield {
        val rArgs = MLkAdNodesTplArgs(
          formState = res.formState,
          mad       = request.mad,
          producer  = request.producer
        )
        val html = adNodesTpl(rArgs)(res.ctx)
        Ok(html)
      }
    }
  }


  /** Обновить статус карточки на узле.
    *
    * @param adId id рекламной карточки.
    * @param isEnabled Размещена или нет?
    * @param rcvrKey Ключ узла.
    * @return 200 OK + MLknNode.
    */
  private def setAdv(adId: String, isEnabled: Boolean, rcvrKey: RcvrKey) = csrf.Check {
    canFreelyAdvAdOnNode(adId, rcvrKey).async { implicit request =>
      // Выполнить обновление текущей карточки согласно значению флага isEnabled.
      val nodeId = request.mnode.id.get
      val nodeIds = Set.empty + nodeId
      val adId = request.mad.id.get

      // Поискать аналогичные бесплатные размещения в биллинге, заглушив их.
      val suppressRelatedItemsFut = slick.db.run {
        bill2Util.justFinalizeItemsLike(
          nodeId  = adId,
          iTypes  = MItemTypes.AdvDirect :: Nil,
          rcvrIds = nodeIds
        )
      }

      val selfPred = MPredicates.Receiver.Self
      // Запустить обновление узла на стороне ES.
      val updateNodeFut = mNodes.tryUpdate( request.mad ) {
        MNode.edges.modify { edges0 =>
          // Удалить эдж текущего размещения. Даже если isEnabled=true, всё равно надо отфильтровать старый эдж, чтобы перезаписать его.
          val edgesIter1 = edges0
            .withoutNodePred( nodeId, MPredicates.Receiver )
          val edgesIter2 = if (isEnabled) {
            // Найти/добавить эдж до указанного узла.
            val medge = MEdge(
              predicate = selfPred,
              nodeIds   = nodeIds
            )
            edgesIter1 ++ (medge :: Nil)
          } else {
            // isEnabled=false, удаление эджа размещения уже было выше.
            edgesIter1
          }
          val out2 = MNodeEdges.edgesToMap1( edgesIter2 )
          MNodeEdges.out.set( out2 )(edges0)
        }
      }

      for {
        itemsSuppressed   <- suppressRelatedItemsFut
        mad2              <- updateNodeFut
      } yield {
        LOGGER.trace(s"setAdv(ad#$adId, node#$nodeId): adv => $isEnabled, suppressed $itemsSuppressed in billing")
        // Собрать ответ.
        val someTrue = OptionUtil.SomeBool.someTrue
        val mLknNode = _mkLknNode(
          request.mnode,
          madOpt      = Some( mad2 ),
          isDetailed  = someTrue,
          isAdmin     = someTrue,
          // tf: На странице размещения это не важно
        )

        // Отправить сериализованные данные по узлу.
        Ok( Json.toJson(mLknNode) )
      }
    }
  }


  /** BodyParser для выставления тарифа. */
  private def _tfDailyBp = parse.json[ITfDailyMode]


  /** Юзер выставляет тариф для узла.
    *
    * @param rcvrKey Ключ узла.
    * @return 200 + бинарь с обновлёнными данными по тарифу.
    */
  def setTfDaily(rcvrKey: RcvrKey) = csrf.Check {
    isNodeAdmin(rcvrKey).async(_tfDailyBp) { implicit request =>

      lazy val logPrefix = s"setTfDaily(${rcvrKey.mkString("/")}):"

      tfDailyUtil.validateTfDailyMode( request.body ).fold(
        // Ошибка проверки данных реквеста.
        {violations =>
          val violsStr = violations.iterator.mkString("\n ")
          LOGGER.debug(s"$logPrefix Failed to validate tf-mode:\n $violsStr")
          errorHandler.onClientError(request, NOT_ACCEPTABLE, violsStr)
        },

        // Всё ок, обновить текущий узел.
        {tfdm =>
          LOGGER.trace(s"$logPrefix new tf-daily mode: $tfdm")
          for {
            // Вычислить тариф для узла на основе заданного режима.
            tfDailyOpt <- tfDailyUtil.tfMode2tfDaily( tfdm, request.mnode.billing.tariffs.daily )

            // Обновить узел новым тарифом.
            mnode2 <- {
              LOGGER.trace(s"$logPrefix mode=$tfdm ==>> tf = $tfDailyOpt")
              mNodes.tryUpdate( request.mnode ) {
                MNode.billing
                  .composeLens( MNodeBilling.tariffs )
                  .composeLens( MNodeTariffs.daily )
                  .set( tfDailyOpt )
              }
            }

            // Собрать инфу по обновлённому тарифу для http-ответа.
            tfInfo <- tfDailyUtil.getTfInfo(mnode2)

          } yield {
            LOGGER.debug(s"$logPrefix Node#${rcvrKey.last} tfDaily set to $tfDailyOpt")

            // Собрать ответ.
            val someTrue = OptionUtil.SomeBool.someTrue
            val mLknNode = _mkLknNode(
              mnode2,
              isDetailed  = someTrue,
              isAdmin     = someTrue,
              tf          = Some(tfInfo),
            )

            // Собрать HTTP-ответ клиенту
            Ok( Json.toJson(mLknNode) )
          }
        }
      )
    }
  }


  /** Выставление флага isShowOpened.
    *
    * @param adId id карточки.
    * @param isEnabled Состояние галочки.
    * @param rcvrKey Узел-ресивера.
    * @return 200 OK + Json[MLknNode].
    */
  private def setAdvShowOpened(adId: String, isEnabled: Boolean, rcvrKey: RcvrKey) = csrf.Check {
    canFreelyAdvAdOnNode(adId, rcvrKey).async { implicit request =>
      val nodeId = request.mnode.id.get
      val pred = MPredicates.Receiver.Self
      lazy val logPrefix = s"setAdvShowOpened($adId, $isEnabled, $nodeId):"

      val madEdgeSelfOpt = request.mad.edges
        .withNodePred(nodeId, pred)
        .nextOption()

      madEdgeSelfOpt.fold [Future[Result]] {
        LOGGER.warn(s"$logPrefix Cannot modify showOpened on in-exising edge $pred")
        errorHandler.onClientError(request, NOT_FOUND, s"!pred: $pred for $nodeId")

      } { edge0 =>
        if ((edge0.info.flagsMap contains MEdgeFlags.AlwaysOpened) ==* isEnabled) {
          LOGGER.debug(s"$logPrefix Nothing to do.")
          NoContent

        } else {
          LOGGER.trace(s"$logPrefix Will update mad#$adId with showOpened=$isEnabled on $nodeId")
          val edge2 = (
            MEdge.nodeIds
              .set( Set.empty + nodeId ) andThen
            MEdge.info
              .composeLens( MEdgeInfo.flags )
              .modify { flags0 =>
                if (isEnabled) {
                  MEdgeFlagData( MEdgeFlags.AlwaysOpened ) +: flags0.toSeq
                } else {
                  flags0
                    .filter(_.flag !=* MEdgeFlags.AlwaysOpened)
                }
              }
          )(edge0)

          // Если в данном эдже было несколко nodeIds, то распилить на несколько эджей.
          val eTail = if (edge0.nodeIds.sizeIs > 1) {
            val eOld2 = MEdge.nodeIds.modify(_ - nodeId)(edge0)
            eOld2 :: Nil
          } else {
            Nil
          }
          val es2 = edge2 :: eTail

          // Дождаться завершения апдейта.
          for {
            // Запустить обновление карточки на стороне ES: перезаписать эдж:
            mad2 <- mNodes.tryUpdate( request.mad ) {
              // TODO Тут используется edge0/edge2 снаружи функции, хотя возможно стоит заново извлекать его из эджей текущего инстанса?
              MNode.edges.modify { edges0 =>
                MNodeEdges.out.set(
                  MNodeEdges.edgesToMap1(
                    (
                      edges0.withoutNodePred(nodeId, pred) ::
                        es2 ::
                        Nil
                      )
                      .iterator
                      .flatten
                  )
                )(edges0)
              }
            }
          } yield {
            LOGGER.trace(s"$logPrefix Done.")

            // Собрать MLknNode в ответ:
            val someTrue = Some(true)
            val mLknNode = _mkLknNode(
              request.mnode,
              madOpt      = Some(mad2),
              isAdmin     = someTrue,
              isDetailed  = someTrue,
            )

            Ok( Json.toJson(mLknNode) )
          }
        }
      }
    }
  }


  /** Выставление флага isShowOpened.
    *
    * @param adId id карточки.
    * @param isEnabled Состояние галочки.
    * @param rcvrKey Узел-ресивера.
    */
  private def setAlwaysOutlined(adId: String, isEnabled: Boolean, rcvrKey: RcvrKey) = csrf.Check {
    canFreelyAdvAdOnNode(adId, rcvrKey).async { implicit request =>
      val nodeId = request.mnode.id.get
      val pred = MPredicates.Receiver.Self
      lazy val logPrefix = s"setAlwaysOutlined($adId, $isEnabled, $nodeId):"

      val madEdgeSelfOpt = request.mad.edges
        .withNodePred(nodeId, pred)
        .nextOption()

      madEdgeSelfOpt.fold [Future[Result]] {
        LOGGER.warn(s"$logPrefix Cannot modify AlwaysOutlined on in-exising edge $pred")
        errorHandler.onClientError(request, NOT_FOUND, s"!pred: $pred for $nodeId")

      } { edge0 =>
        if (edge0.info.flagsMap.contains(MEdgeFlags.AlwaysOutlined) ==* isEnabled) {
          LOGGER.debug(s"$logPrefix Nothing to do.")
          NoContent

        } else {
          LOGGER.trace(s"$logPrefix Will update mad#$adId with AlwaysOutlined=$isEnabled on $nodeId")

          val edge2 = (
            MEdge.nodeIds
              .set( Set.empty + nodeId ) andThen
            MEdge.info.modify { info0 =>
              val fd = MEdgeFlagData( MEdgeFlags.AlwaysOutlined )
              val flags2 =
                if (isEnabled) info0.flags.toSeq appended fd
                else (info0.flagsMap - fd.flag).values
              MEdgeInfo.flags.set( flags2 )(info0)
            }
          )(edge0)

          // Если в данном эдже было несколко nodeIds, то распилить на несколько эджей.
          val eTail = if (edge0.nodeIds.sizeIs > 1) {
            val eOld2 = MEdge.nodeIds.modify(_ - nodeId)(edge0)
            eOld2 :: Nil
          } else {
            Nil
          }
          val es2 = edge2 :: eTail

          // Дождаться завершения апдейта.
          for {
            // Запустить обновление карточки на стороне ES: перезаписать эдж:
            mad2 <- mNodes.tryUpdate( request.mad ) {
              // TODO Тут используется edge0/edge2 снаружи функции, хотя возможно стоит заново извлекать его из эджей текущего инстанса?
              MNode.edges.modify { edges0 =>
                MNodeEdges.out.set(
                  MNodeEdges.edgesToMap1(
                    (
                      edges0.withoutNodePred(nodeId, pred) ::
                        es2 ::
                        Nil
                      )
                      .iterator
                      .flatten
                  )
                )(edges0)
              }
            }
          } yield {
            LOGGER.trace(s"$logPrefix Done.")

            // Собрать MLknNode в ответ:
            val someTrue = OptionUtil.SomeBool.someTrue
            val mLknNode = _mkLknNode(
              request.mnode,
              isAdmin     = someTrue,
              isDetailed  = someTrue,
              madOpt      = Some(mad2),
            )

            Ok( Json.toJson(mLknNode) )
          }
        }
      }
    }
  }


  /** Пакетный запрос инфы по маячкам.
    * Здесь без конкретики: возвращается только базовая инфа по каждому маячку из списка:
    * существует или нет, кому принадлежит и т.д.
    *
    * @return JSON-ответ по запрошенным маячкам.
    */
  def beaconsScan(qs: MLknBeaconsScanReq) = csrf.Check {
    val acl = qs.adId
      // Приведение к MAdOptProdReq. Ненужное здесь поле producer тут может быть null.
      .fold(
        maybeAuth()
          .andThen( new reqUtil.ActionTransformerImpl[MReq, MAdOptProdReq] {
            override protected def transform[A](request: MReq[A]): Future[MAdOptProdReq[A]] = {
              val req2 = MAdOptProdReq(
                madOpt    = None,
                producer  = null,
                request   = request.request,
                user      = request.user,
              )
              Future.successful( req2 )
            }
          })
      ) {
        canAdvAd(_)
          .andThen( new reqUtil.ActionTransformerImpl[MAdProdReq, MAdOptProdReq] {
            override protected def transform[A](request: MAdProdReq[A]): Future[MAdOptProdReq[A]] = {
              val req2 = MAdOptProdReq( request )
              Future.successful( req2 )
            }
          })
      }

    // TODO Активировать BFP, подобрав параметры. Дефолтовые слишком агрессивные для часто-вызываемого скана.
    //bruteForceProtect {
      acl.async { implicit request =>
        val bcnsReqCount = qs.beaconUids.size
        val startedAtMs = System.currentTimeMillis()
        lazy val logPrefix = s"beaconsScan($bcnsReqCount bcns${qs.adId.fold("")(", ad#" + _)})#$startedAtMs:"
        LOGGER.trace(s"$logPrefix User#${request.user.personIdOpt.orNull} [${request.remoteClientAddress}] requesting info about $bcnsReqCount beacons: ${qs.beaconUids.mkString(" ")}")

        for {
          // Поискать в базе инфу по маячкам.
          // dynSearchIds: Нагрузку стараемся максимально размазать по кэшу узлов, т.к. в случае залогиненного юзера,
          // чтение цепочек узлов для проверки прав доступа идёт через этот же кэш.
          bcnIdsCleared <- mNodes.dynSearchIds(
            new MNodeSearch {
              override val withIds = qs.beaconUids.toSeq
              override val nodeTypes = MNodeTypes.BleBeacon :: Nil
              override val limit = bcnsReqCount
              // isDisabled: по флагу пока не фильтруем, т.к. иначе клиент подумает, что маячок свободен для регистрации.
            }
          )
          bcnIdsClearedSet = bcnIdsCleared.toSet

          // Для дальней работы требуется получить на руки узлы-маячки, но через кэш.
          bcnNodesFut = mNodes.multiGetCache( bcnIdsClearedSet )

          // Если юзер залогинен, то надо запустить проверки от узла до текущего юзера.
          hasAdminRightsForIdsFut = {
            LOGGER.trace(s"$logPrefix Cleared beaconIds[${qs.beaconUids.size}] => [${bcnIdsClearedSet.size}/${bcnIdsCleared.total}]: ${bcnIdsCleared.mkString(" ")}")

            request.user.personIdOpt.fold {
              Future.successful( Map.empty[String, Boolean] )
            } { _ =>
              Future.traverse( bcnIdsClearedSet.toIterable ) { bcnId =>
                isNodeAdmin.isNodeAdminUpFrom(
                  nodeId = bcnId,
                  user   = request.user,
                ).map { nodePathOpt =>
                  bcnId -> nodePathOpt.nonEmpty
                }
              }
                .map { bcnsInfos =>
                  val bcnHaveAdminMap = bcnsInfos.toMap
                  LOGGER.trace(s"$logPrefix isAdmin: Tested beacon-nodes[${bcnHaveAdminMap.size}] against user#${request.user.personIdOpt.orNull}.\n Have admin ${bcnHaveAdminMap.valuesIterator.count(identity)} nodes: ${bcnHaveAdminMap.iterator.filter(_._2).map(_._1).mkString(" ")}")
                  bcnHaveAdminMap
                }
            }
          }

          bcnNodes <- bcnNodesFut

          // Для вывода инфы по маячкам, надо получить на руки все родительские узлы для найденных маячков.
          // Определить id родительских элементов:
          childToParentIds = bcnNodes
            .iterator
            .map { mnode =>
              val parentNodeIds = mnode.edges
                .withPredicateIter( MPredicates.OwnedBy )
                .flatMap(_.nodeIds)
                .toSet
              mnode.id.get -> parentNodeIds
            }
            .toMap

          // Получить из базы родительские узлы маячков:
          parentIds = childToParentIds
            .valuesIterator
            .flatten
            .toSet
          parentNodes <- mNodes.multiGetMapCache( parentIds )

          // Собрать карту названий родительских узлов для подписей у дочерних узлов:
          beaconIdDescrs = {
            LOGGER.trace(s"$logPrefix Found ${parentNodes.size}/${parentIds.size} owner-nodes: ${parentNodes.keysIterator.mkString(" ")}")
            (for {
              bcnNode <- bcnNodes.iterator
              bcnNodeId <- bcnNode.id
              bcnParentIds <- childToParentIds.get( bcnNodeId )

              // По идее owner только один. Но мы форсируем тут это - брать только первое доступное имя родительского узла.
              bcnParentName2 <- (for {
                parentId <- bcnParentIds.iterator
                parentNode <- parentNodes.get( parentId )
                parentName <- parentNode.guessDisplayNameOrId
              } yield {
                parentName
              })
                .nextOption()
                .orElse {
                  LOGGER.trace(s"$logPrefix Not found parent/owner node for known beacon#${bcnNodeId}, parents[${bcnParentIds.size}] = ${bcnParentIds.mkString(" ")}")
                  bcnParentIds.headOption
                }
            } yield {
              bcnNodeId -> bcnParentName2
            })
              .toMap
          }

          // Дождаться инфы по правам доступа:
          hasAdminRightsForIds <- hasAdminRightsForIdsFut

        } yield {

          LOGGER.trace {
            val hasAdminNodes = hasAdminRightsForIds
              .view
              .filter(_._2)
              .keySet
              .toSeq
            s"$logPrefix User#${request.user.personIdOpt.orNull} is admin for [${hasAdminNodes.length}/${hasAdminRightsForIds.size}] nodes: ${hasAdminNodes.mkString(" ")}"
          }

          val resp = MLknNodeResp(
            subTree = Tree.Node(
              // Корневой узел-заглушка:
              root = MLknNode("", ntype = None),
              // Рендер ответа внутри subforest.
              forest = (for {
                bcnNode <- bcnNodes.iterator
                bcnId <- bcnNode.id
              } yield {
                Tree.Leaf(
                  MLknNode(
                    id          = bcnId,
                    ntype       = Some( MNodeTypes.BleBeacon ),
                    name        = bcnNode.guessDisplayName,
                    // Права есть или нет - возвращаем, для isAnon тут будет всегда None.
                    isAdmin     = hasAdminRightsForIds.get( bcnId ),
                    // Недетализованный ответ: тариф и прочее не вычислялись.
                    isDetailed  = OptionUtil.SomeBool.someFalse,
                    // tf: тариф не возвращаем: юзер откроет узел и посмотрит.
                    // adv: тут пока только ADN-режим, без карточки.
                    parentName  = beaconIdDescrs.get( bcnId ),
                    options     = _hasAdv2( bcnId, request.madOpt ) +
                      // isEnabled возвращаем: Пусть все знают, что какой-то маячок выключен в системе, чтобы у люди могли быстрее отлаживать проблемы.
                      (MLknOpKeys.NodeEnabled -> MLknOpValue(bool = OptionUtil.SomeBool( bcnNode.common.isEnabled ))),
                  )
                )
              })
                .to( LazyList )
                .toEphemeralStream,
            ),
          )

          val r = Ok( Json.toJson(resp) )
            // Выставляем кэш на клиенте, но не слишком долгий.
            .cacheControl( 3600 )

          // Для оценки скорости работы, логгируем полное время обработки запроса.
          LOGGER.trace(s"$logPrefix Took ${System.currentTimeMillis() - startedAtMs} ms with ${resp.subTree.subForest.length} results, loggedIn?${request.user.personIdOpt.orNull}")

          r
        }
      }
    //}
  }


  /** v2 - унифицированное управление флагами и прочими опциями, вместо кучи похожих экшенов/полей и т.д.
    *
    * POST-инг выставления какого-то опционального значения на узле.
    * Например, значение размещения карточки на узле или управление активностью узла.
    * Значение конкретного параметра -- находится внутри JSON-тела запроса.
    *
    * @return 200 OK + обновлённый MLknNode.
    */
  def modifyNode(qs: MLknModifyQs): Action[AnyContent] = {
    def __notFoundA = Action.async { implicit request =>
      LOGGER.warn(s"modifyNode($qs): Not supported qs combo.")
      errorHandler.onClientError( request, NOT_FOUND )
    }

    // Режим управления размещением в узлах.
    def __isTrue = qs.opValue.bool.getOrElseFalse

    // Роутинг запроса в тот или иной конкретный экшен.
    // ACL в разных экшенах различается, и различаются некоторые внутренние куски, поэтому унифицировать это проблематично.

    qs.adIdOpt.fold {
      qs.opKey match {
        case MLknOpKeys.NodeEnabled =>
          setNodeEnabled( qs.onNodeRk.last, __isTrue )
        case _ =>
          __notFoundA
      }

    } { adId =>
      qs.opKey match {
        case MLknOpKeys.AdvEnabled =>
          setAdv( adId, __isTrue, qs.onNodeRk )
        case MLknOpKeys.ShowOpened =>
          setAdvShowOpened( adId, __isTrue, qs.onNodeRk )
        case MLknOpKeys.AlwaysOutlined =>
          setAlwaysOutlined( adId, __isTrue, qs.onNodeRk )
        case _ =>
          __notFoundA
      }
    }
  }

}
