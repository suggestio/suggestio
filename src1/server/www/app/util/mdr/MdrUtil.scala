package util.mdr

import java.time.OffsetDateTime

import akka.stream.scaladsl.Sink
import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.IMust
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{IMItem, MItem, MItems}
import io.suggest.model.n2.edge._
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
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
import util.billing.Bill2Util
import util.mail.IMailerWrapper
import views.html.sys1.mdr._mdrNeededEmailTpl

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.02.17 17:00
  * Description: Утиль для модерации.
  */
@Singleton
class MdrUtil @Inject() (
                          mailerWrapper     : IMailerWrapper,
                          val mItems        : MItems,
                          mNodes            : MNodes,
                          bill2Util         : Bill2Util,
                          streamsUtil       : StreamsUtil,
                          isNodeAdmin       : IsNodeAdmin,
                          val mCommonDi     : ICommonDi,
                        )
  extends MacroLogsImpl
{

  import mCommonDi.{configuration, current, ec, mat, slick}
  import slick.profile.api._
  import streamsUtil.Implicits._


  /** Кого надо уведомить о необходимости заняться модерацией? */
  val MDR_NOTIFY_SU_EMAILS: Seq[String] = {
    val confKey = "mdr.notify.emails"
    val res = configuration.getOptional[Seq[String]](confKey)
      .filter(_.nonEmpty)
      .fold {
        LOGGER.info(s"$confKey is undefined. Using all superusers as moderators.")
        current.injector
          .instanceOf[MSuperUsers]
          .SU_EMAILS
      } { notifyEmails =>
        LOGGER.trace(s"Successfully aquired moderators emails from $confKey")
        notifyEmails
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
  def mdrNotifyPrepareCtx(): DBIOAction[MMdrNotifyCtx, NoStream, Effect.Read] = {
    // Отправка письма потребуется, если прямо сейчас нет ни одного item'а, ожидающего модерации.
    for {
      isAlreadyExistPaidMdrItems <- awaitingPaidMdrItemsSql.exists.result
    } yield {
      MMdrNotifyCtx(
        // Если уже существуют paid-mdr-items, то модерация после обработки не требуется:
        needSuNotify = !isAlreadyExistPaidMdrItems
      )
    }
  }


  /** Требуется ли сборка данных для mdr-notify? Да, если в контексте есть какие-либо данные. */
  def isMdrNotifyNeeded(ctx: MMdrNotifyCtx): Boolean = {
    ctx.needSuNotify
  }


  /** Отправить уведомление модератором о необходимости модерации чего-либо. */
  def sendMdrNotify(mdrCtx: MMdrNotifyCtx, tplArgs: MMdrNotifyMeta = MMdrNotifyMeta.empty)
                   (implicit ctx: Context): Future[_] = {
    // Исполняем всё в фоновом потоке, чтобы
    Future {
      // Если требуется уведомлять супер-юзеров, то отправить su email:
      if (mdrCtx.needSuNotify) {
        mailerWrapper
          .instance
          .setSubject("Требуется модерация")
          .setRecipients(MDR_NOTIFY_SU_EMAILS: _*)
          .setHtml(_mdrNeededEmailTpl(tplArgs).body)
          .send()
      }

    }
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
      commentNi = reasonOpt,
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

    lazy val logPrefix = s"_processItemsForAd()#${System.currentTimeMillis}:"
    LOGGER.trace(s"$logPrefix Bulk approve items, $f")

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
    * @param mdrUser
    * @return Фьючерс готовности.
    */
  def processMdrResolution(mdrRes: MMdrResolution, mnode: MNode, mdrUser: ISioUser): Future[(MNode, Option[ProcessItemsRes])] = {
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
      mNodes.tryUpdate(mnode) { mnode0 =>
        // выполнить обновление эджей узла.
        mnode0.withEdges(
          mnode0.edges.withOut {
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

            var edgesIter2: Iterator[MEdge] = mnode0.edges
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
                      val medge2 = medge.withNodeIds(medge.nodeIds - selfId)
                      medge2 :: Nil
                    }
                  } else {
                    medge :: Nil
                  }
                }
            }

            MNodeEdges.edgesToMap1( edgesIter2 )
          }
        )
      }
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

        val mdrRcvrIdOpt = mdrRes.conf.rcvrIdOpt

        // Модерация в рамках ресивера: выставить ресивер в sql-запрос.
        for (mdrRcvrId <- mdrRcvrIdOpt) {
          LOGGER.trace(s"$logPrefix Limited only to rcvrId#$mdrRcvrId")
          q = q.filter(_.rcvrIdOpt === mdrRcvrId)
        }

        for {
          // Запустить подготовку списка допустимых ресиверов, если !su-модерация:
          allowedRcvrIds <- {
            if ( !mdrUser.isSuper ) {
              // Когда юзер - не супер-пользователь,
              // то нужно ограничить запрос только множеством ресиверов, на которые у юзера есть права.
              // Для этого запускаем текущую sql query с возвратом distinct rcvr_id.
              LOGGER.trace(s"$logPrefix Will collect allowed rcvrs for user#${mdrUser.personIdOpt.orNull}...")
              def __strOptToList(nodeIdOpt: Option[String]) = nodeIdOpt.toList

              slick.db
                .stream {
                  q .map(_.rcvrIdOpt)
                    .distinct
                    .result
                }
                .toSource
                .mapConcat( __strOptToList )
                // т.к. distinct на уровне СУБД, то дедубликация rcvr_id не нужна. Переходим к поштучной обработке:
                .mapAsyncUnordered(10) { rcvrId =>
                  // Запустить проверку прав доступа на данный ресивер:
                  for {
                    nodeChainOpt <- isNodeAdmin.isNodeAdminUpFrom(rcvrId, mdrUser)
                  } yield {
                    val rcvrAllowed = nodeChainOpt.nonEmpty
                    LOGGER.trace(s"$logPrefix rcvrId=$rcvrId allowed?$rcvrAllowed")
                    OptionUtil.maybe( rcvrAllowed )( rcvrId )
                  }
                }
                .mapConcat( __strOptToList )
                // Собрать финальное множество допущенных nodeid:
                .runWith( Sink.collection[String, Set[String]] )
                // Сразу проверяем, чтобы был хотя бы один rcvrId.
                // Нет смысла продолжать дальнейшую обработку, если не найдено разрешённых ресиверов, т.е. критерии !su-модерации заведомо невыполнимы.
                .filter { allowedRcvrIds =>
                  LOGGER.trace(s"$logPrefix Allowed rcvrs[${allowedRcvrIds.size}]: [${allowedRcvrIds.mkString(", ")}]")
                  val r = allowedRcvrIds.nonEmpty
                  if (!r)
                    LOGGER.warn(s"$logPrefix Mdr impossible, since !su, but allowed rcvrs is empty")
                  r
                }

            } else {
              // Для супер-юзеров: берём ресивера из qs, а если его нет - то модерация не ограничена.
              val allRcvrs = mdrRcvrIdOpt.toSet
              Future.successful( allRcvrs )
            }
          }

          // Запустить биллинговую часть: аппрув item'а или отказ в модерации item'а.
          billProcessRes <- {
            LOGGER.trace(s"$logPrefix Will bill-mdr, ${allowedRcvrIds.size}-allowedRcvs, approve?${mdrRes.isApprove}...")
            _processItemsFor {
              if (allowedRcvrIds.isEmpty) q
              else q.filter { i =>
                i.rcvrIdOpt inSet allowedRcvrIds
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


  def findPaidNodeIds4MdrQ(args: MdrSearchArgs, rcvrIds: Traversable[String]): Query[Rep[String], String, Seq] = {
    var q = awaitingPaidMdrItemsSql

    // Пропуск произвольного узла
    for (hideAdId <- args.hideAdIdOpt) {
      q = q.filter { i =>
        i.nodeId =!= hideAdId
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


  /** Аргументы для поиска узлов, требующих бесплатной модерации. */
  def freeMdrNodeSearchArgs(args: MdrSearchArgs, limit1: Int): MNodeSearch = {
    new MNodeSearchDfltImpl {

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
          must        = must
        )

        // Любое состояние эджа модерации является значимым и определяет результат.
        val isAllowedCr = Criteria(
          predicates  = MPredicates.ModeratedBy :: Nil,
          flag        = args.isAllowed,
          must        = Some( args.isAllowed.isDefined )
        )

        var crs = List[Criteria](
          srp,
          isAllowedCr
        )

        // Если задан продьюсер, то закинуть и его в общую кучу.
        /*
        for (prodId <- args.producerId) {
          crs ::= Criteria(
            predicates  = MPredicates.OwnedBy :: Nil,
            nodeIds     = prodId :: Nil,
            must        = must
          )
        }
        */

        crs
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
