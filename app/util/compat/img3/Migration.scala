package util.compat.img3

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.edge.{MPredicates, MEdge, MNodeEdges, MEdgeInfo}
import io.suggest.model.n2.geo.MGeoShape
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNodeTypes, MNodeType, MNode}
import io.suggest.util.JMXBase
import io.suggest.ym.model.{MWelcomeAd, MAd, MAdnNodeGeo, MAdnNode}
import models.ISize2di
import models.im.{MImg3, MImg3_, MImg}
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import play.api.Configuration
import util.PlayLazyMacroLogsImpl
import util.blocks.BgImg

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.15 17:47
 * Description: Система обновления картинок на новую архитектуру: N2, seaweedfs.
 *
 * Чек-лист миграции #1.
 * 1.[СДЕЛАНО 2015.oct.29] application.conf:
 *  {{{
 *    play.http.errorHandler = "controllers.ErrorHandler"
 *    play.http.filters = "util.xplay.Filters"
 *  }}}
 * 2.[СДЕЛАНО 2015.oct.29] Отработать всё нижеследующее через JMX.
 *
 * // Миграция MAd
 * 09. MNode.resaveMany().
 * 10. Вызвать this.migrateMads()
 */
class Migration @Inject() (
  mImg3                 : MImg3_,
  configuration         : Configuration,
  implicit val ec       : ExecutionContext,
  implicit val esClient : Client,
  implicit val sn       : SioNotifierStaticClientI
)
  extends PlayLazyMacroLogsImpl
{

  import LOGGER._

  /** Пройтись по узлам ADN, логотипы сохранить через MMedia, создать необходимые MEdge. */
  // TODO Исполнено на продакшене 2015.oct.29. Можно удалять, если в течение недели не понадобится снова.
  def adnNodesToN2(): Future[LogosAcc] = {
    // Обойти все узлы ADN, прочитав оттуда данные по логотипам.
    MAdnNode.foldLeftAsync( LogosAcc() ) { (acc0Fut, mAdnNode) =>
      // Принудительно тормозим обработку из-за большого траффика между cassandra, sioweb и seaweedfs.
      // Узлы обходятся строго по очереди. Иначе будет Too many connections, и Broken pipe в http-клиенте при
      // аплоаде картинки как итог.
      acc0Fut flatMap { _ =>
        portOneNode(acc0Fut, mAdnNode)
      }
    }
  }

  /** Портировать одну картинку. */
  private def portOneNode(acc0Fut: Future[LogosAcc], mAdnNode: MAdnNode): Future[LogosAcc] = {
    val adnNodeId = mAdnNode.id.get
    val logPrefix = s"[$adnNodeId]"
    trace(s"$logPrefix Processing ADN node: ${mAdnNode.meta.name} / ${mAdnNode.meta.town}")

    // Запуск обработки логотипа узла.
    val logoEdgeOptFut = FutureUtil.optFut2futOpt(mAdnNode.logoImgOpt) { logoImg =>
      val oldImg = MImg(logoImg)
      val mlocImgFut = oldImg.original.toLocalImg
      val mimg = mImg3.fromImg( oldImg ).original
      for {
        mLocImg     <- mlocImgFut
        imgNodeId   <- portOneImage("logo", mimg, mAdnNode.meta.dateCreated, logoImg.meta)
      } yield {
        Some( MEdge(MPredicates.Logo, imgNodeId) )
      }
    }

    val galEdgesFut = Future.traverse( mAdnNode.gallery.zipWithIndex ) {
      case (galImgFileName, i) =>
        val oldImg = MImg( galImgFileName )
        val mlocImgFut = oldImg.original.toLocalImg
        val mimg = mImg3.fromImg( oldImg ).original
        for {
          mlocImg   <- mlocImgFut
          imgNodeId <- portOneImage("gal", mimg, mAdnNode.meta.dateCreated)
        } yield {
          MEdge( MPredicates.GalleryItem, imgNodeId, order = Some(i), info = MEdgeInfo(
            dynImgArgs = mimg.qOpt
          ))
        }
    }

    // В n2-архитектуре гео-шейпы узла хранятся прямо в узле, т.е. nested doc вместо parent-child doc.
    val nodeGssFut = for {
      nodeGss <- MAdnNodeGeo.findByNode(adnNodeId, maxResults = 50)
    } yield {
      nodeGss
        .iterator
        .zipWithIndex
        .map { case (madnGeo, i) =>
          MGeoShape(
            id          = i,
            glevel      = madnGeo.glevel,
            shape       = madnGeo.shape,
            fromUrl     = madnGeo.url,
            dateEdited  = madnGeo.lastModified
          )
        }
        .toSeq
    }

    val mnode0 = mAdnNode.toMNode

    val mnode1Fut = for {
      logoEdgeOpt <- logoEdgeOptFut
      galEdges    <- galEdgesFut
      nodeGss     <- nodeGssFut
    } yield {
      mnode0.copy(
        edges = mnode0.edges.copy(
          out = mnode0.edges.out ++ MNodeEdges.edgesToMapIter1(
            logoEdgeOpt.iterator ++ galEdges.iterator
          )
        ),
        geo = mnode0.geo.copy(
          shapes = nodeGss
        )
      )
    }

    val mnode1SaveFut = mnode1Fut
      .flatMap(_.save)

    val logosCountFut = logoEdgeOptFut.map(_.size)
    val galImgsCountFut = galEdgesFut.map(_.size)
    val gssCountFut = nodeGssFut.map(_.size)

    for {
      logosCount    <- logosCountFut
      galImgsCount  <- galImgsCountFut
      gssCount      <- gssCountFut
      mnode1id      <- mnode1SaveFut
      acc0          <- acc0Fut
    } yield {
      acc0.copy(
        adnNodes = acc0.adnNodes  + 1,
        logos    = acc0.logos     + logosCount,
        galImgs  = acc0.galImgs   + galImgsCount,
        gss      = acc0.gss       + gssCount
      )
    }
  }

  /** Аккамулятор результатов при обходе узлов для портирования логотипов. */
  case class LogosAcc(
    adnNodes  : Int = 0,
    logos     : Int = 0,
    galImgs   : Int = 0,
    gss       : Int = 0
  ) {

    def toReport: String = {
      s"ADN Nodes: $adnNodes;\nLogos: $logos;\nGal imgs: $galImgs\nGeoShapes: $gss"
    }
  }


  /** Портирование одного оригинала картинки. */
  private def portOneImage(prefix: String, mimg: MImg3, dateCreatedDflt: DateTime, imetaOpt: Option[ISize2di] = None): Future[String] = {

    lazy val logPrefix = s"portOneImage(${mimg.rowKeyStr}): $prefix"

    val imgNodeId = mimg.rowKeyStr
    trace(s"$logPrefix processing: $mimg")

    val imgSaveFut = mimg.saveToPermanent

    imgSaveFut onComplete {
      case Success(mmId) =>
        trace(s"$logPrefix saved permanently: " + mimg)
      case Failure(ex) =>
        error(s"$logPrefix failed to save MMedia", ex)
    }

    for {
      _     <- imgSaveFut
    } yield {
      info(s"$logPrefix done: [$imgNodeId]")
      imgNodeId
    }
  }


  /** Первыми MNode'ами были теги. Они не имели типа, но это отрабатывалось на уровне последующих парсеров.
    * Нужно найти и пересохранить все необходимые теги. */
  // TODO Исполнено на продакшене 2015.oct.29. Можно удалить через неделю, если не понадобится.
  def resaveMissingTypeTags(): Future[Int] = {
    val msearch = new MNodeSearchDfltImpl {
      override def nodeTypes = Seq(null)
    }
    MNode.updateAll(queryOpt = Some(msearch.toEsQuery)) { mnode0 =>
      Future successful mnode0
    }
  }


  case class MadsCntsAcc(
    done        : Int = 0,
    noProducer  : Int = 0
  ) {

    def toReport: String = {
      s"Ads: $done; skipped: missing producer = $noProducer"
    }
  }


  /** Миграция рекламных карточек. */
  def migrateMads(): Future[MadsCntsAcc] = {

    // Возможны ошибочные карточки, не привязанные к кабинетам.
    // Нужно их отсеивать.
    val prodsExistsFut: Future[Set[String]] = {
      val msearch = new MNodeSearchDfltImpl {
        override def nodeTypes: Seq[MNodeType] = Seq( MNodeTypes.AdnNode )
        // На момент написания этого кода в системе было 229 узлов.
        override def limit = 500
      }
      MNode.dynSearchIds(msearch)
        .map { _.toSet }
    }
    prodsExistsFut.onSuccess { case peSet =>
      LOGGER.info(s"Producer IDs set have ${peSet.size} keys.")
    }

    // Собрать общую карту всех тегов, благо их немного сейчас (100-200 тегов вкл.повторяющиеся).
    val tagEdgesMapFut: Future[Map[String, MEdge]] = {
      val mtsearch = new MNodeSearchDfltImpl {
        override def nodeTypes = Seq( MNodeTypes.Tag )
      }
      MNode.foldLeftAsync [Iterator[(String, MEdge)]] (Iterator.empty, queryOpt = mtsearch.toEsQueryOpt) {
        (acc2Fut, mtnode) =>
          val iter2 = for {
            tagNodeId     <- mtnode.id.iterator
            tagExtra      <- mtnode.extras.tag.iterator
            (_, tagFace)  <- tagExtra.faces.iterator
          } yield {
            tagFace.name -> MEdge(MPredicates.TaggedBy, tagNodeId)
          }
          acc2Fut map { accIter =>
            accIter ++ iter2
          }
      }.map { iter3 =>
        iter3.toMap
      }
    }
    tagEdgesMapFut onSuccess { case tem =>
      LOGGER.info(s"Tags map has ${tem.size} keys (tags).")
    }

    // Запустить обход карточек.
    MAd.foldLeftAsync(MadsCntsAcc()) { (acc0Fut, mad) =>
      val resFut = for {
        prodsExists   <- prodsExistsFut
        if prodsExists.contains(mad.producerId)
        // Принудительно тормозим обработку, чтобы портирование картинок шло легче.
        _             <- acc0Fut
        tagEdgesMap   <- tagEdgesMapFut
        acc2          <- migrateMad(mad, tagEdgesMap, acc0Fut)
      } yield {
        acc2
      }
      resFut.recoverWith { case ex: NoSuchElementException =>
        // Нет портированного продьюсера, относящегося к этой карточки. Это значит, это карточка висит в воздухе и недосягаема,
        // портировать её нет смысла никакого.
        acc0Fut map { acc0 =>
          acc0.copy(
            noProducer = acc0.noProducer + 1
          )
        }
      }
    }
  }


  /** Подавлять ошибки отсутствующих картинок? [false] */
  private def SUPPRESS_MISSING_IMGS: Boolean = {
    configuration.getBoolean("compat.migration.imgs.missing.suppress")
      .getOrElse(false)
  }

  /** Выполнить миграцию одной карточки на N2-архитектуру. */
  private def migrateMad(mad: MAd, tagEdgesMap: Map[String, MEdge], acc0Fut: Future[MadsCntsAcc]): Future[MadsCntsAcc] = {
    val _SUPRESS_MISSING_IMGS = SUPPRESS_MISSING_IMGS

    // Отработать картинки
    val bgEdgesOptFut: Future[Option[MEdge]] = {
      val fn = BgImg.BG_IMG_FN
      val imgOpt = mad.imgs.get(fn)
      FutureUtil.optFut2futOpt(imgOpt) { mii =>
        val oldImg = MImg(mii)
        val mlocImgFut = oldImg.original.toLocalImg
        val mimg = mImg3.fromImg(oldImg).original
        val fut = for {
          mLocImg   <- mlocImgFut
          imgNodeId <- portOneImage("bg", mimg, mad.dateCreated, mii.meta)
        } yield {
          val e = MEdge(MPredicates.Bg, imgNodeId, info = MEdgeInfo(
            dynImgArgs = oldImg.qOpt
          ))
          debug("oldImg = " + oldImg + " FROM " + mii + " TO edge " + e)
          Some(e)
        }
        if (_SUPRESS_MISSING_IMGS) {
          fut.recover { case ex: Throwable =>
            LOGGER.warn(s"Supressed error for img $imgOpt", ex)
            None
          }
        } else {
          fut
        }
      }
    }

    // Отработать возможные теги карточки.
    val tagEdgesFut: Future[Seq[MEdge]] = {
      val tedges = mad.tags
        .iterator
        .map { case (_, te) =>
          tagEdgesMap(te.face)
        }
        .toList   // форсируем не-Stream-коллекцию, чтобы всё вычислилось в этом потоке, а не в общей куче.
      Future successful tedges
    }

    val mnode0 = mad.toMNode

    val mnode1Fut = for {
      bgEdgeOpt <- bgEdgesOptFut
      tagsEdges <- tagEdgesFut
    } yield {
      mnode0.copy(
        edges = mnode0.edges.copy(
          out = {
            val iter1 = mnode0.edges.out.valuesIterator ++
              bgEdgeOpt.iterator ++
              tagsEdges.iterator
            MNodeEdges.edgesToMap1( iter1 )
          }
        )
      )
    }

    val saveFut = mnode1Fut
      .flatMap { _.save }

    for {
      acc0  <- acc0Fut
      _adId <- saveFut
    } yield {
      acc0.copy(
        done = acc0.done + 1
      )
    }
  }


  /** Миграция карточек приветствия. Т.к. пока не удалось спроецировать приветствия на
    * рекламные карточки, то карточка приветствия становится опциональным ребром на логотип приветствия. */
  def wcAdsToN2(): Future[MwaAcc] = {
    val p = MPredicates.NodeWelcomeAdIs

    // Найти все узлы, имеющие эдж на карточку приветствия
    val msearch = new MNodeSearchDfltImpl {
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(
          predicates = Seq(p)
        )
        Seq(cr)
      }
      override def nodeTypes = Seq(MNodeTypes.AdnNode)
      // На момент написания на продакшене было ~230 кабинетов всего.
      override def limit = 600
    }
    val mnodesFut = MNode.dynSearch(msearch)

    // Загрузить в память все карточки приветствия, на которые ссылаются узлы.
    val wadsMapFut = mnodesFut.flatMap { mnodes =>
      MWelcomeAd.multiGetMap {
        mnodes.iterator
          .flatMap { _.edges.withPredicateIter(p) }
          .map(_.nodeId)
      }
    }

    for {
      mnodes  <- mnodesFut
      wadsMap <- wadsMapFut
      acc     <- {
        mnodes.foldLeft( Future.successful(MwaAcc()) ) { (acc0Fut, mnode) =>
          _migradeWad(mnode, acc0Fut, wadsMap)
        }
      }
    } yield {
      acc
    }
  }

  /** Аккамулятор результатов портирования welcome ad. */
  case class MwaAcc(done: Int = 0, skipped: Int = 0, error: Int = 0) {
    def toReport: String = {
      s"WelcomeAds:\n------\ndone = $done\nskipped: missing waOpt = $skipped\nfailed = $error"
    }
  }

  private def WELCOME_IMG_KEY = "wlcm"

  /** Миграция одной welcome-карточки. */
  private def _migradeWad(mnode: MNode, acc0Fut: Future[MwaAcc], wadsMap: Map[String, MWelcomeAd]): Future[MwaAcc] = {
    mnode.edges
      .withPredicateIter( MPredicates.NodeWelcomeAdIs )
      .toStream
      .headOption
      .fold {
        LOGGER.warn(s"Skipped welcome node ${mnode.id.get}")
        acc0Fut.map { acc0 =>
          acc0.copy(
            skipped = acc0.skipped + 1
          )
        }
      } { waEdge =>
        // Если карточка отсутствует, то она уже портирована. Это связано с тем,
        // что один и тот же эдж используется в старом и новом формате.
        val waFut = MWelcomeAd.getById(waEdge.nodeId)
          .map { _.get }
        val oldImgInfoFut = for (wa <- waFut) yield {
          wa.imgs.get( WELCOME_IMG_KEY ).get
        }
        val oldImgFut = for (info <- oldImgInfoFut) yield {
          MImg( info )
        }
        val oldLocalImgFut = oldImgFut.flatMap { oldImg =>
          oldImg.original.toLocalImg
        }
        // Запустить операцию обновления.
        val saveFut = for {
          oldImgInfo  <- oldImgInfoFut
          oldImg      <- oldImgFut
          oldLocalImg <- oldLocalImgFut

          // Портануть картинку на img3-архитектуру.
          imgNodeId   <- {
            val newImg = mImg3.fromImg(oldImg.original)
            portOneImage("wclogo", newImg, DateTime.now, oldImgInfo.meta)
          }

          // Обновить welcome-эдж узла.
          mnodeUpdatedId <- {
            val p = MPredicates.NodeWelcomeAdIs
            val edge2 = MEdge(p, imgNodeId, info = MEdgeInfo(
              dynImgArgs = oldImg.qOpt
            ))
            MNode.tryUpdate(mnode) { mnode0 =>
              mnode0.copy(
                edges = mnode0.edges.copy(
                  out = {
                    MNodeEdges.edgesToMap1(
                      mnode0.edges.withoutPredicateIter(p) ++ Iterator(edge2)
                    )
                  }
                )
              )
            }
          }

          // Обновить аккамулятор
          acc <- {
            acc0Fut.map { acc0 =>
              acc0.copy(
                done = acc0.done + 1
              )
            }
          }
        } yield {
          // Вернуть обновлённый акк.
          acc
        }

        saveFut.recoverWith { case ex: Throwable =>
          LOGGER.warn(s"Failed to port welcome ad [${waEdge.nodeId}] on node [${mnode.id.get}]", ex)
          acc0Fut.map { acc0 =>
            acc0.copy(
              error = acc0.error + 1
            )
          }
        }
      }
  }

}


