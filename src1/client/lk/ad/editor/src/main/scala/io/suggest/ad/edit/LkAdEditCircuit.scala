package io.suggest.ad.edit

import diode.{FastEq, ModelRO, ModelRW}
import diode.react.ReactConnector
import io.suggest.ad.edit.m._
import io.suggest.jd.render.m.{MJdArgs, MJdDataJs}
import io.suggest.log.CircuitLog
import play.api.libs.json.Json
import io.suggest.ad.edit.c._
import io.suggest.ad.edit.m.edit.{MDocS, MEditorsS, MJdDocEditS, MSlideBlocks}
import io.suggest.jd.tags._
import io.suggest.ad.edit.m.layout.MLayoutS
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.ad.edit.m.vld.MJdVldAh
import io.suggest.ad.edit.srv.LkAdEditApiHttp
import io.suggest.color.MColorData
import io.suggest.conf.ConfConst
import io.suggest.dev.MSzMults
import io.suggest.grid.GridBuilderUtilJs
import io.suggest.jd.render.c.JdAh
import io.suggest.jd.render.u.JdUtil
import io.suggest.jd.{MJdConf, MJdDoc}
import io.suggest.kv.MKvStorage
import io.suggest.spa.{DoNothingActionProcessor, FastEqUtil, OptFastEq, StateInp}
import io.suggest.ws.pool.{WsChannelApiHttp, WsPoolAh}
import io.suggest.ueq.UnivEqUtil._
import org.scalajs.dom
import io.suggest.lk.c.{ColorPickAh, IsTouchDevSwitchAh, UploadAh}
import io.suggest.lk.m.{MDeleteConfirmPopupS, color}
import io.suggest.lk.m.color.{MColorPick, MColorsState}
import io.suggest.lk.m.img.MUploadAh
import io.suggest.msg.ErrorMsgs
import io.suggest.pick.ContentTypeCheck
import scalaz.{Tree, TreeLoc}
import scalaz.std.option._
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.spa.CircuitUtil._
import io.suggest.up.UploadApiHttp
import io.suggest.ws.pool.m.MWsPoolS
import monocle.Traversal
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:28
  * Description: Diode-circuit редактора рекламных карточек второго поколения.
  */
