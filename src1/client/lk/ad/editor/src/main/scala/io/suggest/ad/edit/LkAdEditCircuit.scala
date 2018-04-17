package io.suggest.ad.edit

import diode.{ModelRO, ModelRW}
import diode.react.ReactConnector
import io.suggest.ad.edit.m._
import io.suggest.jd.render.m.{MJdArgs, MJdCssArgs}
import io.suggest.sjs.common.log.CircuitLog
import play.api.libs.json.Json
import io.suggest.ad.edit.c._
import io.suggest.ad.edit.m.edit.color.{IBgColorPickerS, MColorPick, MColorsState}
import io.suggest.ad.edit.m.edit.MQdEditS
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.jd.tags._
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.layout.MLayoutS
import io.suggest.ad.edit.m.vld.MJdVldAh
import io.suggest.ad.edit.srv.LkAdEditApiHttp
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.up.UploadApiHttp
import io.suggest.dev.MSzMults
import io.suggest.jd.MJdConf
import io.suggest.spa.{OptFastEq, StateInp}
import io.suggest.ws.pool.{WsChannelApiHttp, WsPoolAh}
import io.suggest.ueq.UnivEqUtil._
import org.scalajs.dom
import io.suggest.lk.c.PictureAh
import io.suggest.lk.m.img.MPictureAh
import io.suggest.msg.ErrorMsgs
import scalaz.Tree
import io.suggest.scalaz.ZTreeUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:28
  * Description: Diode-circuit редактора рекламных карточек второго поколения.
  */
