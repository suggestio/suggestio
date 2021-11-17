package util.ad

import javax.inject.Inject
import io.suggest.ad.blk.{BlockWidths, MBlockExpandMode}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.{MSzMult, MSzMults}
import io.suggest.es.model.EsModel
import io.suggest.file.MSrvFileInfo
import io.suggest.grid.GridCalc
import io.suggest.jd.{MJdConf, MJdData, MJdDoc, MJdEdge, MJdEdgeId, MJdTagId}
import io.suggest.jd.tags.{JdTag, MJdProps1, MJdTagNames}
import io.suggest.n2.edge.{EdgeUid_t, MEdge, MEdgeDoc, MNodeEdges, MPredicates}
import io.suggest.n2.media.MPictureMeta
import io.suggest.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.sc.MScApiVsn
import io.suggest.scalaz.NodePath_t
import io.suggest.scalaz.ScalazUtil.Implicits.EphStreamExt
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import models.im.make.MImgMakeArgs
import models.im._
import models.mctx.Context
import play.api.mvc.Call
import util.cdn.CdnUtil
import util.img.DynImgUtil
import monocle.Traversal
import play.api.inject.Injector
import util.showcase.ScWideMaker
import scalaz.std.option._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Tree
import util.blocks.BlkImgMaker
import util.ext.ExtRscUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.17 18:52
  * Description: Утиль для рекламных карточек в jd-формате.
  */
