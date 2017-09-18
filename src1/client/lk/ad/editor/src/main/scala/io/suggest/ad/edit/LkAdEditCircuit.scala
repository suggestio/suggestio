package io.suggest.ad.edit

import diode.ModelRW
import diode.react.ReactConnector
import io.suggest.ad.edit.m.{MAdEditFormInit, MAeRoot, MDocS}
import MDocS.MDocSFastEq
import io.suggest.jd.render.m.{MJdArgs, MJdConf, MJdCssArgs, MJdRenderArgs}
import io.suggest.primo.id.IId
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.spa.{OptFastEq, StateInp}
import play.api.libs.json.Json
import io.suggest.ad.edit.c.{ColorPickAh, DocEditAh, TailAh}
import io.suggest.ad.edit.m.edit.{IBgColorPickerS, MColorPick, MQdEditS}
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.common.empty.OptionUtil._
import io.suggest.jd.tags.{IBgColorOpt, IDocTag, JsonDocument, Strip}
import MColorPick.MColorPickFastEq
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.jd.tags.qd.QdTag

import scala.reflect.ClassTag

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


  private val mDocSRw = zoomRW(_.doc) { _.withDoc(_) }

  private val docAh = docEditAhFactory( mDocSRw )


  /** Сборка RW-зума до опционального инстанса MColorPick.
    *
    * @param doc2bgColorContF Фунция доступа к опциональному модели контейнеру с полем bgColorPick.
    * @param bgColorCont2mdoc Фунция сборки нового инстанса MDocS на основе старого инстанса и обновлённого состояния StateOuter_t.
    * @tparam DocTag_t Реализация IDocTag. Например, Strip.
    * @tparam StateOuter_t Состояние редактирования текущего типа компонента, например MStripEdS.
    * @return ZoomRW до Option[MColorPick].
    */
  private def _zoomToBgColorPickS[DocTag_t <: IDocTag with IBgColorOpt { type T = DocTag_t }: ClassTag,
                                  StateOuter_t <: IBgColorPickerS { type T = StateOuter_t }]
                                 (doc2bgColorContF: MDocS => Option[StateOuter_t])
                                 (bgColorCont2mdoc: (MDocS, Option[StateOuter_t]) => MDocS) = {
    mDocSRw.zoomRW[Option[MColorPick]] { mdoc =>
      for {
        currTag <- mdoc.jdArgs
          .selectedTag
          .filterByType[DocTag_t]
        mColorCont <- doc2bgColorContF( mdoc )
      } yield {
        MColorPick(
          colorOpt    = currTag.bgColor,
          colorsState = mdoc.colorsState,
          pickS       = mColorCont.bgColorPick
        )
      }
    } { (mdoc0, mColorAhOpt) =>
      // Что-то изменилось с моделью MColorAhOpt во время деятельности контроллера.
      // Нужно обновить текущий стрип.
      val mdoc2Opt = for {
        strip0   <- mdoc0.jdArgs.selectedTag.filterByType[DocTag_t]
        mColorAh <- mColorAhOpt
      } yield {
        val strip2 = strip0.withBgColor(
          mColorAh.colorOpt
        )
        val tpl2 = mdoc0.jdArgs.template
          .deepUpdateOne(strip0, strip2 :: Nil)
          .head
          .asInstanceOf[JsonDocument]
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
    _zoomToBgColorPickS[Strip, MStripEdS](_.stripEd) { _.withStripEd(_) }
  )

  /** Контроллер настройки цвета фона контента. */
  private val qdTagBgColorAh = colorPickAhFactory(
    _zoomToBgColorPickS[QdTag, MQdEditS](_.qdEdit) { _.withQdEdit(_) }
  )


  private val tailAh = new TailAh(mDocSRw)

  /** Сборка action-handler'а в зависимости от текущего состояния. */
  override protected def actionHandler: HandlerFunction = {
    // В хвосте -- перехватчик необязательных событий.
    var acc = List[HandlerFunction](
      tailAh
    )

    // Если допускается выбор цвета фона текущего jd-тега, то подцепить соотв. контроллер.
    val mDocS = mDocSRw.value
    mDocS.jdArgs.selectedTag.foreach {
      case _: Strip => acc ::= stripBgColorAh
      case _: QdTag => acc ::= qdTagBgColorAh
      case _ => // do nothing
    }

    // В голове -- обработчик всех основных операций на документом.
    acc ::= docAh

    composeHandlers( acc: _* )
  }

}
