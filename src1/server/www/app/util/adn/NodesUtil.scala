package util.adn

import java.time.OffsetDateTime

import akka.stream.scaladsl.Sink
import javax.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.adn.MAdnRights
import io.suggest.common.coll.Lists.Implicits._
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModel
import io.suggest.ext.svc.MExtServices
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.extra.{MAdnExtra, MNodeExtras}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.url.MHostInfo
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImpl
import models.adv.{MExtTarget, MExtTargets}
import models.im.MImgT
import models.madn.{AdnShownTypes, NodeDfltColors}
import models.mext.MExtServicesJvm
import models.mproj.ICommonDi
import models.mwc.MWelcomeRenderArgs
import play.api.i18n.{Lang, Langs, Messages}
import play.api.inject.Injector
import play.api.mvc.Call
import util.img.DynImgUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 9:53
 * Description: Утиль для работы с нодами. Появилось, когда понадобилась функция создания пользовательского узла
 * в нескольких контроллерах.
 *
 * Обязательно singleton, т.к. инициализация 404-узлов идёт здесь.
 */
@Singleton
final class NodesUtil @Inject() (
                                  esModel                 : EsModel,
                                  mNodes                  : MNodes,
                                  mExtTargets             : MExtTargets,
                                  dynImgUtil              : DynImgUtil,
                                  mCommonDi               : ICommonDi
                                )
  extends MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._
  import esModel.api._

  // Для новосозданного узла надо создавать новые карточки, испортируя их из указанного узла в указанном кол-ве.
  /** id узла, который содержит дефолтовые карточки. Задается явно в конфиге. */
  def ADN_IDS_INIT_ADS_SOURCE = Nil

  /** Кол-во карточек для импорта из дефолтового узла. */
  def INIT_ADS_COUNT = 1


  /** Куда отправлять юзера, когда тот создал новый узел? */
  def userNodeCreatedRedirect(adnId: String): Call = {
    routes.LkAdnEdit.editNodePage( adnId )
  }


  /** Собрать критерии поиска узлов, прямо принадлежащих текущему юзеру. */
  def personNodesSearch(personId: String, limit1: Int = 200, withoutIds1: Seq[String] = Nil): MNodeSearch = {
    new MNodeSearch {
      override def outEdges = Seq(
        Criteria(
          nodeIds     = personId :: Nil,
          predicates  = MPredicates.OwnedBy :: Nil
        )
      )
      override def limit      = limit1
      override def withoutIds = withoutIds1
    }
  }


  /**
    * Создать новый инстанс узла для юзера без сохранения узла в хранилище.
    *
    * @param nameOpt название узла.
    * @param personIdOpt id юзера-владельца.
    * @return Экземпляр узла без id.
    */
  def userNodeInstance(nameOpt: Option[String], personIdOpt: Option[String]): MNode = {
    MNode(
      common = MNodeCommon(
        ntype = MNodeTypes.AdnNode,
        isDependent = true
      ),
      meta = MMeta(
        basic = MBasicMeta(
          nameOpt = nameOpt
        ),
        colors = NodeDfltColors.getOneRandom().adnColors
      ),
      extras = MNodeExtras(
        adn =  Some(MAdnExtra(
          rights          = Set(MAdnRights.PRODUCER, MAdnRights.RECEIVER),
          isUser          = true,
          shownTypeIdOpt  = Some(AdnShownTypes.SHOP.value),
          testNode        = false,
        ))
      ),
      edges = MNodeEdges(
        out = {
          personIdOpt.fold[Seq[MEdge]] (Nil) { personId =>
            val personIdSet = Set(personId)
            val ownedByEdge = MEdge(
              predicate = MPredicates.OwnedBy,
              nodeIds   = personIdSet,
              // Выставлять создателю порядок, чтобы именно он получал деньги, даже при появлении ещё одного владельца узла.
              order     = Some(0)
            )
            val createdByEdge = MEdge(
              predicate = MPredicates.CreatedBy,
              nodeIds   = personIdSet
            )
            MNodeEdges.edgesToMap(ownedByEdge, createdByEdge)
          }
        }
      )
    )
  }


  // TODO Убрать отсюда поддержку adv-ext, вынести в соотв.утиль.
  /**
    * Создать дефолтовые таргеты для размещения в соц.сетях.
    *
    * @param adnId id узла.
    * @return Фьючерс для синхронизации.
    */
  def createExtDfltTargets(adnId: String)(implicit messages: Messages): Future[_] = {
    val targetsIter = for {
      svc <- MExtServices.values.iterator
      if svc.hasAdvExt
      svcJvm = MExtServicesJvm.forService( svc )
      url <- svcJvm.advExt.dfltTargetUrl
    } yield {
      MExtTarget(
        url     = url,
        adnId   = adnId,
        service = svc,
        name    = Some( messages(svc.iAtServiceI18N) )
      )
    }
    Future.traverse(targetsIter)(mExtTargets.save)
  }

  /**
    * Создание нового узла для юзера. Узел должен быть готов к финансовой работе.
    *
    * @param name Название узла.
    * @param personId id юзера-владельца.
    * @return Фьючерс с готовым инстансом нового существующего узла.
    */
  def createUserNode(name: String, personId: String)(implicit messages: Messages): Future[MNode] = {
    val inst = userNodeInstance(
      nameOpt     = Some(name),
      personIdOpt = Some(personId)
    )
    for {
      nodeId        <- mNodes.save(inst)
      // TODO Нужно инсталлить jd-карточки, а не этот старый хлам
      //madsCreateFut = installDfltMads(nodeId)
      // TODO Выключено, т.к. adv-ext не пашет, и надо его перепиливать капитально.
      //_             <- createExtDfltTargets(nodeId)
      //_             <- madsCreateFut
    } yield {
      MNode.id
        .set( Some(nodeId) )(inst)
    }
  }


  /** Установить дефолтовые карточки. */
  def installDfltMads(adnId: String, count: Int = INIT_ADS_COUNT)(implicit messages: Messages): Future[Seq[String]] = {
    lazy val logPrefix = s"installDfltMads($adnId):"
    (Future successful ADN_IDS_INIT_ADS_SOURCE)
      // Если нет продьюсеров, значит функция отключена. Это будет перехвачено в recover()
      .filter { _.nonEmpty }

      // Собрать id карточек, относящиеся к заданным узлам-источникам.
      .flatMap { prodIds =>
        val _limit = Math.max(50, count * 2)
        val dsa0 = new MNodeSearch {
          override val nodeTypes = MNodeTypes.Ad :: Nil
          override val outEdges: Seq[Criteria] = {
            val cr = Criteria(
              nodeIds     = prodIds,
              predicates  = MPredicates.OwnedBy :: Nil
            )
            cr :: Nil
          }
          override def limit  = _limit
          override def offset = 0
        }
        mNodes.dynSearchIds( dsa0 )
      }

      // Случайно выбрать из списка id карточек только указанное кол-во карточек.
      .flatMap { madIdsRes =>
        val count = madIdsRes.length
        val rnd = new Random()

        val madIds2 = for {
          _ <- 0 until Math.min(count, count)
        } yield {
          madIdsRes( rnd.nextInt(count) )
        }

        mNodes.multiGet(madIds2)
      }

      // Обновить карточки
      .flatMap { mads0 =>
        trace(s"$logPrefix Will install ${mads0.size} ads: [${mads0.iterator.flatMap(_.id).mkString(", ")}]")

        Future.traverse(mads0) { mad0 =>
          // Создать новую карточку на базе текущей.
          val mad1 = mad0.copy(
            id = None,
            versionOpt = None,
            meta = mad0.meta.copy(
              basic = mad0.meta.basic.copy(
                dateCreated = OffsetDateTime.now(),
                dateEdited  = None
              )
            ),
            edges = MNodeEdges(
              out = {
                val pp = MPredicates.OwnedBy
                val rp = MPredicates.Receiver.Self
                val nodeIdSet = Set(adnId)
                val prodE = MEdge(
                  predicate = pp,
                  nodeIds   = nodeIdSet
                )
                val selfRcvrE = MEdge(
                  predicate = rp,
                  nodeIds   = nodeIdSet
                )
                MNodeEdges.edgesToMap1 {
                  mad0.edges
                    .withoutPredicateIter(pp, rp)
                    .++( prodE :: selfRcvrE :: Nil )
                }
              }
            )
          )

          // Запустить сохранение сгенеренной карточки.
          mNodes.save(mad1)
        }
      }
      // Если не было adnId узлов-источников, то
      .recover { case _: NoSuchElementException =>
        LOGGER.warn(logPrefix + " Node default ads installer is disabled!")
        LazyList.empty
      }
  }


  /** Подготовка MediaHostsMap для рендера медиа-файлов узла с использование dist-cdn.
    * Изначально код зародился в выдаче, но вынесен для возможности использования на других страницах узла.
    *
    * @param logoImgOpt Инфа по картинке-логотипу.
    *                   см. [[util.img.LogoUtil.getLogoOfNode()]] и [[util.img.LogoUtil.getLogoOpt4scr()]].
    *                   или [[util.img.FitImgMaker]].
    * @param welcomeOpt Инфа по welcome-картинкам.
    *                   см. [[util.img.WelcomeUtil.getWelcomeRenderArgs()]]
    * @param gallery Инфа по внутренней галлерее картинок узла.
    *                см. [[util.img.GalleryUtil.galleryImgs()]]
    * @return Выхлоп [[util.cdn.CdnUtil.mediasHosts()]].
    */
  def nodeMediaHostsMap(logoImgOpt    : Iterable[MImgT]                 = Nil,
                        welcomeOpt    : Iterable[MWelcomeRenderArgs]    = Nil,
                        gallery       : List[MImgT]                 = Nil
                       ): Future[Map[String, Seq[MHostInfo]]] = {
    var allImgsAcc = gallery

    allImgsAcc = (for {
      wc    <- welcomeOpt
      imgWh <- wc.allImgsWithWhInfoIter
    } yield {
      imgWh.mimg
    })
      .prependRevTo( allImgsAcc )

    allImgsAcc = logoImgOpt.prependRevTo(allImgsAcc)

    // Запустить сборку MediaHostsMap.
    dynImgUtil.mkMediaHostsMap( allImgsAcc )
  }


  /** Сбор всех дочерних id в одну кучу по OwnedBy предикату.
    *
    * @param parentNodeIds Интересующие id родительских узлов.
    * @param maxLevels Максимальное кол-во шагов.
    * @return Фьючерс с множеством дочерних узлов + parentNodeIds.
    */
  def collectChildIds(parentNodeIds: Set[String], maxLevels: Int = 3, perStepLimit: Int = 100): Future[Set[String]] = {
    val _predicates = MPredicates.OwnedBy :: Nil

    // Асинхронная рекурсия поиска id-узлов с погружением на под-уровни.
    def __fold(parentNodeIdsCurrent: Set[String], acc0Fut: Future[Set[String]], counter: Int): Future[Set[String]] = {
      if (counter >= maxLevels || parentNodeIdsCurrent.isEmpty) {
        acc0Fut

      } else {
        mNodes
          .dynSearchIds {
            val crs = Criteria(
              nodeIds     = parentNodeIdsCurrent.toSeq,
              predicates  = _predicates
            ) :: Nil
            new MNodeSearch {
              override def outEdges = crs
              override def limit = perStepLimit
            }
          }
          .flatMap { idsResp =>
            val idsSet = idsResp.toSet
            acc0Fut.flatMap { acc0 =>
              val acc2Fut = Future( acc0 ++ idsSet )

              // Чтобы не было циклов графа, надо выкинуть id уже пройденных узлов. В норме - тут просто пересборка idsSet.
              val alreadyUsedIds = idsSet intersect acc0

              // Акк уже готов, но нужно собрать список родительских узлов для следующей итерации:
              val idsSet2 =
                if (alreadyUsedIds.isEmpty) idsSet
                else idsSet -- alreadyUsedIds

              // И перейти на следующую итерацию:
              __fold(idsSet2, acc2Fut, counter + 1)
            }
          }
      }
    }

    // Запуск рекурсии.
    __fold(
      parentNodeIdsCurrent  = parentNodeIds,
      acc0Fut               = Future.successful( parentNodeIds ),
      counter               = 0
    )
  }


  /** Собрать id родительских узлов по отношению к указанным дочерним узлам. */
  def directOwnerIdsOf(childrenIds: Iterable[String]): Future[Set[String]] = {
    // Чтобы узнать родительские узлы, надо прочитать текущие узлы:
    mNodes
      .multiGetCacheSrc( childrenIds )
      .mapConcat { mnode =>
        mnode.edges
          .withPredicateIterIds( MPredicates.OwnedBy )
          .toSet
      }
      .runWith( Sink.collection[String, Set[String]] )
  }


  /** Вычислить personName для tplArgs писем-уведомлений о модерации.
    *
    * @param personNodeOptFut request.personNodeOpt
    * @param userEmailsFutOpt Почтовые адреса юзера, если уже обрабатываются снаружи.
    * @return Фьючерс с найденным именем юзера.
    */
  def getPersonName(personNodeOptFut: Future[Option[MNode]],
                    userEmailsFutOpt: => Option[Future[Seq[String]]] = None): Future[Option[String]] = {
    personNodeOptFut.flatMap { personNodeOpt =>
      val nodeNameOpt = personNodeOpt.flatMap(_.guessDisplayName)
      FutureUtil.opt2futureOpt( nodeNameOpt ) {
        val userEmailsFut = userEmailsFutOpt.getOrElse {
          val r = personNodeOpt
            .iterator
            .flatMap(_.edges.withPredicateIterIds( MPredicates.Ident.Email ))
            .toSeq
          Future.successful(r)
        }
        for (usrEmails <- userEmailsFut) yield {
          usrEmails.headOption
        }
      }
    }
  }


  /** Префикс id узла, содержащего 404-карточки. */
  def NO_ADS_FOUND_404_NODE_ID_PREFIX = ".___404___."
  /** Сборка id узла, содержащего 404-карточки для указанного языка. */
  def noAdsFound404NodeId(lang: Lang): String =
    NO_ADS_FOUND_404_NODE_ID_PREFIX + lang.code

  /** Проверить, является ли id данного узла служебным, относящимся к 404-узлу.
    *
    * @param nodeId id узла.
    * @return true, если данный id узла относится к 404-узлу.
    */
  def is404Node(nodeId: String): Boolean =
    nodeId startsWith NO_ADS_FOUND_404_NODE_ID_PREFIX

  /** Макс. кол-во карточек 404 за порцию плитки в выдаче. */
  def MAX_404_ADS_ONCE = 10

  /** Запуск инициализации пустых 404-узлов.
    * Вызывается вручную.
    *
    * @return Фьючерс.
    */
  def init404nodes(): Future[_] = {
    lazy val logPrefix = s"init404nodes()#${System.currentTimeMillis()}:"

    val langs = current.injector.instanceOf[Langs]
    val nodesFut = Future.traverse( langs.availables ) { lang =>
      val nodeId = noAdsFound404NodeId( lang )
      for {
        mnodeOpt <- mNodes.getByIdCache( nodeId )
        resNode <- FutureUtil.opt2future( mnodeOpt ) {
          LOGGER.info(s"$logPrefix Will initialize 404-node#$nodeId for lang#${lang.code}")
          // Узел максимально прост и примитивен, т.к. не должен нести какой-либо доп.нагрузки: всё в карточках.
          val mnode = MNode(
            id = Some(nodeId),
            common = MNodeCommon(
              ntype       = MNodeTypes.AdnNode,
              isDependent = false,
              isEnabled   = true,
            ),
            extras = MNodeExtras(
              adn = Some(MAdnExtra(
                rights   = Set( MAdnRights.PRODUCER, MAdnRights.RECEIVER ),
                isUser   = false,
                testNode = true,
              ))
            ),
            meta = MMeta(
              basic = MBasicMeta(
                hiddenDescr = Some(s"Узел-контейнер 404-карточек для языка ${lang.code}.")
              )
            )
          )
          for (_ <- mNodes.save( mnode )) yield
            mnode
        }
      } yield {
        resNode
      }
    }

    // Залоггировать id узлов:
    if (LOGGER.underlying.isTraceEnabled)
      for (nodes <- nodesFut)
        LOGGER.trace(s"$logPrefix ${nodes.length} nodes: [${nodes.iterator.flatMap(_.id).mkString(", ")}]")

    nodesFut
  }


  // Конструктор - зарегать все 404-узлы.
  // По идее, это безопасно, т.к. id узлов стабильные постоянные.
  init404nodes()

}


/** Интерфейс для DI-поля, содержащего инжектируемый экземпляр [[util.adn.NodesUtil]]. */
trait INodesUtil {
  def nodesUtil: NodesUtil
}


trait NodesUtilJmxMBean {
  def init404nodes(): String
}
final class NodesUtilJmx @Inject() (
                                     injector: Injector,
                                   )
  extends NodesUtilJmxMBean
  with JmxBase
{
  import JmxBase._

  override def _jmxType = JmxBase.Types.UTIL

  private def nodesUtil = injector.instanceOf[NodesUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  override def init404nodes(): String = {
    val fut = for {
      _ <- nodesUtil.init404nodes()
    } yield {
      "done, see logs for more info"
    }
    awaitString(fut)
  }

}