final class JdAdUtil @Inject()(
                                injector                    : Injector,
                              )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val blkImgMaker = injector.instanceOf[BlkImgMaker]
  private lazy val wideImgMaker = injector.instanceOf[ScWideMaker]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val dynImgUtil = injector.instanceOf[DynImgUtil]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]
  private lazy val extRscUtil = injector.instanceOf[ExtRscUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /** img-предикат, используемый в jd-карточках. */
  def imgPredicate = MPredicates.JdContent.Image

  def framePredicate = MPredicates.JdContent.Frame


  /** Подготовка видео-эджей к эксплуатации на дальнейших шагах.
    *
    * @param edges Эджи рекламной карточки.
    * @return Список jd-video-эджей.
    */
  def prepareVideoEdges(edges: MNodeEdges): Seq[MEdge] = {
    edges
      .withPredicate( framePredicate )
      .out
  }

  def prepareAdEdges(edges: MNodeEdges): Seq[MEdge] = {
    edges
      .withPredicate( MPredicates.JdContent.Ad )
      .out
  }

  /** Собрать узлы для необходимых медиа-вещей: картинок, видео.
    * Изначально, это нужно только в редакторе.
    * Возможно, если будет проверка доступа к картинкам, но понадобится и при обычном рендере.
    *
    * @param imgsEdges Подготовленные img-эджи, полученные из prepareImgEdges().
    * @param otherEdges Подготовленные video-эджи (или иные типы), полученные из prepareVideoEdges() или других источников.
    */
  def prepareMediaNodes(imgsEdges: IterableOnce[(MEdge, MImg3)],
                        otherEdges: (MNodeType, Seq[MEdge])*
                       ): Future[Map[String, MNode]] = {

    import esModel.api._

    val allNodeIds = (
      // Перечисляем все интересующие img-ноды:
      ( imgsEdges
       .iterator
       .flatMap(_._2.dynImgId.mediaIdAndOrigMediaId)
      ) #:: (
        // Присунуть сюда же video и другие ноды:
        otherEdges
          .flatMap(_._2)
          .flatMap(_.nodeIds)
      ) #::
        LazyList.empty
    )
      .iterator
      .flatten
      .toSet

    mNodes.multiGetMapCache( allNodeIds )
    // TODO Нужно сверить ожидаемые типы узлов с полученными:
  }


  /** Собрать ссылку на media-ресурс с учётом dist и cdn.
    * БОльшая часть метода не привязана к картинки, только генератор ссылки.
    *
    * @param call Исходная внутренняя ссылка.
    * @param dynImgId Обработаный указатель на картинку.
    * @param mediaHosts Карта хостов.
    * @return Строка URL для рендера в HTML-документе.
    */
  private def mkDistMediaUrl(call: Call, dynImgId: MDynImgId, mediaHosts: Map[String, Seq[MHostInfo]], forceAbsUrls: Boolean)
                            (implicit ctx: Context): String = {
    cdnUtil.maybeAbsUrl( forceAbsUrls )(
      cdnUtil.forMediaCall1(call, mediaHosts, dynImgId.mediaIdAndOrigMediaId)
    )
  }


  /** Сборка jd-эджей картинок для редактора карточки.
    * От выдачи отличается использованием fileName'ов и оригиналов картинок.
    *
    * @param imgsEdges Выхлоп prepareImgEdges().
    * @param mediaNodes Выхлоп prepareMediaNodes().
    * @param mediaHosts Выхлоп prepareDistMediaHosts().
    * @return Список jd-эджей, готовых к использованию в редакторе.
    */
  def mkJdImgEdgesForEdit(
                           imgsEdges      : IterableOnce[(MEdge, MImg3)],
                           mediaNodes     : Map[String, MNode],
                           mediaHosts     : Map[String, Seq[MHostInfo]]
                         )(implicit ctx: Context): List[MJdEdge] = {
    lazy val logPrefix = s"mkImgJdEdgesForEdit[${System.currentTimeMillis()}]:"

    // Получены медиа-файлы на руки.
    (for {
      // Пройти по BgImg-эджам карточки:
      ((medge, mimg), i) <- imgsEdges.iterator.zipWithIndex
      // id узла эджа -- это идентификатор картинки.
      nodeId          <- medge.nodeIds.iterator
      imgNode         <- mediaNodes.get(mimg.dynImgId.mediaId).iterator
      if imgNode.common.ntype ==* MNodeTypes.Media.Image
      fileEdge        <- imgNode.edges.withPredicateIter( MPredicates.Blob.File )
      edgeMedia       <- fileEdge.media.iterator
    } yield {
      // uid как-то получился обязательным, хотя TODO его следует сделать опциональным в MJdEdge, и убрать getOrElse-костыль:
      val edgeDoc2 = if (medge.doc.id.isEmpty) {
        MEdgeDoc.id
          .replace( Some(-i) )( medge.doc )
      } else {
        medge.doc
      }

      LOGGER.trace(s"$logPrefix E#${edgeDoc2.id.orNull} ${imgNode.idOrNull} imgFmt=${edgeMedia.file.imgFormatOpt} ${edgeMedia.file.mime getOrElse ""}")
      // Получить инфу по хосту, на котором хранится данная картинка.
      val jdEdge = MJdEdge(
        // Тут раньше безусловно выставлялся предикат imgPredicate, но с adn-редактором понадобились и другие предикаты.
        predicate = medge.predicate,
        nodeId    = Some( nodeId ),
        edgeDoc   = edgeDoc2,
        // url не ставим, потому что очень нужен около-оригинальная картинка, для кропа например.
        fileSrv   = Some(MSrvFileInfo(
          url       = Some {
            // TODO Вместо сырого оригинала вернуть нечто пересжатое с тем же w/h.
            mkDistMediaUrl(dynImgUtil.imgCall(mimg), mimg.dynImgId, mediaHosts, forceAbsUrls = false)
          },
          // TODO Дальше модель сильно дублирует модель в MMedia.file (без учёта date_created).
          fileMeta  = edgeMedia.file,
          name = mediaNodes
            .get( nodeId )
            .flatMap(_.guessDisplayName),
          pictureMeta = edgeMedia.picture,
        ))
      )

      LOGGER.trace(s"$logPrefix Img edge compiled: $jdEdge")
      jdEdge
    })
      // Явный неленивый рендер эджей в текущем потоке, поэтому toList.
      .toList
  }


  /** Сборка jd-video-эджей на основе выхлопа и прочитанных из БД узлов.
    *
    * @param videoEdges Выхлоп prepareVideoEdges().
    * @param videoNodes Выхлоп prepareMediaNodes().
    * @return Список jd-эджей.
    */
  def mkJdVideoEdges(videoEdges: Seq[MEdge], videoNodes: Map[String, MNode]): List[MJdEdge] = {
    lazy val logPrefix = s"mkJdVideoEdges():"
    (for {
      medge       <- videoEdges.iterator
      if {
        val r = medge.doc.id.nonEmpty
        if (!r) LOGGER.warn(s"$logPrefix Missing edge uid for edge: $medge")
        r
      }
      nodeId      <- medge.nodeIds.iterator
      mnode       <- videoNodes.get( nodeId )
      if {
        val expectedNtype = MNodeTypes.ExternalRsc
        val r = mnode.common.ntype eqOrHasParent expectedNtype
        if (!r) LOGGER.warn(s"$logPrefix Node $nodeId exist with type ${mnode.common.ntype}, but only $expectedNtype allowed: $medge")
        r
      }
      extUrl      <- {
        val r = mnode.extras
          .extVideo
          .map( extRscUtil.toIframeUrl )
          .orElse {
            for (rsc <- mnode.extras.resource) yield {
              rsc.url
            }
          }
        if (r.isEmpty) LOGGER.warn(s"$logPrefix Can't constuct video URL for video-node#$nodeId, extVideo=${mnode.extras.extVideo} extRsc=${mnode.extras.resource}")
        r
      }
    } yield {
      MJdEdge(
        predicate = framePredicate,
        edgeDoc   = medge.doc,
        url       = Some( extUrl )
      )
    })
      // Явная подготовка без ленивости, т.к. эджей обычно мало, и всё это в отдельном потоке генерится.
      .to( List )
  }


  /** Отрендерить ad-jd-эджи.
    *
    * @param adEdges Исходные N2-эджи.
    * @param nodesMap Карта узлов.
    * @return Список результирующих эджей.
    */
  def mkJdAdEdges(adEdges: Seq[MEdge], nodesMap: Map[String, MNode]): List[MJdEdge] = {
    lazy val logPrefix = "mkJdAdEdges():"
    (for {
      adEdge <- adEdges.iterator
      if {
        val r = adEdge.doc.id.nonEmpty
        if (!r) LOGGER.warn(s"$logPrefix Missing edge uid for: $adEdge")
        r
      }
      adNodeId <- adEdge.nodeIds
      mnode <- nodesMap.get( adNodeId )
      if {
        val ntypeOk = MNodeTypes.Ad
        val r = mnode.common.ntype ==* ntypeOk
        if (!r) LOGGER.warn(s"$logPrefix Node#$adNodeId has unexpected ntype#${mnode.common.ntype}, allowed ntype=$ntypeOk")
        r
      }
    } yield {
      MJdEdge(
        predicate = adEdge.predicate,
        edgeDoc   = adEdge.doc,
        nodeId    = Some( adNodeId ),
      )
    })
      .to( List )
  }


  /** Сборка jd-text-эджей из рекламной карточки.
    *
    * @param edges Эджи рекламной карточки.
    * @return Список jd-эджей.
    */
  def mkTextEdges(edges: MNodeEdges): List[MJdEdge] = {
    val textPred = MPredicates.JdContent.Text
    (for {
      textEdge <- edges
        .withPredicateIter( textPred )
      if textEdge.doc.text.nonEmpty && textEdge.doc.id.nonEmpty
    } yield {
      MJdEdge(
        predicate = textPred,
        edgeDoc   = textEdge.doc,
      )
    })
      // Для явной генерации всех эджей в текущем потоке, параллельно с остальными (img, video, ...) эджами.
      .toList
  }


  /** traversal от JdTag до bm.isWide-флага. */
  private def _jdt_p1_bm_wide_LENS = {
    JdTag.props1
      .andThen( MJdProps1.expandMode )
  }

  /** Выставление нового значения флага wide для всех блоков, имеющих иное значение флага.
    * Обычно используется для подавление true-значений для случаев, когда wide-рендер невозможен.
    *
    * @param blkTpl Шаблон блока или документа.
    * @param expandMode2 Новое значение expandMode.
    *              Обычно false, чтобы рендерить строго плитку вместо обычного варианта.
    * @return Обновлённое дерево документа.
    */
  def resetBlkWide(blkTpl: Tree[JdTag], expandMode2: Option[MBlockExpandMode] = None): Tree[JdTag] = {
    val lens = _jdt_p1_bm_wide_LENS
    for (jdt <- blkTpl) yield {
      if (jdt.name ==* MJdTagNames.STRIP && lens.exist(_ !=* expandMode2)(jdt)) {
        (lens replace expandMode2)(jdt)
      } else {
        jdt
      }
    }
  }


  /** Выделение img-эджей из общего списка эджей рекламной карточки.
    *
    * @param nodeEdges Эджи рекламной карточки.
    * @return Эджи, пригодные для использования в пакетном imgs-рендере,
    *         например в renderAdDocImgs().
    */
  def collectImgEdges(nodeEdges: MNodeEdges, uids2jdEdgeId: Map[EdgeUid_t, MJdEdgeId]): Seq[(MEdge, MImg3)] = {
    lazy val logPrefix = s"collectImgEdges()#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix nodeEdges=${nodeEdges.out.length}edges, uids2jdEdgeId=${uids2jdEdgeId.size}map")

    (for {
      medge <- nodeEdges.withPredicateIter( imgPredicate )
      imgNodeId <- {
        if (medge.nodeIds.isEmpty)
          LOGGER.error(s"$logPrefix Missing nodeIds for img-edge: $medge")
        medge.nodeIds
      }
    } yield {
      val dynImgId = MDynImgId(
        origNodeId = imgNodeId,
        imgFormat = medge.doc.id
          .flatMap(uids2jdEdgeId.get)
          .flatMap(_.outImgFormat)
      )
      val origImg = MImg3(dynImgId)
      medge -> origImg
    })
      .toSeq
  }



  /** Настраиваемая логика рендера карточки. */
  sealed trait JdAdDataMakerBase extends Product {

    lazy val logPrefix = s"$productPrefix[${System.currentTimeMillis}]:"

    def nodeId: Option[String]

    def nodeTitle: Option[String]

    /** Для сборки jd-id верхнего уровня используется сие значение поля selPathRev: */
    def selPathRev: NodePath_t

    // Сразу получаем шаблон, чтобы при вызове поверх левых узлов сразу была ошибка.
    def tpl: Tree[JdTag]

    def nodeEdges: MNodeEdges

    // Собираем картинки, используемые в карточке.
    // Следует помнить, что в jd-карточках модификация картинки задаётся в теге. Эджи всегда указывают на оригинал.
    lazy val origImgsEdges = {
      val uid2jdEdgeId = tpl.edgesUidsMap
      val ie = collectImgEdges( nodeEdges, uid2jdEdgeId )
      LOGGER.trace(s"$logPrefix Found ${ie.size} img.edges: ${ie.iterator.map(_._2.dynImgId.fileName).mkString(", ")}")
      ie
    }

    lazy val imgsEdgesMap = {
      (for {
        edgeAndImg <- origImgsEdges.iterator
        edgeUid <- edgeAndImg._1.doc.id
      } yield {
        edgeUid -> edgeAndImg
      })
        .toMap
    }

    // Собрать связанные инстансы MMedia. Т.к. эджи всегда указывают на оригинал, то тут тоже оригиналы.
    lazy val origImgMediasMapFut = prepareMediaNodes( origImgsEdges )

    // Собрать video-эджи. Для них надо получить инстансы MNode, чтобы достучаться до ссылок.
    lazy val videoEdges = {
      val ve = prepareVideoEdges( nodeEdges )
      if (ve.nonEmpty)
        LOGGER.trace(s"$logPrefix Found ${ve.size} video edges: ${ve.mkString(", ")}")
      ve
    }

    lazy val adEdges = {
      val adEdges = prepareAdEdges( nodeEdges )
      if (adEdges.nonEmpty)
        LOGGER.trace(s"$logPrefix Found ${adEdges.size} ad edges.")
      adEdges
    }

    /** Для каких img-узлов требуется прочитать ноды? */
    def imgEdgesNeedNodes: Seq[(MEdge, MImg3)]

    // Для имён файлов нужно собрать сами узлы.
    lazy val mediaNodesMapFut = prepareMediaNodes(
      imgEdgesNeedNodes,
      MNodeTypes.ExternalRsc.VideoExt -> videoEdges,
      MNodeTypes.Ad -> adEdges,
    )

    // Скомпилить jd-эджи картинок.
    // TODO Нужно lazy val тут. Можно сделать через def + lazy val.
    def imgJdEdgesFut: Future[List[MJdEdge]]

    def mediasForMediaHostsFut: Future[Iterable[MNode]] = {
      origImgMediasMapFut.map(_.values)
    }

    // Собрать инфу по хостам, хранящим интересующие media-файлы.
    def mediaHostsMapFut: Future[Map[String, Seq[MHostInfo]]] = {
      mediasForMediaHostsFut
        .flatMap( cdnUtil.mediasHosts )
    }

    // Скомпилить jd-эджи для видосов:
    def videoJdEdgesFut = for {
      mediaNodesMap   <- mediaNodesMapFut
    } yield {
      mkJdVideoEdges(
        videoEdges = videoEdges,
        videoNodes = mediaNodesMap
      )
    }

    def adJdEdgesFut = for {
      mediaNodesMap   <- mediaNodesMapFut
    } yield {
      mkJdAdEdges( adEdges, mediaNodesMap )
    }

    // Собрать тексты из эджей
    def textJdEdges = mkTextEdges( nodeEdges )

    // Объеденить все эджи.
    def edEdgesFut: Future[Seq[MJdEdge]] = {
      val _imgJdEdgesFut = imgJdEdgesFut
      val _videoJdEdgesFut = videoJdEdgesFut
      val _adJdEdgesFut = adJdEdgesFut

      val _textJdEdges = textJdEdges

      for {
        videoJdEdges  <- _videoJdEdgesFut
        // Промежуточное объединение c video/ad-эджами. text-эджи справа, так быстрее: text-эджей всегда много, а video-эджей - единицы.
        adJdEdges     <- _adJdEdgesFut
        nonImgJdEdges = adJdEdges reverse_::: videoJdEdges reverse_::: _textJdEdges
        imgJdEdges    <- _imgJdEdgesFut
      } yield {
        val r = imgJdEdges reverse_::: nonImgJdEdges
        LOGGER.trace(s"$logPrefix Compiled ${r.size} jd edges: text=${_textJdEdges.size} img=${imgJdEdges.size} video=${videoJdEdges.size} ad=${adJdEdges.size}")
        r
      }
    }

    def finalTpl: Tree[JdTag] = tpl

    /** Запуск сборки данных jd-карточки на исполнение.
      *
      * @return Фьючерс с отрендеренными данными карточки.
      */
    def execute(): Future[MJdData] = {
      val _edEdgesFut = edEdgesFut
      val _finalTpl = finalTpl
      for {
        edEdges <- _edEdgesFut
      } yield {
        MJdData(
          doc = MJdDoc(
            template  = _finalTpl,
            tagId      = MJdTagId(
              nodeId      = nodeId,
              blockExpand = _finalTpl.rootLabel.props1.expandMode,
              selPathRev  = selPathRev,
            ),
          ),
          edges       = edEdges,
          title       = nodeTitle,
        )
      }
    }

  }


  /** Внутренняя инфа по результату пре-рендера одного img-edge. */
  protected[this] case class MImgRenderInfo(
                                             medge        : MEdge,
                                             sourceImg    : MImg3,
                                             dynCallArgs  : MImgT,
                                             imgSzReal    : MSize2di
                                           )

  /** Различные варианты сборки карточек. */
  object mkJdAdDataFor {

    /** Сборщик jd-карточек для редактора карточек.
      *
      * @param mad Узел рекламной карточки.
      */
    case class edit(mad: MNode)
                   (implicit ctx: Context) extends JdAdDataMakerBase {

      /** jdId.selPathRev всегда Nil, т.к. тут редактируется карточка целиком, и документ всегда top-level.
        * Если в будущем это изменится (по-блоковое редактирование), то надо унести это в конструктор edit(). */
      override def selPathRev = Nil

      override def nodeTitle = mad.meta.basic.nameOpt

      override lazy val tpl = mad.extras.doc.get.template

      override lazy val nodeEdges = mad.edges

      override def nodeId = None

      override def imgEdgesNeedNodes = origImgsEdges

      override def imgJdEdgesFut: Future[List[MJdEdge]] = {
        val _mediaHostsMapFut = mediaHostsMapFut
        val _mediaNodesMapFut = mediaNodesMapFut
        for {
          mediasMap           <- origImgMediasMapFut
          mediaNodesMap       <- _mediaNodesMapFut
          imgMedia2hostsMap   <- _mediaHostsMapFut
        } yield {
          LOGGER.trace(s"$logPrefix Found ${mediasMap.size} linked img medias.")
          mkJdImgEdgesForEdit(
            imgsEdges   = origImgsEdges,
            mediaNodes  = mediaNodesMap,
            mediaHosts  = imgMedia2hostsMap
          )
        }
      }

    }


    /** Рендер карточек для выдачи.
      *
      * @param jdConf Параметры рендера плитки.
      * @param tpl Шаблон для рендера.
      *            Обычно -- неполный шаблон, а только главный блок.
      * @param nodeEdges Эджи рекламной карточки.
      *                  Обычно тут отфильтрованные по переданному шаблону эджи.
      * @param ctx Контекст рендера.
      * @param allowWide Допускается ли широкий рендер, если это требуется шаблоном?
      *                  Для плитке -- нет, для фокусировки -- да.
      * @param selPathRev Значения для jdId.selPathRev для JdDoc на выходе.
      *                   В случае целикового рендера всей карточки (документа) тут Nil
      *                   Если рендер одного блока из документа, то надо указать порядковый индекс блока (по zipWithIndex).
      */
    case class show(override val nodeId     : Option[String],
                    override val nodeEdges  : MNodeEdges,
                    override val tpl        : Tree[JdTag],
                    jdConf                  : MJdConf,
                    allowWide               : Boolean,
                    override val selPathRev : NodePath_t,
                    override val nodeTitle  : Option[String],
                    scApiVsn                : Option[MScApiVsn] = None,
                   )(implicit ctx: Context)
      extends JdAdDataMakerBase {

      def forceAbsUrls: Boolean =
        scApiVsn.exists(_.forceAbsUrls)

      /** Для выдачи не требуется  */
      override def imgEdgesNeedNodes = Nil

      /** Нужно собрать все MMedia из имеющихся: помимо оригиналов надо и скомпиленные картинки. */
      override def mediasForMediaHostsFut: Future[Iterable[MNode]] = {
        import esModel.api._

        val _origMediasForMediaHostsFut = super.mediasForMediaHostsFut
        for {
          renderedImgs <- renderAdDocImgsFut
          mmedias <- mNodes.multiGetCache {
            // Интересуют только деривативы, которые могут прямо сейчас существовать в медиа-хранилище. Оригиналы возьмём из _origMediasForMediaHostsFut.
            (for {
              renderedImg <- renderedImgs.iterator
              dynImgId = renderedImg.dynCallArgs.dynImgId
              if dynImgId.hasImgOps
            } yield {
              dynImgId.mediaId
            })
              .to( Iterable )
          }

          origMedias <- _origMediasForMediaHostsFut
        } yield {
          val res = mmedias.view ++ origMedias
          LOGGER.trace(s"$logPrefix mediasForMediaHostsFut: collected ${res.size} medias:\n ${res.iterator.flatMap(_.id).mkString(",\n ")}")
          res
        }
      }

      /** Для выдачи требуются готовые картинки, подогнанные под экран устройства клиента. */
      override def imgJdEdgesFut: Future[List[MJdEdge]] = {
        val _imgsRenderedFut  = renderAdDocImgsFut
        val _mediaHostsMapFut = mediaHostsMapFut
        // TODO Сделать, чтобы mkDistMediaUrl-балансировка работала не по эджам, а только по mmedia.storage.
        //val _origImgMediasMap = origImgMediasMapFut
        for {
          imgsRendered      <- _imgsRenderedFut
          mediaHostsMap     <- _mediaHostsMapFut
        } yield {
          LOGGER.trace(s"$logPrefix ${origImgsEdges.length} img edges => rendered ${imgsRendered.size} map: [${imgsRendered.iterator.flatMap(_.medge.doc.id).mkString(", ")}]\n mediaHostsMap[${mediaHostsMap.size}] = ${mediaHostsMap.keys.mkString(",  ")}")
          val imgPred = imgPredicate
          (for {
            imgMakeRes <- imgsRendered.iterator
          } yield {
            val nodeIdSome = Some( imgMakeRes.sourceImg.dynImgId.origNodeId )
            MJdEdge(
              predicate   = imgPred,
              nodeId      = nodeIdSome,
              edgeDoc     = imgMakeRes.medge.doc,
              url         = {
                val resImg = imgMakeRes.dynCallArgs
                val url = mkDistMediaUrl(dynImgUtil.imgCall(resImg), resImg.dynImgId, mediaHostsMap, forceAbsUrls)
                Some(url)
              },
              fileSrv = Some( MSrvFileInfo(
                pictureMeta = MPictureMeta(
                  whPx   = Some( imgMakeRes.imgSzReal ),
                ),
                // Дополнительно, возвращать клиенту nodeId внутри jd-эджа для приложений <= 4.2:
                nodeId_legacy42 = scApiVsn
                  .filter(_.jdEdgeSrvFileNodeIdMustBe)
                  .flatMap(_ => nodeIdSome),
              )),
            )
          })
            // Для явной подготовки данных строго в текущем потоке, используем toList вместо toStream/toVector.
            .toList
        }
      }


      override def finalTpl: Tree[JdTag] = {
        val tpl0 = super.finalTpl
        // Удалить все crop'ы из растровых картинок, у которых задан кроп. Все растровые картинки должны бы быть уже подогнаны под карточку.

        val _jdt_p1_bgImg_LENS = JdTag.props1
          .andThen( MJdProps1.bgImg )

        val _jdt_p1_bgImg_crop_LENS = _jdt_p1_bgImg_LENS
          .andThen( Traversal.fromTraverse[Option, MJdEdgeId] )
          .andThen( MJdEdgeId.crop )

        for (jdTag <- tpl0) yield {
          (for {
            // Интересуют только теги с фоновой картинкой
            bgImg <- _jdt_p1_bgImg_LENS.get(jdTag)
            if bgImg.crop.nonEmpty
            // Для которых известен эдж
            edgeAndImg <- imgsEdgesMap.get( bgImg.edgeUid )
            // И там растровая картинка задана:
            if !edgeAndImg._2.dynImgId.imgFormat.exists( _.isVector )
          } yield {
            // Пересобрать тег без crop'а в bgImg:
            (_jdt_p1_bgImg_crop_LENS replace None)( jdTag )
          })
            // Если ничего не пересобрано, значит текущий тег не требуется обновлять. Вернуть исходный jd-тег:
            .getOrElse( jdTag )
        }
      }


      /** Рендер картинок в строго необходимом размере.
        * Это подходит для выдачи, но НЕ для редактора, который оперирует оригиналами.
        */
      lazy val renderAdDocImgsFut: Future[Iterable[MImgRenderInfo]] = {
        // TODO Всё становится жирным и разъезжается, если использовать запрошенный szMult.
        // Где-то на верхних уровнях szMult неправильно рассчитывается, повторяя внутри себя devPixelRatio.
        // Надо вспомнить изначальное значение szMult, либо переосмыслить его, что есть источник проблемы. Либо запретить здесь учитывать screen pxRatio.
        val szMult = MSzMults.`1.0`
        val szMultedF = MSzMult.szMultedF( szMult )

        // Сразу запустить в фоне получение MMedia для оригиналов картинок:
        val _origImgsMediasMapFut = origImgMediasMapFut

        lazy val logPrefix = s"renderAdDocImgs#${System.currentTimeMillis()}:"

        case class EdgeImgTag(
                               medge        : MEdge,
                               mimg         : MImg3,
                               jdTag        : JdTag,
                               isWide       : Boolean,
                               wideSzMult   : Option[MSzMult],
                             )

        // Собрать в многоразовую коллекцию все данные по img-эджам и связанным с ними тегам:
        val edgedImgTags: Seq[EdgeImgTag] = (for {
          (medge, mimg) <- origImgsEdges.iterator
          edgeUid       <- medge.doc.id
          imgEdgeSearchCache = new JdTag.HtmlImgEdgeUidsCache
          jdLoc         <- tpl
            .loc
            .find { jdtLoc =>
              jdtLoc
                .allImgEdgeUids( imgEdgeSearchCache )
                .iterator
                .exists(_.edgeUid ==* edgeUid)
            }
        } yield {
          val jdTag = jdLoc.getLabel
          // 2019.07.22 wide может влиять на размер картинки: узкие решения масштабируются по вертикали.
          val _isJdtWide = (_: JdTag).props1.expandMode.nonEmpty
          val isWideThis = allowWide && _isJdtWide(jdTag)
          val wideSzMult = OptionUtil.maybeOpt(
            isWideThis ||
            (allowWide && !jdLoc
              .parents
              .filter { parent => _isJdtWide(parent._2) }
              .isEmpty)
          ) {
            jdTag.props1.wh.flatMap { wh =>
              GridCalc.wideSzMult( wh, jdConf.gridColumnsCount )
            }
          }
          LOGGER.trace(s"$logPrefix ${medge.nodeIds.mkString(",")} : WIDE szMult=${wideSzMult.orNull}")
          EdgeImgTag( medge, mimg, jdTag, isWideThis, wideSzMult )
        })
          // Отработать все img-эджи, пока в фоне идёт подготовка mediaMap. Иначе - отрабатывать лениво асинхронно в Future.sequence().
          .to(
            if (_origImgsMediasMapFut.isCompleted) LazyList
            else List
          )


        // 2018-02-06 Из-за при ресайзе embed-картинок, в аттрибутах фигурирует только только ширина.
        // Но для рассчёта финальной картинки, в ImgMakeArgs нужна и ширина, и высота.
        // Для картинки, которой не хватает h или wh, надо прочитать orig-размеры из MMedia.

        // Надо узнать, для каких картинок надо будет дополучить из MMedia данные оригинала:
        for {
          // Дождаться данных из MMedia
          embedOrigImgsMap <- _origImgsMediasMapFut

          // TODO Всё правильно тут? Масштаб вообще не учитывается? А должен бы.
          //szMult = 1.0f
          //pxRatio = DevPixelRatios.pxRatioDefaulted( devScreenOpt.flatMap(_.pixelRatioOpt) )

          // Продолжить обход списка эджей, создав фьючерс результата
          results <- Future.sequence {
            val wideNorms =
              BlockWidths.min.value ::
              BlockWidths.max.value ::
              wideImgMaker.WIDE_WIDTHS_PX.tail

            (for {
              eit <- edgedImgTags.iterator
              isQd = eit.jdTag.name ==* MJdTagNames.QD_OP

              tgImgSz: MSize2di <- if (isQd) {
                lazy val logPrefix2 = s"$logPrefix#qd#E${eit.medge.doc.id.orNull}#${eit.mimg.dynImgId.origNodeId}:"
                for {
                  mmediaOrig  <- embedOrigImgsMap.get( eit.mimg.dynImgId.original.mediaId )
                  if mmediaOrig.common.ntype ==* MNodeTypes.Media.Image
                  fileEdge    <- mmediaOrig.edges
                    .withPredicateIter( MPredicates.Blob.File )
                    .nextOption()
                  edgeMedia   <- fileEdge.media
                  origWh      <- edgeMedia.picture.whPx
                } yield {
                  // Узнать ширину, заданную в теге (если есть).
                  val jdTagWidthCssPxOpt = for {
                    qdProps     <- eit.jdTag.qdProps
                    attrsEmbed  <- qdProps.attrsEmbed
                    widthSu     <- attrsEmbed.width
                    width       <- widthSu.toOption
                  } yield {
                    width
                  }
                  LOGGER.trace(s"$logPrefix2 embed.img#${eit.mimg.dynImgId.origNodeId} edge#${eit.medge.doc.id.orNull} width=>${jdTagWidthCssPxOpt.orNull}")

                  // Картинка есть. Но надо разобраться, надо ли её ресайзить.
                  val origWidthNorm = wideImgMaker.normWideBgSz( origWh.width, wideNorms )

                  // Узнать точно, какую ширину требуется получить на выходе.
                  // Маловероятно, что в jdTag отсутствует ширина, но всё же отрабатываем и эту ситуацию.
                  val widthPxNonNormal0 = jdTagWidthCssPxOpt.getOrElse {
                    // Нет заданной в теге ширины. Это не хорошо, но ошибку лучше подавить.
                    LOGGER.warn(s"$logPrefix2 Width expected for embed.img.\n jdt=${eit.jdTag}\n img-edge=${eit.medge}\n img=${eit.mimg}.\n Suppressed error, will use origWH=$origWh as scaled img.size.")
                    origWh.width
                  }

                  // Опциональная wide-поправка вносится здесь
                  val widthPxNonNormal = eit.wideSzMult
                    .filter( _ => allowWide )
                    .fold( widthPxNonNormal0 ) { _ => szMultedF(widthPxNonNormal0, eit.wideSzMult) }

                  val jdTagWidthCssPxNorm = wideImgMaker.normWideBgSz( widthPxNonNormal, wideNorms )
                  LOGGER.trace(s"$logPrefix2 Norm.width for jd-tag: ${widthPxNonNormal}px => ${jdTagWidthCssPxNorm}px; origW=${origWh.width}px=>${origWidthNorm}px")

                  // Рассчитать размер итоговой картинки.
                  // TODO Проблема: blkImgMaker учитывает и szMult, и pxRatio. Надо разобраться, что с чем сравнивать вообще.
                  val targetImgSzPx = if (jdTagWidthCssPxNorm /* * szMult * pxRatio.pixelRatio */ < origWidthNorm) {
                    // Велик соблазн возвращать непересжатую картинку, но этого делать не стоит: она может быть огромной.
                    // Если norm-размеры совпадают, то надо пересжать без изменения orig-размера: это будет быстро и без размывания пикселей.
                    val heightCssPxNorm = Math.round(jdTagWidthCssPxNorm.toDouble / origWh.width.toDouble * origWh.height).toInt
                    LOGGER.trace(s"$logPrefix2 Img need downscaling: $origWh => (${jdTagWidthCssPxNorm}x$heightCssPxNorm)")
                    MSize2di(
                      width  = jdTagWidthCssPxNorm,
                      height = heightCssPxNorm,
                    )
                  } else {
                    // Картика не требует дополнительного пересжатия, можно просто вернуть оригинальный размер.
                    LOGGER.trace(s"$logPrefix2 Img resizing not needed. Returning origWh=$origWh")
                    origWh
                  }

                  targetImgSzPx
                }
              } else {
                val wideTgSzOpt = for {
                  h <- eit.jdTag.props1.heightPx
                  if eit.wideSzMult.nonEmpty
                } yield {
                  // Надо посчитать новый вертикальный размер для wide-блока.
                  val heightPx2 = szMultedF(h, eit.wideSzMult)
                  LOGGER.trace(s"$logPrefix Found wideSzMult $szMult, target img height: ${h}px => ${heightPx2}px")
                  MSize2di(
                    // width нет смысла вычислять, т.к. wideMaker игнорирует target width.
                    width  = 0,
                    height = heightPx2,
                  )
                }
                // Если нет wide-мультипликатора, то оставить исходный размер блока.
                wideTgSzOpt.orElse[MSize2di] {
                  val whOpt = eit.jdTag.props1.wh
                  LOGGER.trace(s"$logPrefix Using block meta as img.wh: ${whOpt.orNull}")
                  whOpt
                }
              }

            } yield {
              LOGGER.trace(s"$logPrefix img-edge#${eit.medge.doc.id.orNull} qd?$isQd sz=>$tgImgSz")

              // Если есть кроп у текущей картинки, то запихнуть его в dynImgOps
              val mimg2 = eit.jdTag.props1.bgImg
                .fold(eit.mimg) { bgImgJdId =>
                  MImg3.dynImgId.modify { dynId =>
                    dynId.copy(
                      imgFormat = bgImgJdId.outImgFormat
                        .orElse( dynId.imgFormat ),
                      imgOps = bgImgJdId.crop
                        .map(AbsCropOp.apply)
                        .toList
                    )
                  }( eit.mimg )
                }

              val makeArgs = MImgMakeArgs(
                img           = mimg2,
                targetSz      = tgImgSz,
                szMult        = szMult.toFloat,
                devScreenOpt  = ctx.deviceScreenOpt,
                compressMode  = Some( CompressModes.maybeFg(isQd) ),
              )

              // Выбираем img maker исходя из конфигурации рендера.
              val maker =
                if (eit.isWide) wideImgMaker
                else blkImgMaker

              // Дописать в результат рассчёта картинки инфу по оригинальной картинке:
              (for {
                imakeRes <- FutureUtil.tryCatchFut( maker.icompile( makeArgs ) )
              } yield {
                val res = MImgRenderInfo(eit.medge, eit.mimg, imakeRes.dynCallArgs, imakeRes.szReal)
                Some(res)
              })
                // Отработка ошибок рендера картинки:
                .recover { case ex: Throwable =>
                  LOGGER.error(s"$logPrefix Cannot prepare img for ad#${nodeId.orNull}${nodeTitle.fold("")(" | " + _)}\n jdTag = ${eit.jdTag}\n mimg2 = $mimg2\n makeArgs = $makeArgs\n img maker = $maker\n edge = ${eit.medge}", ex)
                  None
                }
            })
              // Явно запустить на исполнение все future в списке:
              .to( List )
          }

        } yield {
          val results2 = results.flatten
          LOGGER.trace(s"$logPrefix Done, ${results2} success of ${results.size} total img.results.")
          results2
        }
      }

    }

  }


  /** Почистить исходные эджи карточки, оставив только необходимые для рендера шаблона.
    *
    * @param tpl Используемый для рендера шаблон.
    * @param edges Исходный контейнер эджей узла.
    * @return Облегчённый контейнер эджей узла.
    */
  def filterEdgesForTpl(tpl: Tree[JdTag], edges: MNodeEdges): MNodeEdges = {
    val deepEdgesUidsEph = tpl.deepEdgesUids
    if (deepEdgesUidsEph.isEmpty) {
      edges
    } else {
      val tplEdgeUids = deepEdgesUidsEph.iterator.toSet
      MNodeEdges.out.modify { out0 =>
        out0.filter { medge =>
          medge.doc.id
            .exists( tplEdgeUids.contains )
        }
      }( edges )
    }
  }

}
