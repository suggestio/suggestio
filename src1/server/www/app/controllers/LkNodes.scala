package controllers

import java.time.OffsetDateTime

import akka.util.ByteString
import javax.inject.{Inject, Singleton}

import io.suggest.bin.ConvCodecs
import io.suggest.common.fut.FutureUtil
import FutureUtil.HellImplicits._
import io.suggest.adn.MAdnRights
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill.tf.daily.ITfDailyMode
import io.suggest.common.empty.EmptyUtil
import io.suggest.es.model.MEsUuId
import io.suggest.init.routed.MJsiTgs
import io.suggest.lk.nodes._
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.pick.PickleUtil
import io.suggest.pick.PickleSrvUtil._
import io.suggest.primo.id.IId
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.mlk.nodes.{MLkAdNodesTplArgs, MLkNodesTplArgs}
import models.mproj.ICommonDi
import models.req.IReq
import util.acl._
import util.adn.NodesUtil
import util.billing.{Bill2Util, TfDailyUtil}
import util.lk.nodes.LkNodesUtil
import views.html.lk.nodes._
import io.suggest.scalaz.ScalazUtil.Implicits._

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
@Singleton
class LkNodes @Inject() (
                          bill2Util                 : Bill2Util,
                          isNodeAdmin               : IsNodeAdmin,
                          lkNodesUtil               : LkNodesUtil,
                          canFreelyAdvAdOnNode      : CanFreelyAdvAdOnNode,
                          canAdvAd                  : CanAdvAd,
                          tfDailyUtil               : TfDailyUtil,
                          nodesUtil                 : NodesUtil,
                          mNodes                    : MNodes,
                          canChangeNodeAvailability : CanChangeNodeAvailability,
                          bruteForceProtect         : BruteForceProtect,
                          reqUtil                   : ReqUtil,
                          override val mCommonDi    : ICommonDi
                        )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._


  private def _hasAdv(nodeId: String, madOpt: Option[MNode]): Option[Boolean] = {
    for (mad <- madOpt) yield {
      mad.edges
        .withNodePred(nodeId, MPredicates.Receiver)
        .nonEmpty
    }
  }

  private def _subNodesRespFor(mnode: MNode, madOpt: Option[MNode])
                              (implicit ctx: Context): Future[MLknNodeResp] = {
    import ctx.request

    val nodeId = mnode.id.get

    // Запустить поиск узлов.
    val subNodesFut = mNodes.dynSearch {
      lkNodesUtil.subNodesSearch(nodeId)
    }

    // Можно ли менять доступность узла?
    val canChangeAvailabilityFut = canChangeNodeAvailability.adminCanChangeAvailabilityOf(mnode)

    val tfDailyInfoOptFut = madOpt.fold {
      tfDailyUtil.getTfInfo(mnode)(ctx)
        .map( EmptyUtil.someF )
    } { _ =>
      Future.successful(None)
    }

    // Рендер найденных узлов в данные для модели формы.
    for {
      subNodes                <- subNodesFut
      canChangeAvailability   <- canChangeAvailabilityFut
      tfDailyInfoOpt          <- tfDailyInfoOptFut
    } yield {
      MLknNodeResp(
        // Надо ли какую-то инфу по текущему узлу возвращать?
        info = MLknNode(
          id                    = nodeId,
          name                  = mnode.guessDisplayNameOrIdOrQuestions,
          ntypeId               = mnode.common.ntype.strId,
          isEnabled             = mnode.common.isEnabled,
          canChangeAvailability = Some( canChangeAvailability ),
          hasAdv                = _hasAdv(nodeId, madOpt),
          tf                    = tfDailyInfoOpt
        ),

        children = for (mnode <- subNodes) yield {
          val chNodeId = mnode.id.get
          MLknNode(
            id                = chNodeId,
            name              = mnode.guessDisplayNameOrIdOrQuestions,
            ntypeId           = mnode.common.ntype.strId,
            isEnabled         = mnode.common.isEnabled,
            // На уровне под-узлов это значение не важно, т.к. для редактирования надо зайти в под-узел и там будет уже нормальный ответ на вопрос.
            canChangeAvailability = None,
            hasAdv            = _hasAdv(chNodeId, madOpt),
            tf                = None
          )
        }
      )
    }
  }


  /** Внутренняя модель данных по форме и контексту.
    *
    * @param formStateB64 base64-строка с данными формы для инициализации на клиенте.
    * @param ctx Обычный контекст.
    */
  private case class _MLknFormPrep(
                                    formStateB64      : String,
                                    implicit val ctx  : Context
                                  )

  /** Дедубликация кода экшена HTML-рендера формы на узле и для карточки.
    *
    * @param onNode Текущий узел ADN или продьюсер карточки.
    * @param madOpt Рекламная карточки, если известна.
    * @param request Исходный HTTP-реквест.
    * @return Подготовленные данные по форме и контексту.
    */
  private def _lknFormPrep(onNode: MNode, madOpt: Option[MNode] = None)(implicit request: IReq[_]): Future[_MLknFormPrep] = {
    // Собрать модель данных инициализации формы с начальным состоянием формы. Сериализовать в base64.
    val nodeId = onNode.id.get

    // Поискать другие пользовательские узлы верхнего уровня. TODO А что будет, если текущий узел не является прямо-подчинённым текущему юзеру? Каша из узлов?
    val otherPersonNodesFut = request.user.personIdOpt
      .fold[Future[List[MLknNodeResp]]] (Nil) { personId =>
        for {
          mnodes <- mNodes.dynSearch {
            nodesUtil.personNodesSearch(personId, withoutIds1 = onNode.id.toList)
          }
        } yield {
          val someTrue = Some(true)

          val iter = for {
            mnode <- mnodes.iterator
            mnodeId <- mnode.id
          } yield {
            val mLknNode = MLknNode(
              id                        = mnodeId,
              name                      = mnode.guessDisplayNameOrIdOrQuestions,
              ntypeId                   = mnode.common.ntype.strId,
              isEnabled                 = mnode.common.isEnabled,
              canChangeAvailability     = someTrue,
              hasAdv                    = _hasAdv(mnodeId, madOpt),
              tf                        = None // Не факт, что это важно.
            )
            MLknNodeResp(mLknNode, Nil)
          }

          iter.toList
        }
      }

    // Пока подготовить контекст рендера шаблона
    val ctxFut = for {
      lkCtxData <- request.user.lkCtxDataFut
    } yield {
      implicit val lkCtxData2 = lkCtxData.withJsiTgs(
        MJsiTgs.LkNodesForm :: lkCtxData.jsiTgs
      )
      getContext2
    }

    val formStateB64Fut = for {
      // Запустить поиск под-узлов для текущего узла.
      ctx               <- ctxFut
      subNodesResp      <- _subNodesRespFor(onNode, madOpt = madOpt)(ctx)
      otherPersonNodes  <- otherPersonNodesFut
    } yield {
      val minit = MLknFormInit(
        conf = MLknConf(
          onNodeId = nodeId,
          adIdOpt  = madOpt.flatMap(_.id)
        ),
        nodes0   = subNodesResp :: otherPersonNodes
      )
      PickleUtil.pickleConv[MLknFormInit, ConvCodecs.Base64, String](minit)
    }

    // Отрендерить и вернуть HTML-шаблон со страницей для формы.
    for {
      formStateB64    <- formStateB64Fut
      ctx             <- ctxFut
    } yield {
      _MLknFormPrep(formStateB64, ctx)
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
    isNodeAdmin(nodeId, U.Lk).async { implicit request =>
      for {
        res               <- _lknFormPrep(request.mnode)
      } yield {
        val args = MLkNodesTplArgs(
          formState = res.formStateB64,
          mnode     = request.mnode
        )
        Ok( NodesTpl(args)(res.ctx) )
      }
    }
  }


  /** Получение инфы по узлам, относящимся к указанному узлу.
    *
    * @param nodeId id узла.
    * @return 200 OK с бинарем ответа.
    */
  def nodeInfo(nodeId: String) = csrf.Check {
    isNodeAdmin(nodeId).async { implicit request =>
      for {
        resp <- _subNodesRespFor(request.mnode, madOpt = None)    // TODO Сделать madOpt универсальным
      } yield {
        LOGGER.trace(s"nodeInfo($nodeId): Found ${resp.children.size} sub-nodes: ${IId.els2ids(resp.children).mkString(", ")}")
        val bbuf = PickleUtil.pickle(resp)
        Ok( ByteString(bbuf) )
      }
    }
  }


  /** BodyParser для тела запроса по созданию/редактированию узла. */
  private def _mLknNodeReqBP = reqUtil.picklingBodyParser[MLknNodeReq]


  /** Создать новый узел (маячок) с указанными параметрами.
    * POST-запрос с данными добавля
    *
    * @param parentNodeId id родительского узла.
    * @return 200 OK + данные созданного узла.
    *         409 Conflict - id узла уже занят кем-то.
    */
  def createSubNodeSubmit(parentNodeId: String) = csrf.Check {
    bruteForceProtect {
      isNodeAdmin(parentNodeId).async(_mLknNodeReqBP) { implicit request =>

        lazy val logPrefix = s"addSubNodeSubmit($parentNodeId)[${System.currentTimeMillis()}]:"

        lkNodesUtil.validateNodeReq( request.body, isEdit = false ).fold(
          {violations =>
            // Конвертим violations в нормальную коллекцию, чтобы просто её отформатировать без боли.
            val violsList = violations.asIterable
            LOGGER.debug(s"$logPrefix Failed to validate data:\n ${request.body}\n Violations: ${violsList.mkString(", ")}")
            NotAcceptable(s"Invalidated: ${violsList.mkString("\n", ",\n", "")}")
          },

          // Данные по создаваемому узлу приняты и выверены. Произвести создание нового узла.
          {addNodeInfo =>
            LOGGER.trace(s"$logPrefix Validated: $addNodeInfo")

            // Нужно проверить, не занят ли указанный id каким-то другим узлом.
            val currIdNodeOptFut = FutureUtil.optFut2futOpt( addNodeInfo.id ) { newNodeId =>
              mNodesCache.getById(newNodeId)
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
                    nodeIds   = Set( request.user.personIdOpt.get )
                  )

                  val parentNodeIds = Set[String](parentNodeId)
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
                  rights      = Set( MAdnRights.RECEIVER ),
                  isUser      = !request.user.isSuper,
                  showInScNl  = false
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
              tfDailyInfo   <- tfDailyInfoFut
            } yield {
              // Собираем ответ, сериализуем, возвращаем...
              val mResp = MLknNode(
                id        = newNodeId,
                name      = addNodeInfo.name,
                ntypeId   = ntype.strId,
                isEnabled = isEnabled,
                // Текущий юзер создал юзер, значит он может его и удалить.
                canChangeAvailability = Some(true),
                hasAdv    = None,
                tf        = Some(tfDailyInfo)
              )
              val bbuf = PickleUtil.pickle[MLknNode](mResp)
              Ok( ByteString(bbuf) )
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
  def setNodeEnabled(nodeId: String, isEnabled: Boolean) = csrf.Check {
    bruteForceProtect {
      canChangeNodeAvailability(nodeId).async { implicit request =>

        // Запустить обновление узла.
        val saveFut = mNodes.tryUpdate(request.mnode) { mnode =>
          mnode.copy(
            common = mnode.common.copy(
              isEnabled = isEnabled
            ),
            meta = mnode.meta.copy(
              basic = mnode.meta.basic.copy(
                dateEdited = Some(OffsetDateTime.now())
              )
            )
          )
        }

        // В фоне собрать инфу по тарифу текущему.
        val tfDailyInfoFut = tfDailyUtil.getTfInfo( request.mnode )

        // Когда сохранение будет выполнено, то вернуть данные по обновлённому узлу.
        for {
          _             <- saveFut
          tfDailyInfo   <- tfDailyInfoFut
        } yield {
          LOGGER.debug(s"setNodeEnabled($nodeId): enabled => $isEnabled")
          val mLknNode = MLknNode(
            id                      = nodeId,
            name                    = request.mnode.guessDisplayNameOrIdOrQuestions,
            ntypeId                 = request.mnode.common.ntype.strId,
            isEnabled               = isEnabled,
            canChangeAvailability   = Some(true),
            hasAdv                  = None,
            tf                      = Some(tfDailyInfo)
          )
          val bbuf = PickleUtil.pickle(mLknNode)
          Ok(ByteString(bbuf))
        }
      }
    }
  }


  /** Команда к удалению какого-то узла.
    *
    * @param nodeId id узла.
    * @return 204 No content | 404 Not found
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
          if (isDeleted) {
            NoContent
          } else {
            NotFound
          }
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
            NotAcceptable("Invalid name.")
          },

          {binded =>
            LOGGER.trace(s"$logPrefix Binded: $binded")

            // Запустить апдейт узла.
            val updateFut = mNodes.tryUpdate( request.mnode ) { mnode =>
              mnode.copy(
                meta = mnode.meta.copy(
                  basic = {
                    val nameOpt = Some( binded.name )
                    mnode.meta.basic.copy(
                      nameOpt       = nameOpt,
                      nameShortOpt  = nameOpt,
                      dateEdited    = Some( OffsetDateTime.now() )
                    )
                  }
                )
              )
            }

            // В фоне собрать инфу по тарифу текущему.
            val tfDailyInfoFut = tfDailyUtil.getTfInfo( request.mnode )

            // Когда будет сохранено, отрендерить свежую инфу по узлу.
            for {
              mnode         <- updateFut
              tfDailyInfo   <- tfDailyInfoFut
            } yield {
              LOGGER.debug(s"$logPrefix Ok, nodeVsn => ${mnode.versionOpt.orNull}")
              val m = MLknNode(
                id                      = nodeId,
                name                    = mnode.guessDisplayNameOrIdOrQuestions,
                ntypeId                 = mnode.common.ntype.strId,
                isEnabled               = mnode.common.isEnabled,
                canChangeAvailability   = Some(true),
                hasAdv                  = None,
                tf                      = Some(tfDailyInfo)
              )
              val bbuf = PickleUtil.pickle( m )
              Ok( ByteString(bbuf) )
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
    canAdvAd(adId).async { implicit request =>
      // Сборка дерева узлов. Интересует узел-продьюсер.
      for {
        res <- _lknFormPrep(request.producer, madOpt = Some(request.mad))
      } yield {
        val rArgs = MLkAdNodesTplArgs(
          formState = res.formStateB64,
          mad = request.mad,
          producer = request.producer
        )
        val html = AdNodesTpl(rArgs)(res.ctx)
        Ok(html)
      }
    }
  }


  /** Обновить статус карточки на узле.
    *
    * @param adIdU id рекламной карточки.
    * @param isEnabled Размещена или нет?
    * @param rcvrKey Ключ узла.
    * @return 200 OK + MLknNode.
    */
  def setAdv(adIdU: MEsUuId, isEnabled: Boolean, rcvrKey: RcvrKey) = csrf.Check {
    canFreelyAdvAdOnNode(adIdU, rcvrKey).async { implicit request =>
      // Выполнить обновление текущей карточки согласно значению флага isEnabled.
      val adId = request.mad.id.get
      val nodeId = request.mnode.id.get
      val nodeIds = Set(nodeId)

      // Поискать аналогичные бесплатные размещения в биллинге, заглушив их.
      val suppressRelatedItemsFut = slick.db.run {
        bill2Util.justFinalizeItemsLike(
          nodeId  = adId,
          iTypes  = MItemTypes.AdvDirect :: Nil,
          rcvrIds = nodeIds
        )
      }

      // Запустить обновление узла на стороне ES.
      val updateNodeFut = mNodes.tryUpdate( request.mad ) { mad =>
        mad.copy(
          edges = mad.edges.copy(
            out = {
              // Удалить эдж текущего размещения. Даже если isEnabled=true, всё равно надо отфильтровать старый эдж, чтобы перезаписать его.
              val edgesIter1 = mad.edges.withoutNodePred( nodeId, MPredicates.Receiver )
              val edgesIter2 = if (isEnabled) {
                // Найти/добавить эдж до указанного узла.
                val medge = MEdge(
                  predicate = MPredicates.Receiver.Self,
                  nodeIds   = nodeIds
                )
                edgesIter1 ++ (medge :: Nil)
              } else {
                // isEnabled=false, удаление эджа размещения уже было выше.
                edgesIter1
              }
              MNodeEdges.edgesToMap1( edgesIter2 )
            }
          )
        )
      }

      for {
        itemsSuppressed   <- suppressRelatedItemsFut
        _                 <- updateNodeFut
      } yield {
        LOGGER.trace(s"setAdv(ad#$adId, node#$nodeId): adv => $isEnabled, suppressed $itemsSuppressed in billing")

        // Собрать ответ.
        val mLknNode = MLknNode(
          id        = nodeId,
          name      = request.mnode.guessDisplayNameOrIdOrQuestions,
          ntypeId   = request.mnode.common.ntype.strId,
          isEnabled = request.mnode.common.isEnabled,
          canChangeAvailability = Some(true),
          hasAdv    = Some(isEnabled),
          tf        = None // На странице размещения это не важно
        )

        // Отправить сериализованные данные по узлу.
        val bbuf = PickleUtil.pickle(mLknNode)
        Ok( ByteString(bbuf) )
      }
    }
  }

  /** BodyParser для выставления тарифа. */
  private def _tfDailyBp = reqUtil.picklingBodyParser[ITfDailyMode]


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
          NotAcceptable( violsStr )
        },

        // Всё ок, обновить текущий узел.
        {tfdm =>
          for {
            // Вычислить тариф для узла на основе заданного режима.
            tfDailyOpt <- tfDailyUtil.tfMode2tfDaily( tfdm )

            // Обновить узел новым тарифом.
            mnode2 <- {
              LOGGER.trace(s"$logPrefix mode=$tfdm ==>> tf = $tfDailyOpt")
              mNodes.tryUpdate( request.mnode ) { mnode =>
                mnode.copy(
                  billing = mnode.billing.copy(
                    tariffs = mnode.billing.tariffs.copy(
                      daily = tfDailyOpt
                    )
                  )
                )
              }
            }

            // Собрать инфу по обновлённому тарифу для http-ответа.
            tfInfo <- tfDailyUtil.getTfInfo(mnode2)

          } yield {
            LOGGER.debug(s"$logPrefix Node#${rcvrKey.last} tfDaily set to $tfDailyOpt")

            // Собрать ответ.
            val mLknNode = MLknNode(
              id        = mnode2.id.get,
              name      = mnode2.guessDisplayNameOrIdOrQuestions,
              ntypeId   = mnode2.common.ntype.strId,
              isEnabled = mnode2.common.isEnabled,
              canChangeAvailability = Some(true),
              hasAdv    = None,
              tf        = Some(tfInfo)
            )

            // Собрать HTTP-ответ клиенту
            val bbuf = PickleUtil.pickle( mLknNode )
            Ok( ByteString(bbuf) )
          }
        }
      )
    }
  }


}