class LkAdEditCircuit(
                       docEditAhFactory     : (ModelRW[MAeRoot, MDocS]) => DocEditAh[MAeRoot],
                     )
  extends CircuitLog[MAeRoot]
  with ReactConnector[MAeRoot]
{

  import MUploadAh.MPictureAhFastEq
  import MAeRoot.MAeRootFastEq


  /** Флаг использования валидации на клиенте.
    * Валидация на клиенте нежелательна, т.к. это очень жирный кусок js, который может
    * превносить тормоза даже в простой набор текста.
    *
    * Можно управлять этим флагам.
    *
    * final val, чтобы значение флага было на 100% известно ещё на стадии компиляции.
    */
  private final def DOC_VLD_ON_CLIENT: Boolean = scala.scalajs.LinkingInfo.developmentMode


  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.AD_EDIT_CIRCUIT_ERROR

  override protected def initialModel: MAeRoot = {
    // Найти на странице текстовое поле с сериализованным состянием формы.
    val stateInp = StateInp.find().get
    val jsonStr = stateInp.value.get

    val mFormInit = Json
      .parse( jsonStr )
      .as[MAdEditFormInit]

    MAeRoot(
      conf = {
        val conf0 = mFormInit.conf
        // Возможно, что флаг isTouchDev переопределён в localStorage.
        MKvStorage
          .get[Boolean]( ConfConst.IS_TOUCH_DEV )
          .filterNot { isTouchDev2 =>
            conf0.touchDev ==* isTouchDev2.value
          }
          .fold(conf0) { isTouchDev2 =>
            (MAdEditFormConf.touchDev set isTouchDev2.value)(conf0)
          }
      },

      doc  = {
        val jdConf = MJdConf(
          isEdit            = true,
          szMult            = MSzMults.`1.0`,
          blockPadding      = mFormInit.blockPadding,
          gridColumnsCount  = 2
        )
        val jdDataJs = MJdDataJs.fromJdData( mFormInit.adData )
        val jdArgs = MJdArgs(
          data        = jdDataJs,
          conf        = jdConf,
          jdRuntime   = JdUtil
            .mkRuntime(jdConf)
            .docs(jdDataJs.doc)
            .result,
        )
        MDocS(
          jdDoc = MJdDocEditS(
            jdArgs    = jdArgs,
            gridBuild = GridBuilderUtilJs.buildGridFromJdArgs( jdArgs ),
          ),
          editors = MEditorsS(
            // Залить гистограммы в общий словарь гистограмм, т.к. именно оттуда идёт рендер.
            colorsState = MColorsState(
              histograms = (for {
                jdEdge    <- mFormInit.adData.edges.iterator
                srvFile   <- jdEdge.fileSrv
                colorHist <- srvFile.pictureMeta.histogram
              } yield {
                srvFile.nodeId -> colorHist
              })
                .toMap,
            ),
          ),
        )
      },

      layout = MLayoutS(
        // TODO Высчитывать начальную координату на основе текущих данных вертикального скрола.
        rightPanelY = None
      )
    )
  }


  private val rootRW = zoomRW(identity(_)) { (_, mroot2) => mroot2 }
  /** Используется извне, в init например. */
  def rootRO: ModelRO[MAeRoot] = rootRW

  private val confRW = mkLensRootZoomRW(this, MAeRoot.conf)( FastEqUtil.AnyRefFastEq )

  private val uploadApi = new UploadApiHttp

  private val adEditApi = new LkAdEditApiHttp( confRW, uploadApi )

  private val ctxIdRO = mkLensZoomRO(confRW, MAdEditFormConf.ctxId)
  private val wsChannelApi = new WsChannelApiHttp(ctxIdRO)

  private val mDocSRw = mkLensRootZoomRW(this, MAeRoot.doc)( MDocS.MDocSFastEq )

  private val docAh = docEditAhFactory( mDocSRw )

  private val wsPoolRW = mkLensRootZoomRW(this, MAeRoot.wsPool)( MWsPoolS.MWsPoolSFastEq )

  private val layoutRW = mkLensRootZoomRW(this, MAeRoot.layout)( MLayoutS.MLayoutSFastEq )

  private val popupsRW = mkLensRootZoomRW(this, MAeRoot.popups)( MAePopupsS.MAePopupsSFastEq )

  private val deleteConfirmPopupRW = mkLensZoomRW(popupsRW, MAePopupsS.deleteConfirm)( OptFastEq.Wrapped(MDeleteConfirmPopupS.MDeleteConfirmPopupSFastEq) )

  private val editorsRW = mkLensZoomRW(mDocSRw, MDocS.editors)( MEditorsS.MEditorsSFastEq )
  private val slideBlocksRW = mkLensZoomRW(editorsRW, MEditorsS.slideBlocks)( MSlideBlocks.MSlideBlocksFastEq )

  private val jdDocRW = mkLensZoomRW(mDocSRw, MDocS.jdDoc)( MJdDocEditS.MJdDocEditSFastEq )
  private val jdArgsRW = mkLensZoomRW(jdDocRW, MJdDocEditS.jdArgs)( MJdArgs.MJdArgsFastEq )
  private val jdRuntimeRW = mkLensZoomRW(jdArgsRW, MJdArgs.jdRuntime)( FastEqUtil.AnyRefFastEq )

  private val isTouchDevRW = mkLensZoomRW( confRW, MAdEditFormConf.touchDev )( FastEq.AnyValEq.asInstanceOf[FastEq[Boolean]] )


  /** Класс для сборки зумма для color-picker'а. */
  private abstract class ZoomToBgColorPick {

    def getSelTagLoc(mdoc: MDocS): Option[TreeLoc[JdTag]] = {
      mdoc.jdDoc.jdArgs.selJdt
        .treeLocOpt
    }

    def getColorOpt(jdTag: JdTag): Option[MColorData] = {
      jdTag.props1.bgColor
    }

    def setColorOpt(jdTag: JdTag, colorOpt2: Option[MColorData]): JdTag = {
      JdTag.props1
        .composeLens( MJdtProps1.bgColor )
        .set( colorOpt2 )( jdTag )
    }

    /** @return ZoomRW до Option[MColorPick]. */
    def getZoom: ModelRW[MAeRoot, Option[MColorPick]] = {
      mDocSRw.zoomRW[Option[MColorPick]] { mdoc =>
        for {
          currTag <- getSelTagLoc(mdoc)
        } yield {
          color.MColorPick(
            colorOpt    = getColorOpt(currTag.getLabel),
            colorsState = mdoc.editors.colorsState,
          )
        }
      } { (mdoc0, mColorAhOpt) =>
        // Что-то изменилось с моделью MColorAhOpt во время деятельности контроллера.
        // Нужно обновить текущий стрип.
        (for {
          jdtLoc0     <- getSelTagLoc( mdoc0 )
          mColorAh    <- mColorAhOpt
        } yield {
          val strip2 = jdtLoc0.modifyLabel { s0 =>
            setColorOpt( s0, mColorAh.colorOpt )
          }
          val tpl2 = strip2.toTree
          val jdDoc2 = MJdDoc.template
            .set(tpl2)( mdoc0.jdDoc.jdArgs.data.doc )

          (
            MDocS.jdDoc
              .composeLens(MJdDocEditS.jdArgs)
              .modify(
                MJdArgs.data
                  .composeLens(MJdDataJs.doc)
                  .set(jdDoc2) andThen
                MJdArgs.jdRuntime.set {
                  DocEditAh
                    .mkJdRuntime(jdDoc2, mdoc0.jdDoc.jdArgs)
                    .result
                }
              ) andThen
            MDocS.editors
              .composeLens(MEditorsS.colorsState)
              .set( mColorAh.colorsState )
          )(mdoc0)
        })
          // Чисто теоретически возможна какая-то нештатная ситуация, но мы подавляем её в пользу исходного состояния circuit.
          .getOrElse {
            logger.error( ErrorMsgs.UNEXPECTED_FSM_RUNTIME_ERROR, msg = s"$mdoc0 + $mColorAhOpt is empty" )
            mdoc0
          }
      }(OptFastEq.Wrapped)
    }

  }

  /** Трейт для фильтрации по jd tag name. */
  private trait JdTagNameFilter extends ZoomToBgColorPick {

    def jdtName: MJdTagName

    override def getSelTagLoc(mdoc: MDocS): Option[TreeLoc[JdTag]] = {
      super.getSelTagLoc(mdoc)
        .filter( JdTag.treeLocByTypeFilterF(jdtName :: Nil) )
    }

  }

  /** Цвет тени RW. */
  private val shadowColorRW = {
    val zoomer = new ZoomToBgColorPick {
      def jdt_p1_textShadow_color_TRAV = JdTag.props1
        .composeLens( MJdtProps1.textShadow )
        .composeTraversal( Traversal.fromTraverse[Option, MJdShadow] )
        .composeLens( MJdShadow.color )

      override def getColorOpt(jdTag: JdTag): Option[MColorData] = {
        jdt_p1_textShadow_color_TRAV
          .headOption( jdTag )
          .flatten
      }

      override def setColorOpt(jdTag: JdTag, colorOpt2: Option[MColorData]): JdTag = {
        jdt_p1_textShadow_color_TRAV.set( colorOpt2 )(jdTag)
      }
    }
    zoomer.getZoom
  }


  // ---- Контроллеры

  private val layoutAh = new LayoutAh( layoutRW, mDocSRw )


  /** Контроллер настройки цвета фона стрипа. */
  private val stripBgColorAh = {
    val zoomBuilder = new ZoomToBgColorPick with JdTagNameFilter {
      override def jdtName = MJdTagNames.STRIP
    }
    new ColorPickAh(
      myMarker = Some(MJdTagNames.STRIP),
      modelRW  = zoomBuilder.getZoom
    )
  }

  /** Контроллер для цвета тени. */
  private val shadowColorAh = new ColorPickAh(
    myMarker = Some( MJdShadow.ColorMarkers.TextShadow ),
    modelRW  = shadowColorRW
  )

  /** Контроллер настройки цвета фона контента. */
  private val qdTagBgColorAh = {
    val zoomBuilder = new ZoomToBgColorPick with JdTagNameFilter {
      override def jdtName = MJdTagNames.QD_CONTENT
    }
    val p2 = new ColorPickAh(
      myMarker = Some(MJdTagNames.QD_CONTENT),
      modelRW  = zoomBuilder.getZoom
    )
    // Цвет фона контента, цвет тени контента.
    foldHandlers(p2, shadowColorAh)
  }

  private val mUploadAhRW = zoomRW[MUploadAh[Tree[JdTag]]] { mroot =>
    val mdoc = mroot.doc
    MUploadAh(
      edges       = mdoc.jdDoc.jdArgs.data.edges,
      view        = mdoc.jdDoc.jdArgs.data.doc.template,
      errorPopup  = mroot.popups.error,
      cropPopup   = mroot.popups.pictureCrop,
      histograms  = mdoc.editors.colorsState.histograms
    )
  } { (mroot0, mPictureAh) =>
    val mdoc0 = mroot0.doc
    val jdDoc0 = mdoc0.jdDoc.jdArgs.data.doc
    val isTplChanged = jdDoc0.template !===* mPictureAh.view

    val mdoc1 = if (
      isTplChanged ||
      (mPictureAh.edges !===* mdoc0.jdDoc.jdArgs.data.edges)
    ) {
      MDocS.jdDoc
        .composeLens(MJdDocEditS.jdArgs)
        .modify { jdArgs0 =>
          // Разобраться, изменился ли шаблон в реальности:
          val (jdDoc2, jdRuntime2) = if (isTplChanged) {
            // Изменился шаблон. Вернуть новый шаблон, пересобрать css
            val jdDoc1 = (MJdDoc.template set mPictureAh.view)( jdDoc0 )
            val jdRuntime11 = DocEditAh
              .mkJdRuntime(jdDoc1, jdArgs0)
              .result
            (jdDoc1, jdRuntime11)
          } else {
            // Не изменился шаблон, вернуть исходник
            (jdDoc0, jdArgs0.jdRuntime)
          }

          // Залить всё в итоговое состояние пачкой:
          jdArgs0.copy(
            data = jdArgs0.data.copy(
              doc       = jdDoc2,
              edges     = mPictureAh.edges,
            ),
            jdRuntime   = jdRuntime2
          )
        }(mdoc0)
    } else {
      // Нет изменений в документе, пропускаем всё молча.
      mdoc0
    }

    // Гистограммы лежат вне mdoc, и обновляются нечасто, поэтому тут ленивый апдейт гистограмм в состоянии:
    val mdoc2 = if (mdoc0.editors.colorsState.histograms ===* mPictureAh.histograms) {
      // Карта гистограмм не изменилась, пропускаем мимо ушей.
      mdoc1
    } else {
      MDocS.editors
        .composeLens( MEditorsS.colorsState )
        .composeLens( MColorsState.histograms )
        .set( mPictureAh.histograms )( mdoc1 )
    }

    mroot0.copy(
      doc    = mdoc2,
      popups = mroot0.popups.copy(
        pictureCrop = mPictureAh.cropPopup,
        error       = mPictureAh.errorPopup
      )
    )
  }

  /** Контроллер изображений. */
  private val uploadAh = new UploadAh(
    uploadApi             = uploadApi,
    modelRW               = mUploadAhRW,
    prepareUploadRoute    = { _ => adEditApi.prepareUploadRoute },
    contentTypeCheck      = ContentTypeCheck.OnlyImages,
    ctxIdOptRO            = confRW.zoom { conf =>
      Some(conf.ctxId)
    },
    dispatcher            = this,
  )

  private val stripBgColorPickAfterAh = new ColorPickAfterStripAh( mDocSRw )

  //private val actionDelayerAh = new ActionDelayerAh(delayerRW)

  private val wsPoolAh = new WsPoolAh( wsPoolRW, wsChannelApi, this )

  private val tailAh = new TailAh( rootRW )

  private lazy val jdVldAh = new JdVldAh(
    modelRW = zoomRW { mroot =>
      MJdVldAh(
        jdData   = mroot.doc.jdDoc.jdArgs.data,
        popups   = mroot.popups,
      )
    } { (mroot, _) =>
      // TODO Залить возможные изменения в основную модель.
      mroot
    }
  )

  /** Контроллер сохранения. */
  private val saveAh = new SaveAh(
    lkAdEditApi = adEditApi,
    confRO      = confRW,
    modelRW     = rootRW
  )


  /** Контроллер удаления. */
  private val deleteAh = new DeleteAh(
    lkAdEditApi = adEditApi,
    confRO      = confRW,
    modelRW     = deleteConfirmPopupRW
  )

  private val slideBlocksAh = new SlideBlocksAh(
    modelRW     = slideBlocksRW
  )

  private val jdAh = new JdAh(
    modelRW     = jdRuntimeRW,
  )

  private val touchSwitchAh = new IsTouchDevSwitchAh(
    modelRW = isTouchDevRW,
  )


  private val _ahsAcc0 = List[HandlerFunction](
    // В голове -- обработчик всех основных операций на документом.
    docAh,
    jdAh,
    // Управление интерфейсом -- ближе к голове.
    layoutAh,
    // Управление картинками может происходить в фоне от всех, в т.ч. во время upload'а.
    uploadAh,
    wsPoolAh,
    slideBlocksAh,
    saveAh,
    deleteAh,
    touchSwitchAh,
    tailAh
  )
  // Начальный аккамулятор параллельного связывания контроллеров параллельной обработки:
  private val _ahsParAcc0 = {
    var parAcc = List.empty[HandlerFunction]

    if (DOC_VLD_ON_CLIENT)
      parAcc ::= jdVldAh

    parAcc ::= stripBgColorPickAfterAh

    parAcc
  }
  /** Сборка action-handler'а в зависимости от текущего состояния. */
  override protected def actionHandler: HandlerFunction = {
    // TODO Тут def, т.к. ниже велосипед с динамическим выбором ColorPicker'а, где конкретный контроллер выбирается динамически.
    // В хвосте -- перехватчик необязательных событий.
    var acc = _ahsAcc0

    // Если допускается выбор цвета фона текущего jd-тега, то подцепить соотв. контроллер.
    for {
      selTagLoc <- mDocSRw.value.jdDoc.jdArgs.selJdt.treeLocOpt
    } {
      selTagLoc.getLabel.name match {
        case MJdTagNames.STRIP =>
          acc ::= stripBgColorAh
        case MJdTagNames.QD_CONTENT =>
          acc ::= qdTagBgColorAh
        case _ => // do nothing
      }
    }

    // Навешиваем параллельные handler'ы.
    foldHandlers( (composeHandlers(acc: _*) :: _ahsParAcc0): _* )
  }


  // Финальные действа: подписаться на события, так желаемые контроллерами.
  layoutAh.subscribeForScroll( dom.window, this )

  wsPoolAh.initGlobalEvents()

  // Если валидация на клиенте, то мониторить jdArgs.template на предмет изменений шаблона.
  if (DOC_VLD_ON_CLIENT) {
    subscribe(mDocSRw.zoom(_.jdDoc.jdArgs.data.doc.template)) { _ =>
      dispatch( JdDocChanged )
    }
  }

  addProcessor( DoNothingActionProcessor[MAeRoot] )
}
