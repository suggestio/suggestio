package io.suggest.ad.edit

import diode.{ModelRO, ModelRW}
import diode.react.ReactConnector
import io.suggest.ad.edit.m._
import io.suggest.jd.render.m.{MJdArgs, MJdDataJs, MJdRuntime}
import io.suggest.sjs.common.log.CircuitLog
import play.api.libs.json.Json
import io.suggest.ad.edit.c._
import io.suggest.jd.tags._
import io.suggest.ad.edit.m.layout.{MLayoutS, MSlideBlocks}
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.ad.edit.m.vld.MJdVldAh
import io.suggest.ad.edit.srv.LkAdEditApiHttp
import io.suggest.color.MColorData
import io.suggest.up.UploadApiHttp
import io.suggest.dev.MSzMults
import io.suggest.jd.{MJdConf, MJdDoc}
import io.suggest.spa.{OptFastEq, StateInp}
import io.suggest.ws.pool.{WsChannelApiHttp, WsPoolAh}
import io.suggest.ueq.UnivEqUtil._
import org.scalajs.dom
import io.suggest.lk.c.{ColorPickAh, PictureAh}
import io.suggest.lk.m.{MDeleteConfirmPopupS, color}
import io.suggest.lk.m.color.{MColorPick, MColorsState}
import io.suggest.lk.m.img.MPictureAh
import io.suggest.msg.ErrorMsgs
import scalaz.{Tree, TreeLoc}
import scalaz.std.option._
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.spa.CircuitUtil._
import io.suggest.ws.pool.m.MWsPoolS
import monocle.Traversal

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

  import MPictureAh.MPictureAhFastEq
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

    val jdDataJs = MJdDataJs( mFormInit.adData )

    MAeRoot(
      conf = mFormInit.conf,

      doc  = {
        val jdConf = MJdConf(
          isEdit            = true,
          szMult            = MSzMults.`1.0`,
          blockPadding      = mFormInit.blockPadding,
          gridColumnsCount  = 2
        )
        MDocS(
          jdArgs = MJdArgs(
            data        = jdDataJs,
            conf        = jdConf,
            jdRuntime   = MJdRuntime.make(
              tpls   = jdDataJs.doc.template :: Nil,
              jdConf = jdConf,
            )
          ),
          // Залить гистограммы в общий словарь гистограмм, т.к. именно оттуда идёт рендер.
          colorsState = MColorsState(
            histograms = {
              val iter = for {
                jdEdge    <- mFormInit.adData.edges.iterator
                srvFile   <- jdEdge.fileSrv
                colorHist <- srvFile.colors
              } yield {
                srvFile.nodeId -> colorHist
              }
              iter.toMap
            }
          )
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

  private val confRO = zoom(_.conf)

  private val uploadApi = new UploadApiHttp(confRO)

  private val adEditApi = new LkAdEditApiHttp( confRO, uploadApi )

  private val ctxIdRO = mkLensZoomRO(confRO, MAdEditFormConf.ctxId)
  private val wsChannelApi = new WsChannelApiHttp(ctxIdRO)

  private val mDocSRw = mkLensRootZoomRW(this, MAeRoot.doc)( MDocS.MDocSFastEq )

  private val docAh = docEditAhFactory( mDocSRw )

  private val wsPoolRW = mkLensRootZoomRW(this, MAeRoot.wsPool)( MWsPoolS.MWsPoolSFastEq )

  private val layoutRW = mkLensRootZoomRW(this, MAeRoot.layout)( MLayoutS.MLayoutSFastEq )

  private val popupsRW = mkLensRootZoomRW(this, MAeRoot.popups)( MAePopupsS.MAePopupsSFastEq )

  private val deleteConfirmPopupRW = mkLensZoomRW(popupsRW, MAePopupsS.deleteConfirm)( OptFastEq.Wrapped(MDeleteConfirmPopupS.MDeleteConfirmPopupSFastEq) )

  private val slideBlocksRW = mkLensZoomRW(mDocSRw, MDocS.slideBlocks)( MSlideBlocks.MSlideBlocksFastEq )

  /** Класс для сборки зумма для color-picker'а. */
  private abstract class ZoomToBgColorPick {

    def getSelTagLoc(mdoc: MDocS): Option[TreeLoc[JdTag]] = {
      mdoc.jdArgs.selJdt
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
            colorsState = mdoc.colorsState,
          )
        }
      } { (mdoc0, mColorAhOpt) =>
        // Что-то изменилось с моделью MColorAhOpt во время деятельности контроллера.
        // Нужно обновить текущий стрип.
        val mdoc2Opt = for {
          jdt0     <- getSelTagLoc( mdoc0 )
          mColorAh <- mColorAhOpt
        } yield {
          val strip2 = jdt0.modifyLabel { s0 =>
            setColorOpt( s0, mColorAh.colorOpt )
          }
          val tpl2 = strip2.toTree

          (
            MDocS.jdArgs.modify(
              MJdArgs.data
                .composeLens(MJdDataJs.doc)
                .composeLens(MJdDoc.template)
                .set( tpl2 ) andThen
              MJdArgs.jdRuntime.set( MJdRuntime.make(tpl2 :: Nil, mdoc0.jdArgs.conf) )
            ) andThen
            MDocS.colorsState.set( mColorAh.colorsState )
          )(mdoc0)
        }
        // Чисто теоретически возможна какая-то нештатная ситуация, но мы подавляем её в пользу исходного состояния circuit.
        mdoc2Opt.getOrElse {
          LOG.error( ErrorMsgs.UNEXPECTED_FSM_RUNTIME_ERROR, msg = s"$mdoc0 + $mColorAhOpt = $mdoc2Opt" )
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
        .filter( JdTag.treeLocByTypeFilterF(jdtName) )
    }

  }

  /** Цвет тени RW. */
  private val shadowColorRW = {
    val zoomer = new ZoomToBgColorPick {
      lazy val jdt_p1_textShadow_color_TRAV = JdTag.props1
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

  private val mPictureAhRW = zoomRW[MPictureAh[Tree[JdTag]]] { mroot =>
    val mdoc = mroot.doc
    MPictureAh(
      edges       = mdoc.jdArgs.data.edges,
      view        = mdoc.jdArgs.data.doc.template,
      errorPopup  = mroot.popups.error,
      cropPopup   = mroot.popups.pictureCrop,
      histograms  = mdoc.colorsState.histograms
    )
  } { (mroot0, mPictureAh) =>
    val mdoc0 = mroot0.doc
    val tpl0 = mdoc0.jdArgs.data.doc.template
    val isTplChanged = tpl0 !===* mPictureAh.view

    val mdoc1 = if (isTplChanged || (mPictureAh.edges !===* mdoc0.jdArgs.data.edges)) {
      MDocS.jdArgs.set {
        // Разобраться, изменился ли шаблон в реальности:
        val (tpl2, jdRuntime2) = if (isTplChanged) {
          // Изменился шаблон. Вернуть новый шаблон, пересобрать css
          val tpl1 = mPictureAh.view
          val jdRuntime1 = MJdRuntime.make(tpl1 :: Nil, mdoc0.jdArgs.conf)
          (tpl1, jdRuntime1)
        } else {
          // Не изменился шаблон, вернуть исходник
          (tpl0, mdoc0.jdArgs.jdRuntime)
        }

        // Залить всё в итоговое состояние пачкой:
        mdoc0.jdArgs.copy(
          data = mdoc0.jdArgs.data.copy(
            doc = mdoc0.jdArgs.data.doc.copy(
              template = tpl2,
            ),
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
    val mdoc2 = if (mdoc0.colorsState.histograms ===* mPictureAh.histograms) {
      // Карта гистограмм не изменилась, пропускаем мимо ушей.
      mdoc1
    } else {
      MDocS.colorsState
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
  private val pictureAh = new PictureAh(
    uploadApi             = uploadApi,
    modelRW               = mPictureAhRW,
    prepareUploadRoute    = { _ => adEditApi.prepareUploadRoute }
  )

  private val stripBgColorPickAfterAh = new ColorPickAfterStripAh( mDocSRw )

  //private val actionDelayerAh = new ActionDelayerAh(delayerRW)

  private val wsPoolAh = new WsPoolAh( wsPoolRW, wsChannelApi, this )

  private val tailAh = new TailAh( rootRW )

  private lazy val jdVldAh = new JdVldAh(
    modelRW = zoomRW { mroot =>
      MJdVldAh(
        jdData   = mroot.doc.jdArgs.data,
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
    confRO      = confRO,
    modelRW     = rootRW
  )


  /** Контроллер удаления. */
  private val deleteAh = new DeleteAh(
    lkAdEditApi = adEditApi,
    confRO      = confRO,
    modelRW     = deleteConfirmPopupRW
  )

  private val slideBlocksAh = new SlideBlocksAh(
    modelRW     = slideBlocksRW
  )

  /** Сборка action-handler'а в зависимости от текущего состояния. */
  override protected def actionHandler: HandlerFunction = {
    // В хвосте -- перехватчик необязательных событий.
    var acc = List[HandlerFunction](
      wsPoolAh,
      slideBlocksAh,
      saveAh,
      deleteAh,
      tailAh
    )

    // Если допускается выбор цвета фона текущего jd-тега, то подцепить соотв. контроллер.
    val mDocS = mDocSRw.value

    for (selTagLoc <- mDocS.jdArgs.selJdt.treeLocOpt) {
      selTagLoc.getLabel.name match {
        case MJdTagNames.STRIP =>
          acc ::= stripBgColorAh
        case MJdTagNames.QD_CONTENT =>
          acc ::= qdTagBgColorAh
        case _ => // do nothing
      }
    }

    // Управление картинками может происходить в фоне от всех, в т.ч. во время upload'а.
    acc ::= pictureAh

    // Управление интерфейсом -- ближе к голове.
    acc ::= layoutAh

    // В голове -- обработчик всех основных операций на документом.
    acc ::= docAh


    // -----------------------------------------------------------
    // Аккамулятор параллельного связывания.
    var parAcc = List.empty[HandlerFunction]

    if (DOC_VLD_ON_CLIENT)
      parAcc ::= jdVldAh

    parAcc ::= stripBgColorPickAfterAh
    parAcc ::= composeHandlers(acc: _*)

    // Навешиваем параллельные handler'ы.
    foldHandlers( parAcc: _* )
  }


  // Финальные действа: подписаться на события, так желаемые контроллерами.
  layoutAh.subscribeForScroll( dom.window, this )

  wsPoolAh.initGlobalEvents()

  // Если валидация на клиенте, то мониторить jdArgs.template на предмет изменений шаблона.
  if (DOC_VLD_ON_CLIENT) {
    subscribe(mDocSRw.zoom(_.jdArgs.data.doc.template)) { _ =>
      dispatch( JdDocChanged )
    }
  }

}