class LkAdEditCircuit(
                       jdCssFactory         : JdCssFactory,
                       docEditAhFactory     : (ModelRW[MAeRoot, MDocS]) => DocEditAh[MAeRoot],
                       colorPickAhFactory   : (ModelRW[MAeRoot, Option[MColorPick]] => ColorPickAh[MAeRoot])
                     )
  extends CircuitLog[MAeRoot]
  with ReactConnector[MAeRoot]
{

  import MPictureAh.MPictureAhFastEq
  import MAeRoot.MAeRootFastEq
  import MDocS.MDocSFastEq
  import io.suggest.ws.pool.m.MWsPoolS.MWsPoolSFastEq
  import MLayoutS.MLayoutSFastEq
  import io.suggest.ad.edit.m.pop.MAePopupsS.MAePopupsSFastEq
  import io.suggest.ad.edit.m.layout.MSlideBlocks.MSlideBlocksFastEq


  /** Флаг использования валидации на клиенте.
    * Валидация на клиенте нежелательна, т.к. это очень жирный кусок js, который может
    * превносить тормоза даже в простой набор текста.
    *
    * Можно управлять этим флагам.
    *
    * final val, чтобы значение флага было на 100% известно ещё на стадии компиляции.
    */
  final val DOC_VLD_ON_CLIENT: Boolean = scala.scalajs.LinkingInfo.developmentMode


  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.AD_EDIT_CIRCUIT_ERROR

  override protected def initialModel: MAeRoot = {
    // Найти на странице текстовое поле с сериализованным состянием формы.
    val stateInp = StateInp.find().get
    val jsonStr = stateInp.value.get

    val mFormInit = Json
      .parse( jsonStr )
      .as[MAdEditFormInit]

    MAeRoot(
      conf = mFormInit.conf,

      doc  = {
        val jdConf = MJdConf(
          isEdit            = true,
          szMult            = MSzMults.`1.0`,
          blockPadding      = mFormInit.blockPadding,
          gridColumnsCount  = 2
        )
        val tpl = mFormInit.adData.template
        val edges = mFormInit.adData.edgesMap
          .mapValues( MEdgeDataJs(_) )
        val jdCssArgs = MJdCssArgs.singleCssArgs( tpl, jdConf )
        val jdCss = jdCssFactory.mkJdCss( jdCssArgs )
        MDocS(
          jdArgs = MJdArgs(
            template   = tpl,
            edges      = edges,
            jdCss      = jdCss,
            conf       = jdConf
          ),
          // Залить гистограммы в общий словарь гистограмм, т.к. именно оттуда идёт рендер.
          colorsState = MColorsState(
            histograms = {
              val iter = for {
                jdEdge    <- mFormInit.adData.edgesMap.valuesIterator
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
  val rootRO: ModelRO[MAeRoot] = rootRW

  private val confRO = zoom(_.conf)

  private val uploadApi = new UploadApiHttp(confRO)

  private val adEditApi = new LkAdEditApiHttp( confRO, uploadApi )

  private val ctxIdRO = confRO.zoom(_.ctxId)
  private val wsChannelApi = new WsChannelApiHttp(ctxIdRO)

  private val mDocSRw = zoomRW(_.doc) { _.withDoc(_) }

  private val docAh = docEditAhFactory( mDocSRw )

  private val wsPoolRW = zoomRW(_.wsPool) { _.withWsPool(_) }

  private val layoutRW = zoomRW(_.layout) { _.withLayout(_) }

  private val popupsRW = zoomRW(_.popups) { _.withPopups(_) }

  private val deleteConfirmPopupRW = popupsRW.zoomRW(_.deleteConfirm) { _.withDeleteConfirm(_) }

  private val slideBlocksRW = mDocSRw.zoomRW(_.slideBlocks) { _.withSlideBlocks(_) }


  // ---- Контроллеры

  private val layoutAh = new LayoutAh( layoutRW, mDocSRw )

  /** Сборка RW-зума до опционального инстанса MColorPick.
    *
    * @param doc2bgColorContF Фунция доступа к опциональному модели контейнеру с полем bgColorPick.
    * @param bgColorCont2mdoc Фунция сборки нового инстанса MDocS на основе старого инстанса и обновлённого состояния StateOuter_t.
    * @tparam StateOuter_t Состояние редактирования текущего типа компонента, например MStripEdS.
    * @return ZoomRW до Option[MColorPick].
    */
  private def _zoomToBgColorPickS[StateOuter_t <: IBgColorPickerS { type T = StateOuter_t }]
                                 (jdtName: MJdTagName)
                                 (doc2bgColorContF: MDocS => Option[StateOuter_t])
                                 (bgColorCont2mdoc: (MDocS, Option[StateOuter_t]) => MDocS) = {
    def __filteredSelTag(mdoc: MDocS) = {
      mdoc.jdArgs
        .selJdt.treeLocOpt
        .filter( JdTag.treeLocByTypeFilterF(jdtName) )
    }

    mDocSRw.zoomRW[Option[MColorPick]] { mdoc =>
      for {
        currTag <- __filteredSelTag(mdoc)
        mColorCont <- doc2bgColorContF( mdoc )
      } yield {
        MColorPick(
          colorOpt    = currTag.getLabel.props1.bgColor,
          colorsState = mdoc.colorsState,
          pickS       = mColorCont.bgColorPick
        )
      }
    } { (mdoc0, mColorAhOpt) =>
      // Что-то изменилось с моделью MColorAhOpt во время деятельности контроллера.
      // Нужно обновить текущий стрип.
      val mdoc2Opt = for {
        jdt0     <- __filteredSelTag( mdoc0 )
        mColorAh <- mColorAhOpt
      } yield {
        val strip2 = jdt0.modifyLabel { s0 =>
          s0.withProps1(
            s0.props1.withBgColor(
              mColorAh.colorOpt
            )
          )
        }
        val tpl2 = strip2.toTree
        val css2 = jdCssFactory.mkJdCss( MJdCssArgs.singleCssArgs(tpl2, mdoc0.jdArgs.conf) )

        val stateOuter2 = for (state <- doc2bgColorContF(mdoc0)) yield {
          state.withBgColorPick( mColorAh.pickS )
        }

        val mdoc1 = mdoc0
          .withJdArgs {
            mdoc0.jdArgs
              .withTemplate( tpl2 )
              .withJdCss( css2 )
          }
          .withColorsState( mColorAh.colorsState )
        bgColorCont2mdoc( mdoc1, stateOuter2 )
      }
      // Чисто теоретически возможна какая-то нештатная ситуация, но мы подавляем её в пользу исходного состояния circuit.
      mdoc2Opt.getOrElse {
        LOG.error( ErrorMsgs.UNEXPECTED_FSM_RUNTIME_ERROR, msg = s"$mdoc0 + $mColorAhOpt = $mdoc2Opt" )
        mdoc0
      }
    }(OptFastEq.Wrapped)
  }

  /** Контроллер настройки цвета фона стрипа. */
  private val stripBgColorAh = colorPickAhFactory(
    _zoomToBgColorPickS[MStripEdS](MJdTagNames.STRIP)(_.stripEd) { _.withStripEd(_) }
  )

  /** Контроллер настройки цвета фона контента. */
  private val qdTagBgColorAh = colorPickAhFactory(
    _zoomToBgColorPickS[MQdEditS](MJdTagNames.QD_CONTENT)(_.qdEdit) { _.withQdEdit(_) }
  )

  private val mPictureAhRW = zoomRW[MPictureAh[Tree[JdTag]]] { mroot =>
    val mdoc = mroot.doc
    MPictureAh(
      edges       = mdoc.jdArgs.edges,
      view        = mdoc.jdArgs.template,
      errorPopup  = mroot.popups.error,
      cropPopup   = mroot.popups.pictureCrop,
      histograms  = mdoc.colorsState.histograms,
      cropContSz  = mdoc.jdArgs.selJdt.treeLocOpt
        .flatMap(_.getLabel.props1.bm)
    )
  } { (mroot0, mPictureAh) =>
    val mdoc0 = mroot0.doc
    val tpl0 = mdoc0.jdArgs.template
    val isTplChanged = tpl0 !===* mPictureAh.view

    val mdoc1 = if (isTplChanged || (mPictureAh.edges !===* mdoc0.jdArgs.edges)) {
      mdoc0
        .withJdArgs {
          // Разобраться, изменился ли шаблон в реальности:
          val (tpl2, css2) = if (isTplChanged) {
            // Изменился шаблон. Вернуть новый шаблон, пересобрать css
            val tpl1 = mPictureAh.view
            val css1 = jdCssFactory.mkJdCss(
              MJdCssArgs.singleCssArgs(tpl1, mdoc0.jdArgs.conf )
            )
            (tpl1, css1)
          } else {
            // Не изменился шаблон, вернуть исходник
            (tpl0, mdoc0.jdArgs.jdCss)
          }

          // Залить всё в итоговое состояние пачкой:
          mdoc0.jdArgs.copy(
            template    = tpl2,
            edges       = mPictureAh.edges,
            jdCss       = css2
          )
        }
    } else {
      // Нет изменений в документе, пропускаем всё молча.
      mdoc0
    }

    // Гистограммы лежат вне mdoc, и обновляются нечасто, поэтому тут ленивый апдейт гистограмм в состоянии:
    val mdoc2 = if (mdoc0.colorsState.histograms ===* mPictureAh.histograms) {
      // Карта гистограмм не изменилась, пропускаем мимо ушей.
      mdoc1
    } else {
      mdoc1.withColorsState(
        mdoc1.colorsState.withHistograms(
          mPictureAh.histograms
        )
      )
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
      val jdArgs = mroot.doc.jdArgs
      MJdVldAh(
        template = jdArgs.template,
        edges    = jdArgs.edges,
        popups   = mroot.popups
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
    subscribe(mDocSRw.zoom(_.jdArgs.template)) { _ =>
      dispatch( JdDocChanged )
    }
  }

}
