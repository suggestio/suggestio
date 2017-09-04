package io.suggest.ad.edit

import diode.ModelRW
import diode.react.ReactConnector
import io.suggest.ad.edit.m.{MAdEditFormInit, MAdEditRoot, MDocS}
import io.suggest.jd.render.m.{MJdArgs, MJdConf, MJdCssArgs, MJdRenderArgs}
import io.suggest.primo.id.IId
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.spa.StateInp
import play.api.libs.json.Json
import io.suggest.ad.edit.c.DocEditAh
import io.suggest.jd.render.v.JdCssFactory

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:28
  * Description: Diode-circuit редактора рекламных карточек второго поколения.
  */
class LkAdEditCircuit(
                       jdCssFactory     : JdCssFactory,
                       docEditAhFactory : (ModelRW[MAdEditRoot, MDocS]) => DocEditAh[MAdEditRoot]
                     )
  extends CircuitLog[MAdEditRoot]
  with ReactConnector[MAdEditRoot]
{

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.AD_EDIT_CIRCUIT_ERROR

  override protected def initialModel: MAdEditRoot = {
    // Найти на странице текстовое поле с сериализованным состянием формы.
    val stateInp = StateInp.find().get
    val jsonStr = stateInp.value.get

    val mFormInit = Json
      .parse( jsonStr )
      .as[MAdEditFormInit]

    MAdEditRoot(
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


  override protected def actionHandler: HandlerFunction = {
    docAh
  }

}
