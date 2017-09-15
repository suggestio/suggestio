package io.suggest.ad.edit

import diode.ModelRW
import diode.react.ReactConnector
import io.suggest.ad.edit.m.{MAdEditFormInit, MAeRoot, MDocS}
import io.suggest.jd.render.m.{MJdArgs, MJdConf, MJdCssArgs, MJdRenderArgs}
import io.suggest.primo.id.IId
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.spa.StateInp
import play.api.libs.json.Json
import io.suggest.ad.edit.c.{ColorPickAh, DocEditAh, TailAh}
import io.suggest.ad.edit.m.edit.MColorPick
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.common.empty.OptionUtil._
import io.suggest.jd.tags.{JsonDocument, Strip}
import MColorPick.MColorPickFastEq
import io.suggest.sjs.common.spa.OptFastEq.Wrapped

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


  /** Очень жирная RW-зум-функция просто для доступа к состоянию цвета фона текущего Strip'а. */
  private val stripBgRw = mDocSRw.zoomRW { mdoc =>
    for {
      currStrip <- mdoc.jdArgs
        .selectedTag
        .filterByType[Strip]
      stripEditS <- mdoc.stripEd
    } yield {
      MColorPick(
        colorOpt    = currStrip.bgColor,
        colorsState = mdoc.colorsState,
        pickS       = stripEditS.bgColorPick
      )
    }
  } { (mdoc0, mColorAhOpt) =>
    // Что-то изменилось с моделью MColorAhOpt во время деятельности контроллера.
    // Нужно обновить текущий стрип.
    val mdoc2Opt = for {
      strip0   <- mdoc0.jdArgs.selectedTag.filterByType[Strip]
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
      mdoc0
        .withJdArgs {
          mdoc0.jdArgs.copy(
            template    = tpl2,
            jdCss       = css2,
            selectedTag = Some(strip2)
          )
        }
        .withStripEd {
          mdoc0.stripEd.map { stripEd0 =>
            stripEd0.withBgColorPick( mColorAh.pickS )
          }
        }
        .withColorsState( mColorAh.colorsState )
    }
    // Чисто теоретически возможна какая-то нештатная ситуация, но мы подавляем её в пользу исходного состояния circuit.
    mdoc2Opt.getOrElse {
      LOG.error( ErrorMsgs.UNEXPECTED_FSM_RUNTIME_ERROR, msg = s"$mdoc0 + $mColorAhOpt = $mdoc2Opt" )
      mdoc0
    }
  }

  private val stripBgColorAh = colorPickAhFactory( stripBgRw )

  private val tailAh = new TailAh(mDocSRw)

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      docAh,
      stripBgColorAh,
      tailAh
    )
  }

}
