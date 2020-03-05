package util.mdr

import java.time.OffsetDateTime

import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.{EsModel, IMust}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{IMItem, MItem, MItems}
import io.suggest.mbill2.m.order.MOrderWithItems
import io.suggest.n2.edge._
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.streams.StreamsUtil
import io.suggest.sys.mdr.{MMdrResolution, MdrSearchArgs}
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import javax.inject.{Inject, Singleton}
import models.mctx.Context
import models.mdr.{MMdrNotifyCtx, MMdrNotifyMeta}
import models.mproj.ICommonDi
import models.req.ISioUser
import models.usr.MSuperUsers
import util.acl.IsNodeAdmin
import util.adn.NodesUtil
import util.billing.Bill2Util
import util.mail.IMailerWrapper
import views.html.sys1.mdr._mdrNeededEmailTpl
import OptionUtil.BoolOptOps
import akka.stream.scaladsl.{Keep, Sink}
import io.suggest.i18n.MsgCodes
import play.api.i18n.Lang

import scala.concurrent.Future
import scala.util.Failure

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.02.17 17:00
  * Description: Утиль для модерации.
  */
@Singleton
class MdrUtil @Inject() (
                          esModel           : EsModel,
                          mailerWrapper     : IMailerWrapper,
                          val mItems        : MItems,
                          mNodes            : MNodes,
                          bill2Util         : Bill2Util,
                          streamsUtil       : StreamsUtil,
                          nodesUtil         : NodesUtil,
                          mSuperUsers       : MSuperUsers,
                          isNodeAdmin       : IsNodeAdmin,
                          val mCommonDi     : ICommonDi,
                        )
  extends MacroLogsImpl
{

  import mCommonDi.{configuration, current, ec, mat, slick, messagesApi, langs}
  import slick.profile.api._
  import streamsUtil.Implicits._
  import esModel.api._


  /** Кол-во уровней погружения в поисках под-узлов в зависимости от для ситуации.
    *
    * @param isPerson Является ли корневой родительский узел - юзером?
    * @return Кол-во уровней, которые надо пройти в поисках под-узлов.
    */
  def maxLevelsDeepFor(isPerson: Boolean): Int = {
    var res = 3
    if (isPerson)
      res += 1
    res
  }


  /** Кого надо уведомить о необходимости заняться модерацией? */
  val MDR_NOTIFY_SU_EMAILS: Seq[String] = {
    val confKey = "mdr.notify.emails"
    val res = configuration.getOptional[Seq[String]](confKey)
      .filter(_.nonEmpty)
      .getOrElse {
        LOGGER.info(s"$confKey is undefined. Using all superusers as moderators.")
        current.injector
          .instanceOf[MSuperUsers]
          .SU_EMAILS
      }
    LOGGER.info(s"Moderators are: ${res.mkString(", ")}")
    res
  }


  /** SQL для поиска нуждающихся в биллинговой модерации карточек. */
  def awaitingPaidMdrItemsSql = {
    mItems
      .query
      .filter { i =>
        i.statusStr === MItemStatuses.AwaitingMdr.value
      }
  }


  /**
    * Выяснить у базы, требуется ли послать модераторам письмо о появлении нового платного объекта
    * в очереди на модерацию.
    * Надо вызывать перед непосредственной заливкой item'ов в базу.
    *
    * @return true/false если требуется или нет.
    */
  def mdrNotifyPrepareCtx(owi: MOrderWithItems): DBIOAction[MMdrNotifyCtx, NoStream, Effect.Read] = {
    // Уведомление юзеров по email: можно собрать rcrvId из данных ордера. Потом по ним определить, кого уведомлять.
    /*
    val notifyUserMdrRcvrIds = owi.mitems
      .iterator
      .flatMap(_.rcvrIdOpt)
      .toSet
    */

    for {
      // Отправка письма для супер-юзеров потребуется, если прямо сейчас нет ни одного item'а, ожидающего модерации.
      isAlreadyExistPaidMdrItems <- awaitingPaidMdrItemsSql.exists.result
    } yield {
      MMdrNotifyCtx(
        // Если уже существуют paid-mdr-items, то модерация после обработки не требуется:
        needSuNotify  = !isAlreadyExistPaidMdrItems,
        rcvrIds       = Set.empty //notifyUserMdrRcvrIds
      )
    }
  }


  /** Требуется ли сборка данных для mdr-notify? Да, если в контексте есть какие-либо данные. */
  def isMdrNotifyNeeded(ctx: MMdrNotifyCtx): Boolean = {
    ctx.needSuNotify
  }


  val USER_MDR_NOTIFY_ALSO_SU = configuration.getOptional[Boolean]("mdr.user.notify.su").getOrElseFalse

  /** Модель аккамулятора данных внутри обхаживалки графа узлов.
    *
    * @param seenNodeIds id узлов, которые уже были запрошены.ю
    * @param child2ownGraph Аккамулятор графа id связей узлов.
    *                    Для вычисления id родительских узлов в ссылках.
    * @param personsAcc Аккамулятор найденных юзеров.
    */
  private case class MdrNotifyAcc(
                                   seenNodeIds      : Set[String],
                                   child2ownGraph   : List[(String, Set[String])]   = List.empty,
                                   personsAcc       : List[MNode]                   = List.empty,
                                 ) {
    def withSeenNodeIds(seenNodeIds: Set[String]) = copy(seenNodeIds = seenNodeIds)
  }

  /** Отправить уведомление модератором о необходимости модерации чего-либо. */
  def sendMdrNotify(mdrCtx: MMdrNotifyCtx, tplArgs: MMdrNotifyMeta = MMdrNotifyMeta.empty)
                   (implicit ctx: Context): Future[_] = {

    val userMdrNotifyFut = if (mdrCtx.rcvrIds.nonEmpty) {
      lazy val logPrefix = s"sendMdrNotify(rcvrIds[${mdrCtx.rcvrIds.size}])#${System.currentTimeMillis()}:"

      // Организовать асинхронную работу в фоне на тему сбора данных по ресиверам и email'ам тех, каких юзеров надо уведомить.
      for {

        // Запуск гуляния по графу узлов в поиске юзеров:
        walkAcc2 <- {
          val goUpNodeTypes = Set[MNodeType](
            MNodeTypes.AdnNode,
            MNodeTypes.BleBeacon
          )

          mNodes.walkCache( MdrNotifyAcc(mdrCtx.rcvrIds), mdrCtx.rcvrIds ) { (acc0, mnode) =>
            // Сюда приходят узлы, которые скорее всего есть в seen - это нормально. По идее, это можно удалить, используя готовое значение из акка.
            val seenIds2 = if (mnode.id.exists(acc0.seenNodeIds.contains)) {
              acc0.seenNodeIds
            } else {
              acc0.seenNodeIds ++ mnode.id
            }

            if (mnode.common.ntype ==* MNodeTypes.Person) {
              // Если это person-node, то его запихнуть в акк.
              val acc2 = acc0.copy(
                personsAcc  = mnode :: acc0.personsAcc,
                seenNodeIds = seenIds2
              )
              (acc2, Set.empty)

            } else if (goUpNodeTypes contains mnode.common.ntype) {
              // Это узел, по которому можно подниматься вверх.
              val ownerIds = mnode.edges
                .withPredicateIterIds( MPredicates.OwnedBy )
                .filter { nodeId =>
                  val isAlreadySeen = acc0.seenNodeIds.contains(nodeId)
                  !isAlreadySeen &&
                  // Если уведомлять супер-юзеров запрещено, то проверить id узла по модели суперюзеров:
                  (USER_MDR_NOTIFY_ALSO_SU || !mSuperUsers.isSuperuserId(nodeId))
                }
                .toSet

              val acc2 = acc0.copy(
                seenNodeIds     = seenIds2 ++ ownerIds,
                child2ownGraph  = (mnode.id.get -> ownerIds) :: acc0.child2ownGraph
              )
              (acc2, ownerIds)

            } else {
              // Это какой-то неожиданный или неподходящий узел, пропустить.
              LOGGER.debug(s"$logPrefix Skipped node#${mnode.idOrNull} of unexpected type#${mnode.common.ntype}")
              val acc2 = acc0.withSeenNodeIds( seenIds2 )
              (acc2, Set.empty)
            }
          }
        }

        // Проверить, что есть какие-либо полезные результаты обхода:
        if {
          val r = walkAcc2.personsAcc.nonEmpty
          LOGGER.trace {
            if (!r) s"$logPrefix No persons found for rcvrs. Stop."
            else s"$logPrefix Walked ${walkAcc2.seenNodeIds.size} nodes, ~${walkAcc2.personsAcc.size} persons found, withSU?$USER_MDR_NOTIFY_ALSO_SU"
          }
          r
        }

        personsMap = walkAcc2
          .personsAcc
          .zipWithIdIter[String]
          .to(Map)

        // Запустить в фоне сборку email'ы юзеров, которые требуется обработать.
        personId2EmailsMapFut = {
          mNodes
            .dynSearchSource {
              new MNodeSearch {
                override val withIds = personsMap.keySet.toSeq
                override val nodeTypes = MNodeTypes.Person :: Nil
                override val outEdges: Seq[Criteria] = {
                  val cr = Criteria(
                    predicates = MPredicates.Ident.Email :: Nil
                  )
                  cr :: Nil
                }
              }
            }
            .map { mnode =>
              val personId = mnode.id.get
              val emails = mnode.edges
                .withPredicateIter( MPredicates.Ident.Email )
                .flatMap(_.nodeIds)
                .toSet
              personId -> emails
            }
            .toMat( Sink.collection[(String, Set[String]), Map[String, Set[String]]] )( Keep.right )
            .run()
        }


        // Пакетно готовим messages под языки найденных юзеров.
        langCode2MessagesMap = {
          val allLangCodes = personsMap
            .valuesIterator
            .flatMap(_.meta.basic.langs)
            .toSet
          val availLangs = langs.availables.toList
          val iter = for {
            langCode   <- allLangCodes.iterator
            lang       <- Lang.get( langCode )
          } yield {
            val msgs = messagesApi.preferred( lang :: availLangs )
            langCode -> msgs
          }
          iter.toMap
        }

        personId2EmailsMap <- personId2EmailsMapFut

        // Есть почта, пора рендерить шаблоны email-уведомлений для юзеров
        // Надо сгенерить ссылку на модерацию, для этого надо определить на каком узле рендерить.
        // Ищем первый прямой дочерний узел для каждого юзера: выворачиваем child2ownAcc наизнанку и делаем Map.
        own2ChildrenMap = {
          ( for {
              (childNodeId, ownerIds) <- walkAcc2.child2ownGraph.iterator
              ownerId <- ownerIds.iterator
            } yield {
              ownerId -> childNodeId
            })
            .toSeq
            .groupBy(_._1)
            .map { case (k, kvs) =>
              val vs = kvs.iterator
                .map(_._2)
                .toSet
              k -> vs
            }
        }

        // Рендер и отправка email-сообщений
        _ <- Future.traverse(personId2EmailsMap.to(Iterable)) { case (personId, emails) =>
          val personNodeOpt = personsMap.get( personId )

          // Разобраться с языком для рендера контекста.
          implicit val ctx2 = (for {
            personNode    <- personNodeOpt.iterator
            langCode      <- personNode.meta.basic.langs
            langMessages  <- langCode2MessagesMap.get( langCode )
          } yield {
            ctx.withMessages( langMessages )
          })
            .nextOption()
            .getOrElse {
              LOGGER.warn( s"$logPrefix i18n failed for person#$personId, available langs = [${langCode2MessagesMap.keysIterator.mkString(", ")}]" )
              ctx
            }

          // Вычислить id узла, на который генерить ссылку для юзера.
          val mdrNodeId = own2ChildrenMap
            .get( personId )
            .flatMap(_.headOption)
            .getOrElse {
              LOGGER.warn( s"$logPrefix No child node found for person#$personId, will render URL into person-node." )
              personId
            }
          val toMdrNodeId = Some(mdrNodeId)

          val tplArgs2 = tplArgs.copy(
            // Пусть ссылка будет в личный кабинет узла юзера.
            toMdrNodeId = toMdrNodeId,
            // Данные биллинга исходного юзера не нужны. orderId оставить, чтобы был идентификатор на всякий случай.
            txn         = None,
            personId    = None,
            paidTotal   = None,
          )
          LOGGER.trace(s"$logPrefix Will send message for person#$personId emails=${emails.mkString("|")}, mdrNodeId#$mdrNodeId")

          mailerWrapper
            .instance
            .setSubject(
              ctx2.messages( MsgCodes.`Moderation.needed` )
            )
            .setRecipients(emails.toSeq: _*)
            .setHtml(
              _mdrNeededEmailTpl(tplArgs2)(ctx2).body
            )
            .send()
            .andThen {
              case Failure(ex) =>
                LOGGER.warn(s"$logPrefix Unable to send email for person#$personId to $emails", ex)
              case _ => // do nothing
            }
        }

      } yield {
        LOGGER.debug(s"$logPrefix Done, processed ${personId2EmailsMap.size} persons with ${personId2EmailsMap.valuesIterator.flatten.size} emails.")
      }
    } else {
      Future.successful( None )
    }

    // Если требуется уведомлять супер-юзеров, то отправить su email:
    val suMdrNotifyFut = if (mdrCtx.needSuNotify) {
      Future {
        mailerWrapper
          .instance
          .setSubject("Требуется модерация")
          .setRecipients(MDR_NOTIFY_SU_EMAILS: _*)
          .setHtml(_mdrNeededEmailTpl(tplArgs).body)
          .send()
      }
    } else {
      Future.successful(None)
    }

    // Объеденить оба процесса.
    userMdrNotifyFut
      .flatMap(_ => suMdrNotifyFut)
  }


  def someNow = Some(OffsetDateTime.now)

  /** Сборка инфы эджа модерации.
    *
    * @param reasonOpt None - всё ок, модерация одобрена
    *                  Some(reason) - бан.
    * @return Инстанс MEdgeInfo.
    */
  def mdrEdgeInfo(reasonOpt: Option[String]): MEdgeInfo = {
    MEdgeInfo(
      dateNi    = someNow,
      textNi = reasonOpt,
      flag      = Some(reasonOpt.isEmpty)
    )
  }

  /** Сборка эджа текущего модератора с указанной инфой по модерации. */
  def mdrEdge(mdrUser: ISioUser, einfo: MEdgeInfo): MEdge = {
    MEdge(
      predicate = MPredicates.ModeratedBy,
      nodeIds   = mdrUser.personIdOpt.toSet,
      info      = einfo
    )
  }



  type Q_t = Query[mItems.MItemsTable, MItem, Seq]

  def onlyAwaitingMdr(q: Q_t): Q_t = {
    q.filter( _.statusStr === MItemStatuses.AwaitingMdr.value)
  }

  /** Общий код сборки всех SQL queries для сборки items модерации карточки. */
  def itemsQuery(nodeId: String): Q_t = {
    mItems.query
      .filter(_.nodeId === nodeId)
  }

  def itemsQueryAwaiting(nodeId: String): Q_t = {
    onlyAwaitingMdr(
      itemsQuery(nodeId)
    )
  }


  /** Логика поштучной обработки item'ов. */
  def _processOneItem[Res_t <: IMItem](dbAction: DBIOAction[Res_t, NoStream, _]): Future[Res_t] = {
    // Запуск обновления MItems.
    slick.db.run {
      dbAction.transactionally
    }
  }



  /** Результат вызова _processItemsForAd(). */
  sealed case class ProcessItemsRes(itemIds: Seq[Gid_t], successMask: Seq[Boolean], itemsCount: Int)

  /** Логика массовой обработки item'ов. */
  def _processItemsFor[Res_t <: IMItem](q: Q_t)
                                       (f: Gid_t => DBIOAction[Res_t, NoStream, _]): Future[ProcessItemsRes] = {
    // TODO Opt Тут можно db.stream применять
    val itemIdsFut = slick.db.run {
      q.map(_.id)
        .result
    }

    lazy val logPrefix = s"_processItemsFor()#${System.currentTimeMillis}:"
    LOGGER.trace(s"$logPrefix Bulk approving items...")

    for {
      itemIds <- itemIdsFut
      saveFut = Future.traverse(itemIds) { itemId =>
        _processOneItem(f(itemId))
          // Следует мягко разруливать ситуации, когда несколько модераторов одновременно аппрувят item'ы одновременно.
          .map { _ => true }
          .recover {
            // Вероятно, race conditions двух модераторов.
            case _: NoSuchElementException =>
              LOGGER.warn(s"$logPrefix Possibly conficting mdr MItem UPDATE. Suppressed.")
              false
            // Пока вырублено подавление любых ошибок, т.к. скрывают реальные проблемы.
            //case ex: Throwable =>
            //  LOGGER.error(s"$logPrefix Unknown error occured while approving item $itemId", ex)
            //  true
          }
      }
      itemsCount  = itemIds.size
      saveRes     <- saveFut

    } yield {
      ProcessItemsRes(itemIds, saveRes, itemsCount)
    }
  }


  /** Выполнить команду модератора.
    *
    * @param mdrRes Резолюция модератора.
    * @param mnode Узел, упомянутый в mdrRes.nodeId.
    * @param mdrUser Юзер-модератор.
    * @param rcvrIds Список допустимых id ресиверов.
    * @return Фьючерс готовности.
    */
  def processMdrResolution(mdrRes: MMdrResolution, mnode: MNode, mdrUser: ISioUser,
                           rcvrIds: Set[String]): Future[(MNode, Option[ProcessItemsRes])] = {

    lazy val logPrefix = s"processMdrResolution(${mdrRes.isApprove} node#${mnode.idOrNull} mdrUser#${mdrUser.personIdOpt.orNull})#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix res=$mdrRes")

    val nfo = mdrRes.info
    val isRefused = !mdrRes.isApprove
    val infoApplyAll = nfo.isEmpty

    // free-модерация, чтобы по-скорее запустить обновление mnode, максимально до активации adv-билдеров.

    // Для user-mdr: nfo.direct* должны быть уже отфильтрованы/отработаны на уровне ACL.
    // Этот метод не занимается проверкой прав доступа, но из-за isSuper всё же косвенно предотвращает нецелевое использование.

    var mnode2Fut: Future[MNode] = if ((infoApplyAll && mdrUser.isSuper) || nfo.directSelfAll || nfo.directSelfId.nonEmpty) {
      LOGGER.trace(s"$logPrefix Will process mdr-edges for free mdr, applyAll?$infoApplyAll")

      // Тут два варианта: полный аппрув выставлением mdr-эджа, или отказ в размещении на узле/узлах.
      mNodes.tryUpdate(mnode)(
        // выполнить обновление эджей узла.
        MNode.edges.modify { edges0 =>
          // Если isApproved, то просто выставить эдж
          // Если !isApproved и directSelfAll, то удалить все rcvr.self-эджи
          // Если !isApproved и directSelfId.nonEmpty, то удалить только конкретный rcvr-self-эдж.
          val mdrEdge2 = mdrEdge(
            mdrUser,
            mdrEdgeInfo( mdrRes.reason )
          )

          // Какие предикаты вычищать полностью?
          var rmPredicates: List[MPredicate] =
            MPredicates.ModeratedBy :: Nil

          // Удалить Rcvr.self-эджи при отрицательном вердикте.
          if (isRefused && mdrRes.info.directSelfAll) {
            LOGGER.trace(s"$logPrefix rm * self-rcvrs")
            rmPredicates ::= MPredicates.Receiver.Self
          }

          var edgesIter2: Iterator[MEdge] = edges0
            .withoutPredicateIter( rmPredicates: _* )
            .++( mdrEdge2 :: Nil )

          // Выпилить rcvrs.self с указанным node-id:
          for {
            selfId <- mdrRes.info.directSelfId
            // TODO !isRefused, т.к. откат забана не реализован тут.
            if isRefused && !mdrRes.info.directSelfAll
          } {
            LOGGER.trace(s"$logPrefix rm selfId=$selfId")
            edgesIter2 = edgesIter2
              .flatMap { medge =>
                if ((medge.predicate ==* MPredicates.Receiver.Self) &&
                    (medge.nodeIds contains selfId)) {
                  if (medge.nodeIds.size ==* 1) {
                    Nil
                  } else {
                    val medge2 = MEdge.nodeIds.modify(_ - selfId)(medge)
                    medge2 :: Nil
                  }
                } else {
                  medge :: Nil
                }
              }
          }

          (MNodeEdges.out set MNodeEdges.edgesToMap1(edgesIter2))( edges0 )
        }
      )
    } else {
      // Нет элементов для набега на биллинг.
      Future.successful( mnode )
    }

    val infoHasBillCrs = nfo.itemId.nonEmpty || nfo.itemType.nonEmpty
    // Если заданы критерии item'ов, то ковыряем биллинг:
    for {
      // Дождаться окончания freeMdr-действий, чтобы не было конфликтов при перезаписи узла в bill-части:
      mnode2 <- mnode2Fut

      billResOpt <- OptionUtil.maybeFut( infoHasBillCrs || infoApplyAll ) {

        var q = itemsQueryAwaiting( mdrRes.nodeId )

        // TODO Нужна поддержка отката изменений: чтобы вместо бана можно было поверх наложить обратное решение.
        if (infoHasBillCrs) {
          q = q.filter { i =>
            // Накопить аргументы для WHERE:
            var acc = List.empty[Rep[Boolean]]
            for (itemId <- nfo.itemId) {
              LOGGER.trace(s"$logPrefix only itemId=$itemId")
              acc ::= (i.id === itemId)
            }
            for (itemType <- nfo.itemType) {
              LOGGER.trace(s"$logPrefix only itemType=$itemType")
              acc ::= (i.iTypeStr === itemType.value)
            }
            acc.reduce(_ || _)
          }
        } else {
          LOGGER.trace(s"$logPrefix Will process ALL billing data.")
        }

        // Модерация в рамках ресивера: выставить ресивер в sql-запрос.
        if (rcvrIds.nonEmpty) {
          LOGGER.trace(s"$logPrefix Limited only to rcvrIds: ##[${rcvrIds.mkString(", ")}]")
          q = q.filter(_.rcvrIdOpt inSet rcvrIds)
        }

        for {
          // Запустить биллинговую часть: аппрув item'а или отказ в модерации item'а.
          billProcessRes <- {
            LOGGER.trace(s"$logPrefix Will bill-mdr, ${rcvrIds.size}-allowedRcvs, approve?${mdrRes.isApprove}...")
            _processItemsFor {
              if (rcvrIds.isEmpty) q
              else q.filter { i =>
                i.rcvrIdOpt inSet rcvrIds
              }
            } {
              if (mdrRes.isApprove)
                bill2Util.approveItem
              else
                bill2Util.refuseItem(_, mdrRes.reason)
            }
          }

        } yield {
          LOGGER.trace(s"$logPrefix Bill mdr done, res => $billProcessRes")
          Some( billProcessRes )
        }

      }

    } yield {
      LOGGER.trace(s"$logPrefix Done all changes")
      mnode2 -> billResOpt
    }
  }


  def findPaidNodeIds4MdrQ(hideNodeIdOpt    : Option[String]        = None,
                           rcvrIds          : Iterable[String]      = Nil): Query[Rep[String], String, Seq] = {
    var q = awaitingPaidMdrItemsSql

    // Пропуск произвольного узла
    for (hideNodeId <- hideNodeIdOpt) {
      q = q.filter { i =>
        i.nodeId =!= hideNodeId
      }
    }

    // Поиск только на указанных ресиверах:
    if (rcvrIds.nonEmpty) {
      q = q.filter { i =>
        i.rcvrIdOpt inSet rcvrIds
      }
    }

    q .map(_.nodeId)
      .distinct
  }


  /** SQL для экшена поиска id карточек, нуждающихся в модерации. */
  def getFirstIn(args: MdrSearchArgs, q: Query[Rep[String], String, Seq], limit: Int): DBIOAction[Seq[String], Streaming[String], Effect.Read] = {
    q .drop( args.offset )
      .take( limit )
      .result
  }


  /** Аргументы для поиска узлов (карточек), требующих бесплатной модерации. */
  def freeMdrNodeSearchArgs(args: MdrSearchArgs, rcvrIds: Seq[String], limit1: Int): MNodeSearch = {
    new MNodeSearch {

      /** Интересуют только карточки. */
      override def nodeTypes =
        MNodeTypes.Ad :: Nil

      override def offset  = args.offset
      override def limit   = limit1

      override def outEdges: Seq[Criteria] = {
        val must = IMust.MUST

        // Собираем self-receiver predicate, поиск бесплатных размещений начинается с этого
        val srp = Criteria(
          predicates  = MPredicates.Receiver.Self :: Nil,
          must        = must,
          nodeIds     = rcvrIds
        )

        // Любое состояние эджа модерации является значимым и определяет результат.
        val isAllowedCr = Criteria(
          predicates  = MPredicates.ModeratedBy :: Nil,
          flag        = args.isAllowed,
          must        = Some( args.isAllowed.isDefined )
        )

        List[Criteria](
          srp,
          isAllowedCr
        )
      }

      override def withoutIds = args.hideAdIdOpt.toSeq
    }
  }


  /** Автоматически загасить любые проблемы с модерацией указанного узла.
    *
    * @param nodeId id узла, который не может быть отмодерирован.
    * @param reasonOpt Причина.
    * @return Фьючерс.
    */
  def fixNode(nodeId: String, reasonOpt: Option[String]): Future[_] = {
    // Узел отсутствует, но должны быть какие-то item'ы для модерации.
    lazy val logPrefix = s"fixNode($nodeId)#${System.currentTimeMillis()}:"
    LOGGER.info(s"$logPrefix Starting, reason = ${reasonOpt.orNull}")

    slick.db.stream {
      mItems.query
        .filter { i =>
          (i.nodeId === nodeId) &&
          (i.statusStr === MItemStatuses.AwaitingMdr.value)
        }
        .map(_.id)
        // distinct не требуется, т.к. primary key.
        .result
        .forPgStreaming(40)
    }
      .toSource
      .mapAsyncUnordered(4) { itemId =>
        slick.db.run {
          bill2Util.refuseItem(itemId, reasonOpt)
        }
      }
      .runForeach { refuseRes =>
        LOGGER.trace(s"$logPrefix item#${refuseRes.mitem.id.orNull} => $refuseRes")
      }
  }

}
