package util.ad

import javax.inject.{Inject, Named, Singleton}
import io.suggest.ad.blk.{BlockWidths, MBlockExpandMode}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.{MSzMult, MSzMults}
import io.suggest.es.model.EsModel
import io.suggest.file.MSrvFileInfo
import io.suggest.grid.GridCalc
import io.suggest.img.MImgFmts
import io.suggest.jd.{MJdConf, MJdData, MJdDoc, MJdEdge, MJdEdgeId, MJdTagId}
import io.suggest.jd.tags.{JdTag, MJdTagNames, MJdtProps1}
import io.suggest.model.n2.edge.{EdgeUid_t, MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.media.{MMedia, MMedias, MPictureMeta}
import io.suggest.model.n2.node.{MNode, MNodes}
import io.suggest.scalaz.NodePath_t
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import models.im.make.MImgMakeArgs
import models.im._
import models.mctx.Context
import play.api.mvc.Call
import util.cdn.CdnUtil
import util.img.{DynImgUtil, IImgMaker}
import monocle.Traversal
import util.showcase.ScWideMaker
import scalaz.std.option._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Tree
import util.ext.ExtRscUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.17 18:52
  * Description: Утиль для рекламных карточек в jd-формате.
  */
@Singleton
class JdAdUtil @Inject()(
                          esModel                     : EsModel,
                          @Named("blk") blkImgMaker   : IImgMaker,
                          wideImgMaker                : ScWideMaker,
                          mMedias                     : MMedias,
                          mNodes                      : MNodes,
                          dynImgUtil                  : DynImgUtil,
                          cdnUtil                     : CdnUtil,
                          extRscUtil                  : ExtRscUtil,
                          implicit private val ec     : ExecutionContext
                        )
  extends MacroLogsImpl
{

  import esModel.api._

  /** img-предикат, используемый в jd-карточках. */
  def imgPredicate = MPredicates.JdContent.Image

  def framePredicate = MPredicates.JdContent.Frame


  /** Получить на руки список MMedia для подготавливаемых картинок.
    *
    * @param imgsEdges Данные по картинкам из prepareImgEdges().
    * @return Фьючерс с картой MMedia.
    */
  def prepareImgMedias(imgsEdges: IterableOnce[(MEdge, MImg3)]): Future[Map[String, MMedia]] = {
    mMedias.multiGetMapCache {
      imgsEdges
        .iterator
        .map(_._2.dynImgId.mediaId)
        .toSet
    }
  }


  /** Подготовка видео-эджей к эксплуатации на дальнейших шагах.
    *
    * @param edges Эджи рекламной карточки.
    * @return Список jd-video-эджей.
    */
  def prepareVideoEdges(edges: MNodeEdges): Seq[MEdge] = {
    edges
      .withPredicateIter( framePredicate )
      .toSeq
  }


  /** Собрать узлы для необходимых медиа-вещей: картинок, видео.
    * Изначально, это нужно только в редакторе.
    * Возможно, если будет проверка доступа к картинкам, но понадобится и при обычном рендере.
    *
    * @param imgsEdges Подготовленные img-эджи, полученные из prepareImgEdges().
    * @param videoEdges Подготовленные video-эджи, полученные из prepareVideoEdges().
    */
  def prepareMediaNodes(imgsEdges: IterableOnce[(MEdge, MImg3)], videoEdges: Seq[MEdge]): Future[Map[String, MNode]] = {
    mNodes.multiGetMapCache {
      // Перечисляем все интересующие img-ноды:
      val imgNodeIdsIter = imgsEdges
        .iterator
        .map(_._1)
      // Присунуть сюда же video-ноды:
      val videoNodeIdsIter = videoEdges
        .iterator
      // Всё объеденить и дедублицировать.
      (imgNodeIdsIter ++ videoNodeIdsIter)
        .flatMap(_.nodeIds)
        .toSet
    }
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
    * @param mediasMap Выхлоп prepareImgMedias().
    * @param mediaNodes Выхлоп prepareMediaNodes().
    * @param mediaHosts Выхлоп prepareDistMediaHosts().
    * @return Список jd-эджей, готовых к использованию в редакторе.
    */
  def mkJdImgEdgesForEdit(
                           imgsEdges      : IterableOnce[(MEdge, MImg3)],
                           mediasMap      : Map[String, MMedia],
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
      mmedia          <- mediasMap.get(mimg.dynImgId.mediaId).iterator
    } yield {
      // uid как-то получился обязательным, хотя TODO его следует сделать опциональным в MJdEdge, и убрать getOrElse-костыль:
      val edgeUid = medge.doc.uid.getOrElse( -i )

      LOGGER.trace(s"$logPrefix E#$edgeUid ${mmedia.idOrNull} imgFmt=${mmedia.file.imgFormatOpt} ${mmedia.file.mime}")
      // Получить инфу по хосту, на котором хранится данная картинка.
      val jdEdge = MJdEdge(
        // Тут раньше безусловно выставлялся предикат imgPredicate, но с adn-редактором понадобились и другие предикаты.
        predicate = medge.predicate,
        id        = edgeUid,
        // url не ставим, потому что очень нужен около-оригинальная картинка, для кропа например.
        fileSrv   = Some(MSrvFileInfo(
          nodeId    = nodeId,
          url       = Some {
            // TODO Вместо сырого оригинала вернуть нечто пересжатое с тем же w/h.
            mkDistMediaUrl(dynImgUtil.imgCall(mimg), mimg.dynImgId, mediaHosts, forceAbsUrls = false)
          },
          // TODO Дальше модель сильно дублирует модель в MMedia.file (без учёта date_created).
          fileMeta  = Some( mmedia.file ),
          name = mediaNodes
            .get( nodeId )
            .flatMap(_.guessDisplayName),
          pictureMeta = mmedia.picture,
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
    (for {
      medge       <- videoEdges.iterator
      edgeUid     <- medge.doc.uid.iterator
      nodeId      <- medge.nodeIds.iterator
      mnode       <- videoNodes.get( nodeId )
      extUrl      <- {
        mnode.extras
          .extVideo
          .map( extRscUtil.toIframeUrl )
          .orElse {
            for (rsc <- mnode.extras.resource) yield {
              rsc.url
            }
          }
      }
    } yield {
      MJdEdge(
        predicate = framePredicate,
        id        = edgeUid,
        url       = Some( extUrl )
      )
    })
      // Явная подготовка без ленивости, т.к. эджей обычно мало, и всё это в отдельном потоке генерится.
      .toList
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
      edgeUid  <- textEdge.doc.uid.iterator
      textOpt = textEdge.doc.text.headOption
      if textOpt.nonEmpty
    } yield {
      MJdEdge(
        predicate = textPred,
        id        = edgeUid,
        text      = textOpt
      )
    })
      // Для явной генерации всех эджей в текущем потоке, параллельно с остальными (img, video, ...) эджами.
      .toList
  }


  def getNodeTpl(mad: MNode) = mad.extras.doc.get.template


  /** traversal от JdTag до bm.isWide-флага. */
  private def _jdt_p1_bm_wide_LENS = {
    JdTag.props1
      .composeLens( MJdtProps1.expandMode )
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
        lens.set(expandMode2)(jdt)
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
        rowKeyStr = imgNodeId,
        // TODO По идее, тут достаточно MImgFmts.defaults, но почему-то всё тогда сглючивает.
        dynFormat = medge.doc.uid
          .flatMap(uids2jdEdgeId.get)
          .flatMap(_.outImgFormat)
          .getOrElse {
            LOGGER.warn(s"collectImgEdges(): Not found edge ${medge.doc.uid} in edgesMap:\n ${uids2jdEdgeId.mkString(", ")}")
            MImgFmts.default
          }
      )
      val origImg = MImg3(dynImgId)
      medge -> origImg
    })
      .toSeq
  }



  /** Настраиваемая логика рендера карточки. */
  trait JdAdDataMakerBase extends Product {

    lazy val logPrefix = s"$productPrefix[${System.currentTimeMillis}]:"

    def nodeId: Option[String]

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
        edgeUid <- edgeAndImg._1.doc.uid
      } yield {
        edgeUid -> edgeAndImg
      })
        .toMap
    }

    // Собрать связанные инстансы MMedia. Т.к. эджи всегда указывают на оригинал, то тут тоже оригиналы.
    lazy val origImgMediasMapFut = prepareImgMedias( origImgsEdges )

    // Собрать video-эджи. Для них надо получить инстансы MNode, чтобы достучаться до ссылок.
    lazy val videoEdges = {
      val ve = prepareVideoEdges( nodeEdges )
      LOGGER.trace(s"$logPrefix Found ${ve.size} video edges: ${ve.mkString(", ")}")
      ve
    }

    /** Для каких img-узлов требуется прочитать ноды? */
    def imgEdgesNeedNodes: Seq[(MEdge, MImg3)]

    // Для имён файлов нужно собрать сами узлы.
    lazy val mediaNodesMapFut = prepareMediaNodes( imgEdgesNeedNodes, videoEdges )

    // Скомпилить jd-эджи картинок.
    // TODO Нужно lazy val тут. Можно сделать через def + lazy val.
    def imgJdEdgesFut: Future[List[MJdEdge]]

    def mediasForMediaHostsFut: Future[Iterable[MMedia]] = {
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

    // Собрать тексты из эджей
    def textJdEdges = mkTextEdges( nodeEdges )

    // Объеденить все эджи.
    def edEdgesFut: Future[Seq[MJdEdge]] = {
      val _imgJdEdgesFut = imgJdEdgesFut
      val _videoJdEdgesFut = videoJdEdgesFut
      val _textJdEdges = textJdEdges
      for {
        videoJdEdges  <- _videoJdEdgesFut
        // Промежуточное объединение c video-эджами. text-эджи справа, так быстрее: text-эджей всегда много, а video-эджей - единицы.
        textAndVideoJdEdges = videoJdEdges reverse_::: _textJdEdges
        imgJdEdges    <- _imgJdEdgesFut
      } yield {
        val r = imgJdEdges reverse_::: textAndVideoJdEdges
        LOGGER.trace(s"$logPrefix Compiled ${r.size} jd edges: text=${_textJdEdges.size} img=${imgJdEdges.size} video=${videoJdEdges.size}")
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
            jdId      = MJdTagId(
              nodeId      = nodeId,
              blockExpand = _finalTpl.rootLabel.props1.expandMode,
              selPathRev  = selPathRev,
            ),
          ),
          edges       = edEdges,
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

      override lazy val tpl = getNodeTpl(mad)

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
            mediasMap   = mediasMap,
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
                    forceAbsUrls            : Boolean,
                    override val selPathRev : NodePath_t,
                   )(implicit ctx: Context)
      extends JdAdDataMakerBase {

      /** Для выдачи не требуется  */
      override def imgEdgesNeedNodes = Nil

      /** Нужно собрать все MMedia из имеющихся: помимо оригиналов надо и скомпиленные картинки. */
      override def mediasForMediaHostsFut: Future[Iterable[MMedia]] = {
        val _origMediasForMediaHostsFut = super.mediasForMediaHostsFut
        for {
          renderedImgs <- renderAdDocImgsFut
          mmedias <- mMedias.multiGetCache {
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
          LOGGER.trace(s"$logPrefix ${origImgsEdges.length} img edges => rendered ${imgsRendered.size} map: [${imgsRendered.iterator.flatMap(_.medge.doc.uid).mkString(", ")}]\n mediaHostsMap[${mediaHostsMap.size}] = ${mediaHostsMap.keys.mkString(",  ")}")
          val imgPred = imgPredicate
          (for {
            imgMakeRes <- imgsRendered.iterator
            edgeUid    <- imgMakeRes.medge.doc.uid
          } yield {
            MJdEdge(
              predicate   = imgPred,
              id          = edgeUid,
              url         = {
                val resImg = imgMakeRes.dynCallArgs
                val url = mkDistMediaUrl(dynImgUtil.imgCall(resImg), resImg.dynImgId, mediaHostsMap, forceAbsUrls)
                Some(url)
              },
              fileSrv = Some( MSrvFileInfo(
                nodeId = imgMakeRes.sourceImg.dynImgId.rowKeyStr,
                pictureMeta = MPictureMeta(
                  whPx   = Some( imgMakeRes.imgSzReal ),
                ),
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
          .composeLens( MJdtProps1.bgImg )
        val _jdt_p1_bgImg_crop_LENS = _jdt_p1_bgImg_LENS
          .composeTraversal( Traversal.fromTraverse[Option, MJdEdgeId] )
          .composeLens( MJdEdgeId.crop )

        for (jdTag <- tpl0) yield {
          val jdTagOpt2 = for {
            // Интересуют только теги с фоновой картинкой
            bgImg <- _jdt_p1_bgImg_LENS.get(jdTag)
            if bgImg.crop.nonEmpty
            // Для которых известен эдж
            edgeAndImg <- imgsEdgesMap.get( bgImg.edgeUid )
            // И там растровая картинка задана:
            if edgeAndImg._2.dynImgId.dynFormat.isRaster
          } yield {
            // Пересобрать тег без crop'а в bgImg:
            _jdt_p1_bgImg_crop_LENS
              .set( None )( jdTag )
          }
          // Если ничего не пересобрано, значит текущий тег не требуется обновлять. Вернуть исходный jd-тег:
          jdTagOpt2.getOrElse( jdTag )
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
          edgeUid       <- medge.doc.uid
          jdLoc         <- tpl
            .loc
            .find { jdTagTree =>
              jdTagTree
                .getLabel
                .edgeUids
                .exists(_.edgeUid ==* edgeUid)
            }
        } yield {
          val jdTag = jdLoc.getLabel
          // 2019.07.22 wide может влиять на размер картинки: узкие решения масштабируются по вертикали.
          val _isJdtWide = (_: JdTag).props1.expandMode.nonEmpty
          val isWideThis = allowWide && _isJdtWide(jdTag)
          val wideSzMult = OptionUtil.maybeOpt(
            isWideThis ||
            (allowWide && jdLoc.parents.exists { parent => _isJdtWide(parent._2) })
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
                lazy val logPrefix2 = s"$logPrefix#qd#E${eit.medge.doc.uid.orNull}#${eit.mimg.dynImgId.rowKeyStr}:"
                for {
                  mmediaOrig  <- embedOrigImgsMap.get( eit.mimg.dynImgId.original.mediaId )
                  origWh      <- mmediaOrig.picture.whPx
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
                  LOGGER.trace(s"$logPrefix2 embed.img#${eit.mimg.dynImgId.rowKeyStr} edge#${eit.medge.doc.uid.orNull} width=>${jdTagWidthCssPxOpt.orNull}")

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
              LOGGER.trace(s"$logPrefix img-edge#${eit.medge.doc.uid.orNull} qd?$isQd sz=>$tgImgSz")

              // Если есть кроп у текущей картинки, то запихнуть его в dynImgOps
              val mimg2 = eit.jdTag.props1.bgImg
                .fold(eit.mimg) { bgImgJdId =>
                  MImg3.dynImgId.modify { dynId =>
                    dynId.copy(
                      dynFormat = bgImgJdId.outImgFormat
                        .getOrElse( dynId.dynFormat ),
                      dynImgOps = bgImgJdId.crop
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
              for {
                imakeRes <- maker.icompile( makeArgs )
              } yield {
                MImgRenderInfo(eit.medge, eit.mimg, imakeRes.dynCallArgs, imakeRes.szReal)
              }
            })
              // Явно запустить на исполнение все future в списке:
              .to( List )
          }

        } yield {
          LOGGER.trace(s"$logPrefix Done, ${results.size} img.results.")
          results
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
    val tplEdgeUids = tpl.deepEdgesUids.toSet
    if (tplEdgeUids.isEmpty) {
      edges
    } else {
      MNodeEdges.out.modify { out0 =>
        out0.filter { medge =>
          medge.doc.uid
            .exists( tplEdgeUids.contains )
        }
      }( edges )
    }
  }

}

trait IJdAdUtilDi {
  def jdAdUtil: JdAdUtil
}
