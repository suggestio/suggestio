package io.suggest.ad.edit

import diode.{ModelRO, ModelRW}
import diode.react.ReactConnector
import io.suggest.ad.edit.m.{MAdEditFormInit, MAeRoot, MDocS}
import MDocS.MDocSFastEq
import io.suggest.jd.render.m.{MJdArgs, MJdConf, MJdCssArgs, MJdRenderArgs}
import io.suggest.primo.id.IId
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import play.api.libs.json.Json
import io.suggest.ad.edit.c._
import io.suggest.ad.edit.m.edit.color.{IBgColorPickerS, MColorPick}
import io.suggest.ad.edit.m.edit.MQdEditS
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.jd.tags._
import io.suggest.ad.edit.m.edit.pic.MPictureAh
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.MAeRoot.MAeRootFastEq
import io.suggest.ad.edit.srv.LkAdEditApiHttp
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.up.UploadApiHttp
import io.suggest.dev.MSzMults
import io.suggest.spa.{OptFastEq, StateInp}
import io.suggest.ws.pool.{WsChannelApiHttp, WsPoolAh}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ZTreeUtil._
import japgolly.univeq._

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
          isEdit  = true,
          szMult    = MSzMults.`1.0`
        )
        val tpl = mFormInit.form.template
        val edges = IId.els2idMap[EdgeUid_t, MEdgeDataJs] {
          mFormInit.form.edges
            .iterator
            .map {
              MEdgeDataJs(_)
            }
        }
        val jdCssArgs = MJdCssArgs.singleCssArgs( tpl, jdConf, edges )
        val jdCss = jdCssFactory.mkJdCss( jdCssArgs )
        MDocS(
          jdArgs = MJdArgs(
            template   = tpl,
            renderArgs = MJdRenderArgs(
              edges = edges
            ),
            jdCss      = jdCss,
            conf       = jdConf
          )
        )
      }
    )
  }

  private val rootRW = zoomRW(m => m) { (_, mroot2) => mroot2 }
  /** Используется извне, в init например. */
  val rootRO: ModelRO[MAeRoot] = rootRW

  private val confRO = zoom(_.conf)

  private val uploadApi = new UploadApiHttp(confRO)

  private val api = new LkAdEditApiHttp( confRO, uploadApi )

  private val ctxIdRO = confRO.zoom(_.ctxId)
  private val wsChannelApi = new WsChannelApiHttp(ctxIdRO)

  private val mDocSRw = zoomRW(_.doc) { _.withDoc(_) }

  private val docAh = docEditAhFactory( mDocSRw )

  private val wsPoolRW = zoomRW(_.wsPool) { _.withWsPool(_) }

  //private val delayerRW = zoomRW( _.delayer ) { _.withDelayer(_) }


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
        .selectedTagLoc
        .filter( IDocTag.treeLocByTypeFilterF(jdtName) )
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
        val css2 = jdCssFactory.mkJdCss( MJdCssArgs.singleCssArgs(tpl2, mdoc0.jdArgs.conf, mdoc0.jdArgs.renderArgs.edges) )

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

  private val mPictureAhRW = zoomRW[MPictureAh] { mroot =>
    val mdoc = mroot.doc
    MPictureAh(
      edges       = mdoc.jdArgs.renderArgs.edges,
      selectedTag = mdoc.jdArgs.selectedTagLoc.toLabelOpt,
      errorPopup  = mroot.popups.error,
      cropPopup   = mroot.popups.pictureCrop,
      histograms  = mdoc.colorsState.histograms
    )
  } { (mroot0, mPictureAh) =>
    val mdoc0 = mroot0.doc

    val mdoc1 = mdoc0
      .withJdArgs {
        // Пробросить обновлённый selected-тег в шаблон:
        val tpl2Opt = for {
          selJdt2    <- mPictureAh.selectedTag
          selJdtLoc0 <- mdoc0.jdArgs.selectedTagLoc
          // Пересобирать дерево только если теги различаются.
          if selJdtLoc0.getLabel !=* selJdt2
        } yield {
          selJdtLoc0.setLabel( selJdt2 )
        }
        val tpl2 = tpl2Opt.fold(mdoc0.jdArgs.template)(_.toTree)

        // css обновляем только после FastEq, чтобы избежать жирного перерендера без необходимости.
        // TODO Вынести этот код куда-нибудь.
        val css2 = {
          val oldCssArgs = MJdCssArgs.singleCssArgs(mdoc0.jdArgs.template, mdoc0.jdArgs.conf, mdoc0.jdArgs.renderArgs.edges)
          val newCssArgs = MJdCssArgs.singleCssArgs(tpl2, mdoc0.jdArgs.conf, mPictureAh.edges)
          if (MJdCssArgs.MJdCssArgsFastEq.eqv(oldCssArgs, newCssArgs)) {
            mdoc0.jdArgs.jdCss
          } else {
            // Что-то важное изменилось, отправляем CSS на пересборку.
            jdCssFactory.mkJdCss(
              MJdCssArgs.singleCssArgs(tpl2, mdoc0.jdArgs.conf, mPictureAh.edges )
            )
          }
        }

        // Залить всё в итоговое состояние пачкой:
        mdoc0.jdArgs.copy(
          template    = tpl2,
          renderArgs  = mdoc0.jdArgs.renderArgs.withEdges(
            mPictureAh.edges
          ),
          jdCss       = css2
        )
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
    api         = api,
    uploadApi   = uploadApi,
    modelRW     = mPictureAhRW
  )

  private val stripBgColorPickAfterAh = new ColorPickAfterStripAh( mDocSRw )

  //private val actionDelayerAh = new ActionDelayerAh(delayerRW)

  private val wsPoolAh = new WsPoolAh( wsPoolRW, wsChannelApi, this )

  private val tailAh = new TailAh( rootRW )

  /** Сборка action-handler'а в зависимости от текущего состояния. */
  override protected def actionHandler: HandlerFunction = {
    // В хвосте -- перехватчик необязательных событий.
    var acc = List[HandlerFunction](
      wsPoolAh,
      tailAh
    )

    // Если допускается выбор цвета фона текущего jd-тега, то подцепить соотв. контроллер.
    val mDocS = mDocSRw.value

    for (selTag <- mDocS.jdArgs.selectedTag) {
      selTag.rootLabel.name match {
        case MJdTagNames.STRIP =>
          acc ::= stripBgColorAh
        case MJdTagNames.QD_CONTENT =>
          acc ::= qdTagBgColorAh
        case _ => // do nothing
      }
    }

    // Управление картинками может происходить в фоне от всех, в т.ч. во время upload'а.
    acc ::= pictureAh

    // В голове -- обработчик всех основных операций на документом.
    acc ::= docAh

    // Навешиваем параллельные handler'ы.
    foldHandlers(
      composeHandlers(acc: _*),
      stripBgColorPickAfterAh
    )
  }


  // Финальные действа: подписаться на события, так желаемые контроллерами.
  wsPoolAh.initGlobalEvents()

}
