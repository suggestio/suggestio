package util.ad

import javax.inject.{Inject, Named, Singleton}

import io.suggest.ad.blk.BlockWidths
import io.suggest.color.MHistogram
import io.suggest.common.geom.d2.MSize2di
import io.suggest.file.MSrvFileInfo
import io.suggest.jd.{MJdAdData, MJdEdge}
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.jd.tags.JdTag.Implicits._
import io.suggest.model.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.media.{MFileMetaHash, MMedia, MMediasCache}
import io.suggest.model.n2.node.{MNode, MNodesCache}
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import models.im.make.{IImgMaker, MImgMakeArgs}
import models.im._
import models.mctx.Context
import models.req.IReqHdr
import play.api.mvc.Call
import util.cdn.CdnUtil
import util.img.DynImgUtil
import util.vid.VideoUtil
import models.blk.SzMult_t
import util.showcase.ScWideMaker

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.17 18:52
  * Description: Утиль для рекламных карточек в jd-формате.
  */
@Singleton
class JdAdUtil @Inject()(
                          @Named("blk") blkImgMaker   : IImgMaker,
                          wideImgMaker                : ScWideMaker,
                          mMediasCache                : MMediasCache,
                          mNodesCache                 : MNodesCache,
                          dynImgUtil                  : DynImgUtil,
                          cdnUtil                     : CdnUtil,
                          videoUtil                   : VideoUtil,
                          implicit private val ec     : ExecutionContext
                       )
  extends MacroLogsImpl
{

  /** img-предикат, используемый в jd-карточках. */
  def imgPredicate = MPredicates.JdContent.Image

  def videoPredicate = MPredicates.JdContent.Video


  /** Выделение img-эджей из общего списка эджей рекламной карточки.
    *
    * @param edges Эджи рекламной карточки.
    * @return Итератор, пригодный для использования в пакетом imgs-рендере,
    *         например в renderAdDocImgs().
    */
  def prepareOrigImgEdges(edges: MNodeEdges): Seq[(MEdge, MImg3)] = {
    edges
      // TODO Надо ли тут Bg-предикат? по идее, он устарел ещё до ввода jd-редактора.
      .withPredicateIter( imgPredicate, MPredicates.Bg )
      .map { medge =>
        //LOGGER.info(s"prepareImgEdges(): E#${medge.doc.uid.orNull} img=${medge.info.dynImgArgs}")
        // По факту, тут всегда будет оригинал, даже если без .original. Потому что в jd-карточках эджи НЕ хранят в себе кроп или иные модификации.
        val origImg = MImg3(medge).original
        medge -> origImg
      }
      .toSeq
  }


  /** Получить на руки список MMedia для подготавливаемых картинок.
    *
    * @param imgsEdges Данные по картинкам из prepareImgEdges().
    * @return Фьючерс с картой MMedia.
    */
  def prepareImgMedias(imgsEdges: TraversableOnce[(MEdge, MImg3)]): Future[Map[String, MMedia]] = {
    mMediasCache.multiGetMap {
      imgsEdges
        .toIterator
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
      .withPredicateIter( videoPredicate )
      .toSeq
  }


  /** Собрать узлы для необходимых медиа-вещей: картинок, видео.
    * Изначально, это нужно только в редакторе.
    * Возможно, если будет проверка доступа к картинкам, но понадобится и при обычном рендере.
    *
    * @param imgsEdges Подготовленные img-эджи, полученные из prepareImgEdges().
    * @param videoEdges Подготовленные video-эджи, полученные из prepareVideoEdges().
    */
  def prepareMediaNodes(imgsEdges: TraversableOnce[(MEdge, MImg3)], videoEdges: Seq[MEdge]): Future[Map[String, MNode]] = {
    mNodesCache.multiGetMap {
      // Перечисляем все интересующие img-ноды:
      val imgNodeIdsIter = imgsEdges
        .toIterator
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
    * @param medge Исходный MEdge.
    * @param mediaHosts Карта хостов.
    * @return Строка URL для рендера в HTML-документе.
    */
  private def mkDistMediaUrl(call: Call, medge: MEdge, mediaHosts: Map[String, Seq[MHostInfo]]): String = {
    def logPrefix = s"mkDistMediaUrl(#${medge.doc.uid.orNull},$call):"
    medge
      .nodeIds
      .headOption
      .fold {
        // Вообще, это плохо, если нет хостнейма для media. Это значит, что-то не так.
        LOGGER.warn(s"$logPrefix Media host-name missing for edge $medge")
        call
      } { nodeId =>
        LOGGER.trace(s"$logPrefix Using nodeId#$nodeId for media-edge edge nodeIds=[${medge.nodeIds.mkString(", ")}]")
        cdnUtil.forMediaCall(call, nodeId, mediaHosts )
      }
      .url
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
                           imgsEdges      : TraversableOnce[(MEdge, MImg3)],
                           mediasMap      : Map[String, MMedia],
                           mediaNodes     : Map[String, MNode],
                           mediaHosts     : Map[String, Seq[MHostInfo]]
                         ): Seq[MJdEdge] = {
    lazy val logPrefix = s"mkImgJdEdgesForEdit[${System.currentTimeMillis()}]:"

    // Получены медиа-файлы на руки.
    val iter = for {
      // Пройти по BgImg-эджам карточки:
      (medge, mimg)   <- imgsEdges.toIterator
      // id узла эджа -- это идентификатор картинки.
      edgeUid         <- medge.doc.uid.iterator
      nodeId          <- medge.nodeIds.iterator
      mmedia          <- mediasMap.get(mimg.dynImgId.mediaId).iterator
    } yield {
      LOGGER.trace(s"$logPrefix E#$edgeUid ${mmedia.idOrNull} imgFmt=${mmedia.file.imgFormatOpt} ${mmedia.file.mime}")
      // Получить инфу по хосту, на котором хранится данная картинка.
      val jdEdge = MJdEdge(
        predicate = imgPredicate,
        id        = edgeUid,
        // url не ставим, потому что очень нужен около-оригинальная картинка, для кропа например.
        fileSrv   = Some(MSrvFileInfo(
          nodeId    = nodeId,
          url       = Some {
            // TODO Вместо сырого оригинала вернуть нечто пересжатое с тем же w/h.
            mkDistMediaUrl(dynImgUtil.imgCall(mimg), medge, mediaHosts)
          },
          // TODO Дальше модель сильно дублирует модель в MMedia.file (без учёта date_created).
          sizeB     = Some( mmedia.file.sizeB ),
          mimeType  = Some( mmedia.file.mime ),
          hashesHex = MFileMetaHash.toHashesHex( mmedia.file.hashesHex ),
          colors    = for (_ <- mmedia.picture.colors.headOption) yield {
            MHistogram( mmedia.picture.colors )
          },
          name = mediaNodes.get( nodeId )
            .flatMap(_.guessDisplayName),
          whPx = mmedia.picture.whPx
        ))
      )

      LOGGER.trace(s"$logPrefix Img edge compiled: $jdEdge")
      jdEdge
    }
    iter.toSeq
  }


  /** Сборка jd-video-эджей на основе выхлопа и прочитанных из БД узлов.
    *
    * @param videoEdges Выхлоп prepareVideoEdges().
    * @param videoNodes Выхлоп prepareMediaNodes().
    * @return Список jd-эджей.
    */
  def mkJdVideoEdges(videoEdges: Seq[MEdge], videoNodes: Map[String, MNode]): Seq[MJdEdge] = {
    val iter = for {
      medge       <- videoEdges.iterator
      edgeUid     <- medge.doc.uid.iterator
      nodeId      <- medge.nodeIds.iterator
      mnode       <- videoNodes.get( nodeId )
      extVideo    <- mnode.extras.extVideo
    } yield {
      MJdEdge(
        predicate = videoPredicate,
        id        = edgeUid,
        url       = Some( videoUtil.toIframeUrl(extVideo) )
      )
    }
    iter.toSeq
  }


  /** Сборка jd-text-эджей из рекламной карточки.
    *
    * @param edges Эджи рекламной карточки.
    * @return Список jd-эджей.
    */
  def mkTextEdges(edges: MNodeEdges): Seq[MJdEdge] = {
    val textPred = MPredicates.JdContent.Text
    val textJdEdgesIter = for {
      textEdge <- edges
        .withPredicateIter( textPred )
      edgeUid  <- textEdge.doc.uid.iterator
      text     <- textEdge.doc.text
    } yield {
      MJdEdge(
        predicate = textPred,
        id        = edgeUid,
        text      = Some(text)
      )
    }
    textJdEdgesIter.toSeq
  }


  /** Конкатенация всех наборов jd-эджей воедино.
    *
    * @param edges jd-эджи из функций mkJd*Edges().
    * @return Коллекция со всеми jd-эджами.
    */
  def mergeJdEdges[T <: MJdEdge](edges: Seq[T]*): Seq[T] = {
    edges
      .iterator
      .flatten
      .toSeq
  }


  def getNodeTpl(mad: MNode) = mad.extras.doc.get.template


  /** Узнать главный блок в карточке. */
  def getMainBlockTpl(mad: MNode): Tree[JdTag] = {
    getNodeTpl(mad)
      .getMainBlockOrFirst
  }


  /** Настраиваемая логика рендера карточки. */
  trait JdAdDataMakerBase extends Product {

    lazy val logPrefix = s"$productPrefix[${System.currentTimeMillis}]:"

    def nodeId: Option[String]

    // Сразу получаем шаблон, чтобы при вызове поверх левых узлов сразу была ошибка.
    def tpl: Tree[JdTag]

    def nodeEdges: MNodeEdges

    // Собираем картинки, используемые в карточке.
    // Следует помнить, что в jd-карточках модификация картинки задаётся в теге. Эджи всегда указывают на оригинал.
    lazy val origImgsEdges = {
      val ie = prepareOrigImgEdges( nodeEdges )
      LOGGER.trace(s"$logPrefix Found ${ie.size} img.edges: ${ie.iterator.map(_._2.dynImgId.fileName).mkString(", ")}")
      ie
    }

    lazy val imgsEdgesMap = {
      val iter = for {
        edgeAndImg <- origImgsEdges.iterator
        edgeUid <- edgeAndImg._1.doc.uid
      } yield {
        edgeUid -> edgeAndImg
      }
      iter.toMap
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

    // Собрать инфу по хостам, хранящим интересующие media-файлы.
    def mediaHostsMapFut = origImgMediasMapFut.flatMap { medias =>
      cdnUtil.mediasHosts( medias.values )
    }

    // Скомпилить jd-эджи картинок.
    def imgJdEdgesFut: Future[Seq[MJdEdge]]

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
    def edEdgesFut = {
      val _videoJdEdgesFut = videoJdEdgesFut
      for {
        imgJdEdges    <- imgJdEdgesFut
        videoJdEdges  <- _videoJdEdgesFut
      } yield {
        val _textJdEdges = textJdEdges
        val r = mergeJdEdges( _textJdEdges, imgJdEdges, videoJdEdges )
        LOGGER.trace(s"$logPrefix Compiled ${r.size} jd edges: text=${_textJdEdges.size} img=${imgJdEdges.size} video=${videoJdEdges.size}")
        r
      }
    }

    def finalTpl: Tree[JdTag] = tpl

    /** Запуск сборки данных jd-карточки на исполнение.
      *
      * @return Фьючерс с отрендеренными данными карточки.
      */
    def execute(): Future[MJdAdData] = {
      val _edEdgesFut = edEdgesFut
      val _finalTpl = finalTpl
      for {
        edEdges <- _edEdgesFut
      } yield {
        MJdAdData(
          template  = _finalTpl,
          edges     = edEdges,
          nodeId    = nodeId
        )
      }
    }

  }


  /** Внутренняя инфа по результату пре-рендера одного img-edge. */
  protected[this] case class MImgRenderInfo(
                                             medge        : MEdge,
                                             mimg         : MImg3,
                                             dynCallArgs  : MImgT,
                                             imgSzReal    : MSize2di
                                           )

  /** Различные варианты сборки карточек. */
  object mkJdAdDataFor {

    /** Сборщик jd-карточек для редактора карточек.
      *
      * @param mad Узел рекламной карточки.
      * @param req Реквест.
      */
    case class edit(mad: MNode)
                   (implicit req: IReqHdr) extends JdAdDataMakerBase {

      override val tpl = getNodeTpl(mad)

      override val nodeEdges = mad.edges

      override def nodeId = None

      override def imgEdgesNeedNodes = origImgsEdges

      override def imgJdEdgesFut: Future[Seq[MJdEdge]] = {
        val _mediaHostsMapFut = mediaHostsMapFut
        for {
          mediasMap           <- origImgMediasMapFut
          mediaNodesMap       <- mediaNodesMapFut
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
      * @param tpl Шаблон для рендера.
      *            Обычно -- неполный шаблон, а только главный блок.
      * @param nodeEdges Эджи рекламной карточки.
      *                  Обычно тут отфильтрованные по переданному шаблону эджи.
      * @param ctx Контекст рендера.
      * @param allowWide Допускается ли широкий рендер, если это требуется шаблоном?
      *                  Для плитке -- нет, для фокусировки -- да.
      */
    case class show(override val nodeId     : Option[String],
                    override val nodeEdges  : MNodeEdges,
                    override val tpl        : Tree[JdTag],
                    szMult                  : SzMult_t,
                    allowWide               : Boolean
                   )(implicit ctx: Context) extends JdAdDataMakerBase {

      /** Для выдачи не требуется  */
      override def imgEdgesNeedNodes = Nil

      /** Для выдачи требуются готовые картинки, подогнанные под экран устройства клиента. */
      override def imgJdEdgesFut: Future[Seq[MJdEdge]] = {
        val _imgsRenderedFut  = renderAdDocImgs
        val _mediaHostsMapFut = mediaHostsMapFut
        // TODO Сделать, чтобы mkDistMediaUrl-балансировка работала не по эджам, а только по mmedia.storage.
        //val _origImgMediasMap = origImgMediasMapFut
        for {
          imgsRendered      <- _imgsRenderedFut
          mediaHostsMap     <- _mediaHostsMapFut
        } yield {
          LOGGER.trace(s"$logPrefix ${origImgsEdges.length} img edges => rendered ${imgsRendered.size} map: [${imgsRendered.iterator.flatMap(_.medge.doc.uid).mkString(", ")}]")
          val imgPred = imgPredicate
          val iter = for {
            imgMakeRes <- imgsRendered.iterator
            edgeUid    <- imgMakeRes.medge.doc.uid
          } yield {
            MJdEdge(
              predicate   = imgPred,
              id          = edgeUid,
              url         = {
                val url = mkDistMediaUrl(dynImgUtil.imgCall(imgMakeRes.dynCallArgs), imgMakeRes.medge, mediaHostsMap)
                Some(url)
              },
              fileSrv = Some(MSrvFileInfo(
                nodeId = imgMakeRes.mimg.dynImgId.rowKeyStr,
                whPx   = Some( imgMakeRes.imgSzReal )
              ))
            )
          }
          iter.toSeq
        }
      }

      override def finalTpl: Tree[JdTag] = {
        val tpl0 = super.finalTpl
        // Удалить все crop'ы из растровых картинок, у которых задан кроп. Все растровые картинки должны бы быть уже подогнаны под карточку.
        for (jdTag <- tpl0) yield {
          val jdTagOpt2 = for {
            // Интересуют только теги с фоновой картинкой
            bgImg <- jdTag.props1.bgImg
            if bgImg.crop.nonEmpty
            // Для которых известен эдж
            edgeAndImg <- imgsEdgesMap.get( bgImg.imgEdge.edgeUid )
            // И там растровая картинка задана:
            if edgeAndImg._2.dynImgId.dynFormat.isRaster
          } yield {
            // Пересобрать тег без crop'а в bgImg:
            jdTag.withProps1(
              jdTag.props1.withBgImg(
                Some(
                  bgImg.withCrop(None)
                )
              )
            )
          }
          // Если ничего не пересобрано, значит текущий тег не требуется обновлять. Вернуть исходный jd-тег:
          jdTagOpt2.getOrElse( jdTag )
        }
      }

      /** Рендер картинок в строго необходимом размере.
        * Это подходит для выдачи, но НЕ для редактора, который оперирует оригиналами.
        */
      def renderAdDocImgs: Future[Iterable[MImgRenderInfo]] = {
        // TODO Всё становится жирным и разъезжается, если использовать запрошенный szMult.
        // Где-то на верхних уровнях szMult неправильно рассчитывается, повторяя внутри себя devPixelRatio.
        // Надо вспомнить изначальное значение szMult, либо переосмыслить его, что есть источник проблемы. Либо запретить здесь учитывать screen pxRatio.
        val szMult: SzMult_t = 1.0f

        // Сразу запустить в фоне получение MMedia для оригиналов картинок:
        val _origImgsMediasMapFut = origImgMediasMapFut

        lazy val logPrefix = s"renderAdDocImgs#${System.currentTimeMillis()}:"

        case class EdgeImgTag(medge: MEdge, mimg: MImg3, jdTag: JdTag)

        // Собрать в многоразовую коллекцию все данные по img-эджам и связанным с ними тегам:
        val edgedImgTags = {
          val iter = for {
            (medge, mimg) <- origImgsEdges.toIterator
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
            EdgeImgTag(medge, mimg, jdLoc.getLabel)
          }
          iter.toList
        }

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
            val wideNorms = BlockWidths.min.value :: BlockWidths.max.value :: wideImgMaker.WIDE_WIDTHS_PX.tail

            val iter = for {
              eit <- edgedImgTags.iterator
              isQd = eit.jdTag.name ==* MJdTagNames.QD_OP

              tgImgSzOpt = if (isQd) {
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
                  val widthPxNonNormal = jdTagWidthCssPxOpt.getOrElse {
                    // Нет заданной в теге ширины. Это не хорошо, но ошибку лучше подавить.
                    LOGGER.warn(s"$logPrefix2 Width expected for embed.img.\n jdt=${eit.jdTag}\n img-edge=${eit.medge}\n img=${eit.mimg}.\n Suppressed error, will use origWH=$origWh as scaled img.size.")
                    origWh.width
                  }
                  val jdTagWidthCssPxNorm = wideImgMaker.normWideBgSz( widthPxNonNormal, wideNorms )
                  LOGGER.trace(s"$logPrefix2 Norm.width for jd-tag: ${widthPxNonNormal}px => ${jdTagWidthCssPxNorm}px; origW=${origWh.width}px=>${origWidthNorm}px")

                  // Рассчитать размер итоговой картинки.
                  // TODO Проблема: blkImgMaker учитывает и szMult, и pxRatio. Надо разобраться, что с чем сравнивать вообще.
                  val targetImgSzPx = if (jdTagWidthCssPxNorm /* * szMult * pxRatio.pixelRatio */ < origWidthNorm) {
                    // Велик соблазн возвращать непересжатую картинку, но этого делать не стоит: она может быть огромной.
                    // Если norm-размеры совпадают, то надо пересжать без изменения orig-размера: это будет быстро и без размывания пикселей.
                    val heightCssPxNorm = jdTagWidthCssPxNorm.toDouble / origWh.width.toDouble * origWh.height
                    LOGGER.trace(s"$logPrefix2 Img need downscaling: $origWh => (${jdTagWidthCssPxNorm}x$heightCssPxNorm)")
                    MSize2di(
                      width  = jdTagWidthCssPxNorm,
                      height = heightCssPxNorm.toInt
                    )
                  } else {
                    // Картика не требует дополнительного пересжатия, можно просто вернуть оригинальный размер.
                    LOGGER.trace(s"$logPrefix2 Img resizing not needed. Returning origWh=$origWh")
                    origWh
                  }

                  targetImgSzPx
                }
              } else {
                val wh = eit.jdTag.props1.bm
                LOGGER.trace(s"$logPrefix Using block meta as img.wh: $wh")
                wh
              }

              tgImgSz <- {
                LOGGER.trace(s"$logPrefix img-edge#${eit.medge.doc.uid.orNull} qd?$isQd sz=>${tgImgSzOpt.orNull}")
                tgImgSzOpt
              }

            } yield {
              // Если есть кроп у текущей картинки, то запихнуть его в dynImgOps
              val mimg2 = eit.jdTag.props1.bgImg
                .flatMap(_.crop)
                .fold(eit.mimg) { crop =>
                  eit.mimg.withDynOps(
                    AbsCropOp(crop) :: Nil
                  )
                }

              val makeArgs = MImgMakeArgs(
                img           = mimg2,
                targetSz      = tgImgSz,
                szMult        = szMult,
                devScreenOpt  = ctx.deviceScreenOpt,
                compressMode  = Some(
                  if (isQd) CompressModes.Fg else CompressModes.Bg
                )
              )

              // Выбираем img maker исходя из конфигурации рендера.
              val maker = if (allowWide && eit.jdTag.props1.bm.exists(_.wide)) {
                wideImgMaker
              } else {
                blkImgMaker
              }

              // Дописать в результат рассчёта картинки инфу по оригинальной картинке:
              for {
                imakeRes <- maker.icompile( makeArgs )
              } yield {
                MImgRenderInfo(eit.medge, eit.mimg, imakeRes.dynCallArgs, imakeRes.szReal)
              }
            }

            iter.toList
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
    import JdTag.Implicits._
    val tplEdgeUids = tpl.deepEdgesUidsIter.toSet
    if (tplEdgeUids.isEmpty) {
      edges
    } else {
      edges.copy(
        out = edges.out.filter { medge =>
          medge.doc.uid
            .exists( tplEdgeUids.contains )
        }
      )
    }
  }

}

trait IJdAdUtilDi {
  def jdAdUtil: JdAdUtil
}
