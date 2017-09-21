package io.suggest.ad.edit

import diode.{ModelRO, ModelRW}
import diode.react.ReactConnector
import io.suggest.ad.edit.m.{MAdEditFormInit, MAeRoot, MDocS}
import MDocS.MDocSFastEq
import io.suggest.jd.render.m.{MJdArgs, MJdConf, MJdCssArgs, MJdRenderArgs}
import io.suggest.primo.id.IId
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.spa.{OptFastEq, StateInp}
import play.api.libs.json.Json
import io.suggest.ad.edit.c._
import io.suggest.ad.edit.m.edit.color.{IBgColorPickerS, MColorPick}
import io.suggest.ad.edit.m.edit.MQdEditS
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.jd.tags._
import io.suggest.ad.edit.m.edit.pic.MPictureAh
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.MAeRoot.MAeRootFastEq

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
        val conf = MJdConf(
          withEdit  = true,
          szMult    = 2
        )
        val tpl = mFormInit.form.template
        val jdCssArgs = MJdCssArgs.singleCssArgs( tpl, conf )
        val jdCss = jdCssFactory.mkJdCss( jdCssArgs )
        MDocS(
          jdArgs = MJdArgs(
            template   = tpl,
            renderArgs = MJdRenderArgs(
              edges = IId.els2idMap( mFormInit.form.edges )
            ),
            jdCss      = jdCss,
            conf       = conf
          )
        )
      }
    )
  }

  private val rootRW = zoomRW(m => m) { (_, mroot2) => mroot2 }
  /** Используется извне, в init например. */
  val rootRO: ModelRO[MAeRoot] = rootRW

  private val mDocSRw = zoomRW(_.doc) { _.withDoc(_) }

  private val docAh = docEditAhFactory( mDocSRw )


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
        .selectedTag
        .filter(_.jdTagName == jdtName)
    }

    mDocSRw.zoomRW[Option[MColorPick]] { mdoc =>
      for {
        currTag <- __filteredSelTag(mdoc)
        mColorCont <- doc2bgColorContF( mdoc )
      } yield {
        MColorPick(
          colorOpt    = currTag.props1.bgColor,
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
        val strip2 = jdt0.withProps1(
          jdt0.props1.withBgColor(
            mColorAh.colorOpt
          )
        )
        val tpl2 = mdoc0.jdArgs.template
          .deepUpdateOne(jdt0, strip2 :: Nil)
          .head
        val css2 = jdCssFactory.mkJdCss( MJdCssArgs.singleCssArgs(tpl2, mdoc0.jdArgs.conf) )

        val stateOuter2 = for (state <- doc2bgColorContF(mdoc0)) yield {
          state.withBgColorPick( mColorAh.pickS )
        }

        val mdoc1 = mdoc0
          .withJdArgs {
            mdoc0.jdArgs.copy(
              template    = tpl2,
              jdCss       = css2,
              selectedTag = Some(strip2)
            )
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
    _zoomToBgColorPickS[MQdEditS](MJdTagNames.QUILL_DELTA)(_.qdEdit) { _.withQdEdit(_) }
  )

  private val mPictureAhRW = zoomRW[MPictureAh] { mroot =>
    val mdoc = mroot.doc
    MPictureAh(
      files       = mdoc.files,
      edges       = mdoc.jdArgs.renderArgs.edges,
      selectedTag = mdoc.jdArgs.selectedTag,
      errorPopup  = mroot.popups.error,
      cropPopup   = mroot.popups.pictureCrop
    )
  } { (mroot0, mPictureAh) =>
    val mdoc0 = mroot0.doc

    val mdoc2 = mdoc0
      .withFiles( mPictureAh.files )
      .withJdArgs {
        // TODO Opt поменять местами две операции, кучу jdArgs.with*() заменить на один jdArgs.copy().
        val jdArgs1 = mdoc0.jdArgs
          .withRenderArgs(
            mdoc0.jdArgs.renderArgs.withEdges(
              mPictureAh.edges
            )
          )
          .withSelectedTag( mPictureAh.selectedTag )
        // Продублировать обновлённый selected-тег в шаблон и css:
        mPictureAh.selectedTag.fold(jdArgs1) { selJdt =>
          val tpl2 = jdArgs1.template
            .deepUpdateOne(mdoc0.jdArgs.selectedTag.get, selJdt :: Nil)
            .head
          jdArgs1
            .withTemplate( tpl2 )
            .withJdCss( jdCssFactory.mkJdCss( MJdCssArgs.singleCssArgs(tpl2, jdArgs1.conf) ) )
        }
      }

    mroot0.copy(
      doc    = mdoc2,
      popups = mroot0.popups.copy(
        pictureCrop = mPictureAh.cropPopup,
        error       = mPictureAh.errorPopup
      )
    )
  }

  private val pictureAh = new PictureAh( mPictureAhRW )

  private val stripBgColorPickAfterAh = new ColorPickAfterStripAh( mDocSRw )

  private val tailAh = new TailAh( rootRW )

  /** Сборка action-handler'а в зависимости от текущего состояния. */
  override protected def actionHandler: HandlerFunction = {
    // В хвосте -- перехватчик необязательных событий.
    var acc = List[HandlerFunction](
      tailAh
    )

    // Если допускается выбор цвета фона текущего jd-тега, то подцепить соотв. контроллер.
    val mDocS = mDocSRw.value
    mDocS.jdArgs.selectedTag.foreach { selTag =>
      selTag.jdTagName match {
        case MJdTagNames.STRIP =>
          acc ::= stripBgColorAh
        case MJdTagNames.QUILL_DELTA =>
          acc ::= qdTagBgColorAh
        case _ => // do nothing
      }
    }

    // В голове -- обработчик всех основных операций на документом.
    acc ::= docAh

    // Управление картинками может происходить в фоне от всех, в т.ч. во время upload'а.
    acc ::= pictureAh

    // Навешиваем параллельные handler'ы.
    foldHandlers(
      composeHandlers(acc: _*),
      stripBgColorPickAfterAh
    )
  }

}
