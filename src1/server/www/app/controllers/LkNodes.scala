package controllers

import java.time.OffsetDateTime

import akka.util.ByteString
import com.google.inject.Inject
import io.suggest.bin.ConvCodecs
import io.suggest.common.fut.FutureUtil
import FutureUtil.HellImplicits._
import io.suggest.es.model.MEsUuId
import io.suggest.init.routed.MJsiTgs
import io.suggest.lk.nodes._
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.pick.{PickleSrvUtil, PickleUtil}
import io.suggest.primo.id.IId
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.req.ReqUtil
import io.suggest.ym.model.common.AdnRights
import models.mlk.nodes.MLkNodesTplArgs
import models.mproj.ICommonDi
import models.req.IReqHdr
import util.acl.{BruteForceProtect, CanChangeNodeAvailability, IsNodeAdmin}
import util.lk.nodes.LkNodesUtil
import views.html.lk.nodes.nodesTpl

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
class LkNodes @Inject() (
                          isNodeAdmin               : IsNodeAdmin,
                          lkNodesUtil               : LkNodesUtil,
                          pickleSrvUtil             : PickleSrvUtil,
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
  import pickleSrvUtil._


  private def _subNodesRespFor(nodeId: String, withCurrNode: Option[MNode] = None)(implicit req: IReqHdr): Future[MLknNodeResp] = {
    // Запустить поиск узлов.
    val subNodesFut = mNodes.dynSearch {
      lkNodesUtil.subNodesSearch(nodeId)
    }

    // Можно ли менять доступность узла?
    val canChangeAvailabilityFut = withCurrNode.fold [Future[Boolean]] (false) { mnode =>
      canChangeNodeAvailability.adminCanChangeAvailabilityOf(mnode)
    }

    // Рендер найденных узлов в данные для модели формы.
    for {
      subNodes                <- subNodesFut
      canChangeAvailability   <- canChangeAvailabilityFut
    } yield {
      MLknNodeResp(
        // Надо ли какую-то инфу по текущему узлу возвращать?
        info = for {
          mnode <- withCurrNode
        } yield {
          MLknNode(
            id                    = nodeId,
            name                  = mnode.guessDisplayNameOrIdOrQuestions,
            ntypeId               = mnode.common.ntype.strId,
            isEnabled             = mnode.common.isEnabled,
            canChangeAvailability = Some( canChangeAvailability )
          )
        },

        children = for (mnode <- subNodes) yield {
          MLknNode(
            id                = mnode.id.get,
            name              = mnode.guessDisplayNameOrIdOrQuestions,
            ntypeId           = mnode.common.ntype.strId,
            isEnabled         = mnode.common.isEnabled,
            // На уровне под-узлов это значение не важно, т.к. для редактирования надо зайти в под-узел и там будет уже нормальный ответ на вопрос.
            canChangeAvailability = None
          )
        }
      )
    }
  }


  /**
    * Рендер страницы с формой управления подузлами текущего узла.
    * Сама форма реализована через react, тут у нас лишь страничка-обёртка.
    *
    * @param nodeId id текущей узла, т.е. узла с которым идёт взаимодействие.
    * @return 200 + HTML, если у юзера достаточно прав для управления узлом.
    */
  def nodesOf(nodeId: MEsUuId) = csrf.AddToken {
    isNodeAdmin(nodeId, U.Lk).async { implicit request =>

      // Собрать модель данных инициализации формы с начальным состоянием формы. Сериализовать в base64.
      val formStateB64Fut = for {
        // Запустить поиск под-узлов для текущего узла.
        subNodesResp  <- _subNodesRespFor(nodeId, Some(request.mnode))
      } yield {
        val minit = MLknFormInit(
          onNodeId = nodeId,
          adIdOpt  = None,      // Сейчас находимся явно вне карточки, поэтому просто управление узлами.
          nodes0   = subNodesResp,
          // Собрать начальное состояние формы.
          form     = MLknForm()
        )
        PickleUtil.pickleConv[MLknFormInit, ConvCodecs.Base64, String](minit)
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

      // Отрендерить и вернуть HTML-шаблон со страницей для формы.
      for {
        formStateB64    <- formStateB64Fut
        ctx             <- ctxFut
      } yield {
        val args = MLkNodesTplArgs(
          formState = formStateB64,
          mnode     = request.mnode
        )
        Ok( nodesTpl(args)(ctx) )
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
        resp <- _subNodesRespFor(nodeId, Some(request.mnode))
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
            LOGGER.debug(s"$logPrefix Failed to validate data:\n ${request.body}\n Violations: ${violations.mkString(", ")}")
            NotAcceptable(s"Invalidated: ${violations.mkString("\n", ",\n", "")}")
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
                basic = MBasicMeta(
                  nameOpt = Some(addNodeInfo.name)
                )
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
                  rights      = Set( AdnRights.RECEIVER ),
                  isUser      = true,
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

            // Дождаться окончания сохранения нового узла.
            val respFut = for {
              newNodeId <- newNodeIdFut
            } yield {
              // Собираем ответ, сериализуем, возвращаем...
              val mResp = MLknNode(
                id        = newNodeId,
                name      = addNodeInfo.name,
                ntypeId   = ntype.strId,
                isEnabled = isEnabled,
                // Текущий юзер создал юзер, значит он может его и удалить.
                canChangeAvailability = Some(true)
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

        // Когда сохранение будет выполнено, то вернуть данные по обновлённому узлу.
        for {
          _ <- saveFut
        } yield {
          LOGGER.debug(s"setNodeEnabled($nodeId): enabled => $isEnabled")
          val mLknNode = MLknNode(
            id                      = nodeId,
            name                    = request.mnode.guessDisplayNameOrIdOrQuestions,
            ntypeId                 = request.mnode.common.ntype.strId,
            isEnabled               = isEnabled,
            canChangeAvailability   = Some(true)
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
            LOGGER.debug(s"$logPrefix Failed to bind form: ${violations.mkString(", ")}")
            NotAcceptable("Invalid name.")
          },

          {binded =>
            LOGGER.trace(s"$logPrefix Binded: $binded")

            // Запустить апдейт узла.
            val updateFut = mNodes.tryUpdate( request.mnode ) { mnode =>
              mnode.copy(
                meta = mnode.meta.copy(
                  basic = mnode.meta.basic.copy(
                    nameOpt = Some( binded.name )
                  )
                )
              )
            }

            // Когда будет сохранено, отрендерить свежую инфу по узлу.
            for {
              mnode <- updateFut
            } yield {
              LOGGER.debug(s"$logPrefix Ok, nodeVsn => ${mnode.versionOpt.orNull}")
              val m = MLknNode(
                id                      = nodeId,
                name                    = mnode.guessDisplayNameOrIdOrQuestions,
                ntypeId                 = mnode.common.ntype.strId,
                isEnabled               = mnode.common.isEnabled,
                canChangeAvailability   = Some(true)
              )
              val bbuf = PickleUtil.pickle( m )
              Ok( ByteString(bbuf) )
            }
          }
        )

      }
    }
  }

}
