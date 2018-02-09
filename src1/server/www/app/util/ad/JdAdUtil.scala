package util.ad

import javax.inject.{Inject, Named, Singleton}

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
import util.cdn.{CdnUtil, DistUtil}
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
                          distUtil                    : DistUtil,
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
  def prepareImgEdges(edges: MNodeEdges): Seq[(MEdge, MImg3)] = {
    edges
      // TODO Надо ли тут Bg-предикат? по идее, он устарел ещё до ввода jd-редактора.
      .withPredicateIter( imgPredicate, MPredicates.Bg )
      .map { medge =>
        medge -> MImg3(medge)
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


  /** Подготовка dist-данных по хостам. на которых живут разные медиа-файлы.
    *
    * @param medias Медиа-карты.
    * @return КАрта хостов.
    */
  // TODO Надо это? Тут вызов другого метода, по сути.
  def prepareDistMediaHosts(medias: Map[String, MMedia]): Future[Map[String, Seq[MHostInfo]]] = {
    distUtil.medias2hosts( medias.values )
  }


  /** Собрать ссылку на media-ресурс с учётом dist и cdn.
    * БОльшая часть метода не привязана к картинки, только генератор ссылки.
    *
    * @param call Исходная внутренняя ссылка.
    * @param medge Исходный MEdge.
    * @param mediaHosts Карта хостов.
    * @return Строка URL для рендера в HTML-документе.
    */
  def mkDistMediaUrl(call: Call, medge: MEdge, mediaHosts: Map[String, Seq[MHostInfo]])
                    (implicit req: IReqHdr): String = {
    val hostInfoOpt = medge.nodeIds
      .iterator
      .flatMap(mediaHosts.get)
      .flatten
      .toStream
      .headOption

    def logPrefix = s"mkDistMediaUrl(#${medge.doc.uid.orNull},$call):"

    hostInfoOpt
      .fold[String] {
        // Вообще, это плохо, если нет хостнейма для media. Это значит, что-то не так.
        LOGGER.warn(s"$logPrefix Media host-name missing for edge $medge")
        call.url
      } { hostInfo =>
        LOGGER.trace(s"$logPrefix Using host=$hostInfo for media-edge $medge")
        cdnUtil.distNodeCdnUrl( hostInfo, call)
      }
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
                         )(implicit req: IReqHdr): Seq[MJdEdge] = {
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

    // Собираем картинки, используемые в карточке:
    lazy val imgsEdges = {
      val ie = prepareImgEdges( nodeEdges )
      LOGGER.trace(s"$logPrefix Found ${ie.size} img.edges: ${ie.iterator.map(_._2.dynImgId.fileName).mkString(", ")}")
      ie
    }

    // Собрать связанные инстансы MMedia
    lazy val imgOrigsMediasMapFut = prepareImgMedias( imgsEdges )

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
    def mediaHostsMapFut = imgOrigsMediasMapFut
      .flatMap { prepareDistMediaHosts }

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

      override def imgEdgesNeedNodes = imgsEdges

      override def imgJdEdgesFut: Future[Seq[MJdEdge]] = {
        val _mediaHostsMapFut = mediaHostsMapFut
        for {
          mediasMap           <- imgOrigsMediasMapFut
          mediaNodesMap       <- mediaNodesMapFut
          imgMedia2hostsMap   <- _mediaHostsMapFut
        } yield {
          LOGGER.trace(s"$logPrefix Found ${mediasMap.size} linked img medias.")
          mkJdImgEdgesForEdit(
            imgsEdges   = imgsEdges,
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
      */
    case class show(override val nodeId     : Option[String],
                    override val nodeEdges  : MNodeEdges,
                    override val tpl        : Tree[JdTag],
                    szMult                  : SzMult_t,
                    allowWide               : Boolean
                   )(implicit ctx: Context) extends JdAdDataMakerBase {
      import ctx.request

      /** Для выдачи не требуется  */
      override def imgEdgesNeedNodes = Nil

      /** Для выдачи требуются готовые картинки, подогнанные под экран устройства клиента. */
      override def imgJdEdgesFut: Future[Seq[MJdEdge]] = {
        val _imgsRenderedFut  = renderAdDocImgs(tpl, imgsEdges, szMult, ctx.deviceScreenOpt, allowWide)
        val _mediaHostsMapFut = mediaHostsMapFut
        for {
          imgsRendered      <- _imgsRenderedFut
          mediaHostsMap     <- _mediaHostsMapFut
        } yield {
          LOGGER.trace(s"$logPrefix ${imgsEdges.length} img edges => rendered ${imgsRendered.size} map: [${imgsRendered.iterator.flatMap(_.medge.doc.uid).mkString(", ")}]")
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
        // Удалить все crop'ы из содержимого, т.к. все картинки уже подогнаны под карточку.
        for (jdTag <- tpl0) yield {
          jdTag.withProps1(
            jdTag.props1.withBgImg(
              for (bgImg <- jdTag.props1.bgImg) yield {
                bgImg.withCrop(None)
              }
            )
          )
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


  /** Рендер картинок в строго необходимом размере.
    * Это подходит для выдачи, но НЕ для редактора, который оперирует оригиналами.
    *
    * @param jdDoc Jd-шаблон.
    * @param imgsEdges Только img-эджи, с подвязанными к ним изображениями.
    * @param devScreenOpt Данные по экрану клиентского устройства.
    * @param allowWide Допускается ли широкий рендер, если это требуется шаблоном?
    *                  Для плитке -- нет, для фокусировки -- да.
    * @return Фьючерс с картой MakeResult'ов для эджей.
    */
  def renderAdDocImgs(jdDoc         : Tree[JdTag],
                      imgsEdges     : Traversable[(MEdge, MImg3)],
                      szMult        : Float,
                      devScreenOpt  : Option[DevScreen],
                      allowWide     : Boolean
                     ): Future[Iterable[MImgRenderInfo]] = {

    lazy val logPrefix = s"renderAdDocImgs()#${System.currentTimeMillis()}:"

    case class EdgeImgTag(medge: MEdge, mimg: MImg3, jdTag: JdTag)

    // Собрать в многоразовую коллекцию все данные по img-эджам и связанным с ними тегам:
    val edgedImgTags = {
      val iter = for {
        (medge, mimg) <- imgsEdges.toIterator
        edgeUid       <- medge.doc.uid
        jdLoc         <- jdDoc
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
      embedOrigImgsMap <- mMediasCache.multiGetMap(
        edgedImgTags
          .iterator
          .filter(_.jdTag.name ==* MJdTagNames.QD_OP)
          .map(_.mimg.original.dynImgId.mediaId)
          .toSet
      )

      // Продолжить обход списка эджей, создав фьючерс результата
      results <- Future.sequence {
        val iter = for {
          eit <- edgedImgTags.iterator
          isQd = eit.jdTag.name ==* MJdTagNames.QD_OP

          tgImgSzOpt = if (isQd) {
            for {
              mmediaOrig  <- embedOrigImgsMap.get( eit.mimg.original.dynImgId.mediaId )
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
              LOGGER.trace(s"$logPrefix embed.img#${eit.mimg.dynImgId.rowKeyStr} edge#${eit.medge.doc.uid.orNull} width=>${jdTagWidthCssPxOpt.orNull}")

              // Картинка есть. Но надо разобраться, надо ли её ресайзить.
              val origWidthNorm = wideImgMaker.normWideWidthBgSz( origWh.width )

              // Узнать точно, какую ширину требуется получить на выходе.
              // Маловероятно, что в jdTag отсутствует ширина, но всё же отрабатываем и эту ситуацию.
              val widthPxNonNormal = jdTagWidthCssPxOpt.fold {
                // Нет заданной в теге ширины. Это не хорошо, но ошибку лучше подавить.
                LOGGER.warn(s"$logPrefix Width expected for embed.img.\n jdt=${eit.jdTag}\n img-edge=${eit.medge}\n img=${eit.mimg}.\n Suppressed error, will use origWH=$origWh as scaled img.size.")
                origWh.width
              } { jdTagWidthCssPx =>
                // Есть заданная ширина для отображения картинки. Нормировать её по wide-шкале.
                jdTagWidthCssPx
              }

              val jdTagWidthCssPxNorm = wideImgMaker.normWideWidthBgSz( widthPxNonNormal )

              // Рассчитать размер итоговой картинки.
              val targetImgSzPx = if (jdTagWidthCssPxNorm < origWidthNorm) {
                // Велик соблазн возвращать непересжатую картинку, но этого делать не стоит: она может быть огромной.
                // Если norm-размеры совпадают, то надо пересжать без изменения orig-размера: это будет быстро и без размывания пикселей.
                val heightCssPxNorm = jdTagWidthCssPxNorm.toDouble / origWh.width.toDouble * origWh.height
                MSize2di(
                  width = jdTagWidthCssPxNorm,
                  height = heightCssPxNorm.toInt
                )
              } else {
                // Картика не требует дополнительного пересжатия, можно просто вернуть оригинальный размер.
                origWh
              }

              targetImgSzPx
            }
          } else {
            eit.jdTag.props1.bm
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
            blockMeta     = tgImgSz,
            szMult        = 1.0f,
            devScreenOpt  = devScreenOpt,
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

trait IJdAdUtilDi {
  def jdAdUtil: JdAdUtil
}
