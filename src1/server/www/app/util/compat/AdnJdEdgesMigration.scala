package util.compat

import java.util.concurrent.atomic.AtomicInteger

import io.suggest.adn.edit.m.MAdnResView
import io.suggest.common.empty.EmptyUtil
import io.suggest.jd.MJdEdgeId
import io.suggest.model.n2.edge._
import io.suggest.model.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.im
import models.mproj.ICommonDi
import io.suggest.text.parse.ParserUtil.Implicits._
import io.suggest.util.JMXBase
import models.im.AbsCropOp
import japgolly.univeq._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.18 16:25
  * Description: jd-эджи являются кроссплатфроменным аналогом обычных эджей для контента
  * в рамках предиката JdContent, однако конфликтуют со старыми эджами.
  *
  * 1. Удалить старые карточки (до jd). Автоматом исчезнет jd-предикат Bg, который уже не используется.
  *
  * 2. Пройтись по ADN-узлам.
  * Заменить старые эджи картинок jd-эджами (logo, wcfg, ...), а параметры рендера картинки (кропа)
  * уносятся за пределы эджей на уровень конкретного представления.
  * Так можно будет окончательно удалить поле edge.info...dynImgArgs.
  *
  */
class AdnJdEdgesMigration @Inject()(
                                     mNodes     : MNodes,
                                     mCommonDi  : ICommonDi
                                   )
  extends MacroLogsImpl
{

  import mNodes.Implicits._
  import mCommonDi.{mat, ec}

  /** Запуск процедуры обновления БД. */
  def jdMigrate(onlyNodeIds: Seq[String] = Nil): Future[String] = {
    val logPrefix = ""

    val msearch = new MNodeSearchDfltImpl {
      override def nodeTypes: Seq[MNodeType] = {
        MNodeTypes.Ad ::
          MNodeTypes.AdnNode ::
          Nil
      }

      override def withIds = onlyNodeIds
      override def limit = 20
    }

    val adnOldEdges: List[MPredicate] = {
      MPredicates.Logo ::
        MPredicates.WcFgImg ::
        MPredicates.GalleryItem ::
        Nil
    }

    val img3parsers = new im.MImg3.Parsers

    // Извлечь данные представления картинки из эджа и упаковать MJdEdgeId.
    def __edgeOptImg2jdId( medgeOpt2: Option[MEdge] ): Option[MJdEdgeId] = {
      for {
        medge0  <- medgeOpt2
        edgeUid = medge0.doc.uid.get
      } yield {
        val dynOpt = medge0.info.dynImgArgs
        MJdEdgeId(
          edgeUid = edgeUid,
          outImgFormat = dynOpt.map(_.dynFormat),
          crop = for {
            dyn       <- dynOpt
            dynOpsStr <- dyn.dynOpsStr
            imOps     <- img3parsers.parseImgArgs(dynOpsStr).toOption
            crop      <- imOps
              .iterator
              .flatMap {
                case cropOp: AbsCropOp =>
                  cropOp.crop :: Nil
                case _ =>
                  Nil
              }
              .toStream
              .headOption
          } yield {
            crop
          }
        )
      }
    }

    // Для оптимального сброса всех изменений в базу традиционно организуем Bulk processor:
    val bp = mNodes.bulkProcessor(
      new mNodes.BulkProcessorListener( logPrefix )
    )

    val adsDeleted = new AtomicInteger(0)
    val adnNodesUpdated = new AtomicInteger( 0 )

    mNodes
      .source[MNode]( msearch.toEsQuery )
      .runForeach { mnode =>
        val nodeId = mnode.id.get

        // Генерация edge uid. По факту требуется только для adn-узлов.
        var busyEdgeUids = mnode.edges.out
          .flatMap(_.doc.uid)
          .toSet
        var edgeUidCounter = EdgesUtil.nextEdgeUidFrom( busyEdgeUids )
        def __getNextEdgeUid: Int = {
          val r = edgeUidCounter
          busyEdgeUids += r
          edgeUidCounter += 1
          r
        }

        // Обновить эдж, стерев dynImgArgs и обновив предикат:
        def __edgeOptImgUpgrade( medgeOpt0: Option[MEdge] ): Option[MEdge] = {
          for (medge0 <- medgeOpt0) yield {
            medge0.copy(
              // Предикат для всех картинок теперь един:
              predicate = MPredicates.JdContent.Image,
              info      = medge0.info.dynImgArgs.fold (medge0.info) { _ =>
                // Просто удалить данные представления картинки из эджа.
                medge0.info.withDynImgArgs(None)
              },
              doc = medge0.doc.uid.fold[MEdgeDoc] {
                medge0.doc.withUid(
                  Some( __getNextEdgeUid )
                )
              }( _ => medge0.doc )
            )
          }
        }

        // Разобраться, карточка тут или adn-узел?
        mnode.common.ntype match {

          // Рекламная карточка.
          case MNodeTypes.Ad =>
            // Самоконтроль: Убедиться, что она без jd-эджей, и тогда её надо удалить.
            if ( mnode.edges.withPredicateIter(MPredicates.JdContent).isEmpty ) {
              LOGGER.info(s"$logPrefix Will delete ad#$nodeId")
              bp.add( mNodes.prepareDelete( nodeId ).request() )
              adsDeleted.incrementAndGet()

            } else {
              var mnode9 = mnode

              // Удалить старинный Bg-эдж, если он там есть.
              if ( mnode9.edges.withPredicateIter(MPredicates.Bg).nonEmpty ) {
                // Убедиться, что нет Bg-эджа среди остальных эджей jd-карточки?
                LOGGER.info(s"$logPrefix Migrate jd-ad#$nodeId. BG-edge clean-up")
                // Есть что-то в эджах, требующее обновления.
                mnode9 = mnode9.withEdges(
                  edges = mnode9.edges.copy(
                    out = MNodeEdges.edgesToMap1(
                      mnode9.edges
                        .withoutPredicateIter( MPredicates.Bg )
                    )
                  )
                )
              }

              // Перенести dynFormat на уровень jd-doc
              if ( mnode9.edges.out.exists(_.info.dynImgArgs.nonEmpty) ) {
                // Собираем карту эджей
                val edges2uid = mnode9.edges.out
                  .iterator
                  .flatMap { e =>
                    for (uid <- e.doc.uid) yield {
                      uid -> e
                    }
                  }
                  .toMap

                def __upgradeJdEdgeId(jdEdgeIdOpt2: Option[MJdEdgeId]) = {
                  for {
                    jdId    <- jdEdgeIdOpt2
                    bgImgOpt = edges2uid.get( jdId.edgeUid )
                    bgImg   <- bgImgOpt
                    if bgImg.info.dynImgArgs.nonEmpty
                    jdId2   <- __edgeOptImg2jdId( bgImgOpt )
                  } yield {
                    jdId2
                  }
                }

                mnode9 = mnode9.copy(
                  // Обновить шаблон карточки: залить данные из img-эджей
                  extras = mnode9.extras.withDoc(
                    doc = for (doc <- mnode9.extras.doc) yield {
                      doc.withTemplate(
                        for (jdTag <- doc.template) yield {
                          jdTag.copy(
                            props1 = {
                              // Кроп лежал в MJdEdgeId bgImg, а формат - в MEdge. Надо всё запихать в MJdEdgeId.
                              // Нужно залить dynFormat в существующий bgImg, не ломая кроп.
                              val p1UpdatedOpt = for {
                                bgImg0 <- jdTag.props1.bgImg
                                bgImg1 <- __upgradeJdEdgeId( jdTag.props1.bgImg )
                              } yield {
                                jdTag.props1.withBgImg(
                                  Some(
                                    bgImg1.withCrop( bgImg0.crop )
                                  )
                                )
                              }
                              p1UpdatedOpt
                                .getOrElse( jdTag.props1 )
                            },
                            qdProps = for (qdOp <- jdTag.qdProps) yield {
                              val qdOpOpt2 = for {
                                jdId2 <- __upgradeJdEdgeId( qdOp.edgeInfo )
                              } yield {
                                // qd без кропов, просто пересобираем jd id.
                                qdOp.withEdgeInfo( Some(jdId2) )
                              }
                              qdOpOpt2.getOrElse( qdOp )
                            }
                          )
                        }
                      )
                    }
                  ),
                  // Почистить эджи: вычистить dynImgArgs из эджей?
                  edges = mnode9.edges.copy(
                    out = {
                      for {
                        e <- mnode9.edges.out
                        upgradedEdge <- {
                          if (e.predicate ==* MPredicates.JdContent.Image) {
                            __edgeOptImgUpgrade( Some(e) )
                          } else {
                            Some(e)
                          }
                        }
                      } yield {
                        upgradedEdge
                      }
                    }
                  )
                )
              }

              if (mnode9 ne mnode)
                bp.add( mNodes.prepareIndex(mnode9).request() )
            }


          // Это узел с личным кабинетом. Надо отработать древние img-эджи, отделив данные от представления.
          case MNodeTypes.AdnNode =>
            // Пройтись по эджам, которые требуют обработки.
            val oldEdges = mnode.edges
              .withPredicateIter( adnOldEdges: _* )
              .toStream
            lazy val nodeName = mnode.guessDisplayName.getOrElse("")

            if (oldEdges.nonEmpty) {
              LOGGER.trace(s"$logPrefix Will update ${oldEdges.size} edges of node#$nodeId $nodeName:\n ${oldEdges.mkString("\n ")}")
              // Функция поиска эджа в oldEdges по предикату:
              def __findOldEdgeByPred(pred: MPredicate) =
                oldEdges.find(_.predicate ==* pred)

              // Есть старые эджи, которые нужно перевести в jd-эджи, вынеся представление картинок в adn ResView
              val logoEdgeOpt0 = __findOldEdgeByPred( MPredicates.Logo )
              val logoEdgeOpt2 = __edgeOptImgUpgrade( logoEdgeOpt0 )

              val wcFgOpt0 = __findOldEdgeByPred( MPredicates.WcFgImg )
              val wcFgOpt2 = __edgeOptImgUpgrade( wcFgOpt0 )

              val galItemOldSomes = oldEdges
                .iterator
                .filter(_.predicate ==* MPredicates.GalleryItem)
                .map( EmptyUtil.someF )
                .toSeq

              val galEdges2 = galItemOldSomes.flatMap( __edgeOptImgUpgrade )

              val rv = MAdnResView(
                // Извлечь dynImg-данные из старого эджа, и сконвертировать представление картинки:
                logo = __edgeOptImg2jdId( logoEdgeOpt2 ),
                wcFg = __edgeOptImg2jdId( wcFgOpt2 ),
                galImgs = galEdges2
                  .map( Some.apply )
                  .flatMap( __edgeOptImg2jdId )
                  .toSeq
              )

              // Залить измения в mnode:
              val mnode2 = mnode.copy(
                extras = mnode.extras.withAdn(Some(
                  mnode.extras.adn
                    .get
                    .withResView(rv)
                )),
                edges = mnode.edges.copy(
                  out = MNodeEdges.edgesToMap1(
                    mnode.edges
                      // Срезать все старые эджи
                      .withoutPredicateIter( adnOldEdges: _* )
                      // Залить обновлённые эджи JdContent.Image:
                      .++(
                        Iterable[Iterable[MEdge]]( galEdges2, logoEdgeOpt2, wcFgOpt2 )
                          .iterator
                          .flatten
                      )
                  )
                )
              )

              LOGGER.info(s"$logPrefix Will update ADN-node#${mnode.idOrNull} $nodeName:\n oldEdges[${oldEdges.size}] = ${oldEdges.mkString(", ")}\n ADN res-view: $rv")
              bp.add( mNodes.prepareIndex(mnode2).request() )
              adnNodesUpdated.incrementAndGet()

            } else {
              // Нечего обновлять тут.
              LOGGER.debug(s"$logPrefix Skipping ADN-node#${mnode.idOrNull} $nodeName: no img-edges for update")
            }

          // Should never happen
          case _ =>
            ???
        }
      }
      .map { _ =>
        val msg = s"$logPrefix Done. adsDeleted=${adsDeleted.get()} ads, adn-nodes updated = ${adnNodesUpdated.get()}"
        LOGGER.info(msg)
        bp.close()
        msg
      }
  }

}


trait AdnJdEdgesMigrationJmxMBean {
  def jdMigrate(): String
  def jdMigrateNode(nodeId: String): String

}

class AdnJdEdgesMigrationJmx @Inject() (
                                         adnJdEdgesMigration    : AdnJdEdgesMigration,
                                         override val ec        : ExecutionContext
                                       )
  extends JMXBase
  with AdnJdEdgesMigrationJmxMBean
  with MacroLogsImpl
{

  override def jmxName = "io.suggest:type=compat,name=" + classOf[AdnJdEdgesMigration].getSimpleName

  override def futureTimeout = 60.seconds

  override def jdMigrate(): String = {
    LOGGER.warn("jdMigrate()")
    awaitString(
      adnJdEdgesMigration.jdMigrate()
    )
  }

  override def jdMigrateNode(nodeId: String): String = {
    LOGGER.warn(s"jdMigrate($nodeId)")
    awaitString(
      adnJdEdgesMigration.jdMigrate(nodeId :: Nil)
    )
  }

}