trait MigrationJmxMBean {
  def adnNodesToN2(): String
  def resaveMissingTypeTags(): String
  def madsToN2(): String
  def wcAdsToN2(): String
}

class MigrationJmx @Inject() (
  migration       : Migration,
  implicit val ec : ExecutionContext
)
  extends JMXBase
  with MigrationJmxMBean
{

  override def jmxName: String = "io.suggest:type=n2,name=" + migration.getClass.getSimpleName
  override def futureTimeout = 1000.seconds

  // TODO Исполнено на продакшене 2015.oct.29. Можно удалить через неделю, если не понадобится.
  override def adnNodesToN2(): String = {
    val strFut = migration.adnNodesToN2()
      .map { acc =>
        "Total processed\n\n" + acc.toReport
      }
    awaitString( strFut )
  }

  // TODO Исполнено на продакшене 2015.oct.29. Можно удалить через неделю, если не понадобится.
  override def resaveMissingTypeTags(): String = {
    val strFut = migration.resaveMissingTypeTags()
      .map { count =>
        "Total resaved: " + count
      }
    awaitString( strFut )
  }


  /** Портирование рекламных карточек на N2. */
  override def madsToN2(): String = {
    val fut = for (info <- migration.migrateMads()) yield {
      info.toReport
    }
    awaitString(fut)
  }

  /** Портирование welcome ads. */
  override def wcAdsToN2(): String = {
    val fut = for (info <- migration.wcAdsToN2()) yield {
      info.toReport
    }
    awaitString(fut)
  }

}
