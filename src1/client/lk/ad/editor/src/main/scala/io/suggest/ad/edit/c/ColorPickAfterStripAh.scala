package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m._
import io.suggest.css.Css
import io.suggest.jd.render.m.IJdAction
import io.suggest.jd.tags.MJdTagNames
import io.suggest.common.html.HtmlConstants.{COMMA, `(`, `)`}
import io.suggest.lk.m.{ColorBtnClick, ColorChanged}
import japgolly.univeq._
import io.suggest.ueq.ReactUnivEqUtil._
import io.suggest.scalaz.ZTreeUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 17:53
  * Description: Специальный Strip-only color pick редактора, вешается ПАРАЛЛЕЛЬНО после [[ColorPickAh]].
  * Нужен для воздействия на рендер стрипа, чтобы цвет фона был виден.
  */
class ColorPickAfterStripAh[M](modelRW: ModelRW[M, MDocS]) extends ActionHandler(modelRW) {

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    case _: ColorBtnClick =>
      val v0 = value

      val needTransformOpt = for {
        // Если выделен стрип, имеющий фоновое изображение...
        selJdt <- v0.jdArgs.selectedTagLoc.toLabelOpt
        if selJdt.name ==* MJdTagNames.STRIP &&
           selJdt.props1.bgImg.nonEmpty
        // и открыт стрип-редактор...
        stripEd <- v0.stripEd
      } yield {
        stripEd.bgColorPick.isShown
      }

      needTransformOpt.fold(noChange)(_doTransform(_, v0))


    case _: ColorCheckboxChange =>
      _doTransform( true, value )

    // Если идёт тыканье по рекомендуемым цветам, то там выставляется forceTransform.
    case cc: ColorChanged if cc.forceTransform =>
      _doTransform( true, value )

    case m if m == DocBodyClick ||
      m.isInstanceOf[IStripAction] ||
      m.isInstanceOf[IAddAction] ||
      m.isInstanceOf[IJdAction] =>
      _maybeSetNewTrasform(Some(BG_IMG_ALL_TRANSFORM), value)

  }


  import japgolly.scalajs.react.vdom.TagMod

  private def _doTransform(isNeeded: Boolean, v0: MDocS): ActionResult[M] = {
    val tm0 = BG_IMG_ALL_TRANSFORM
    // Составить настройки состояния.
    val tm2 = if (isNeeded) {
      TagMod(tm0, BG_IMG_TRANSFORM)
    } else {
      tm0
    }
    val tm2Opt = Some(tm2)

    _maybeSetNewTrasform( tm2Opt, v0 )
  }

  private def _maybeSetNewTrasform(tmOpt: Option[TagMod], v0: MDocS): ActionResult[M] = {
    val ra0 = v0.jdArgs.renderArgs
    // Проверить, изменилось ли хоть что-нибудь:
    if (ra0.selJdtBgImgMod ==* tmOpt) {   // TODO есть сомнения в том, что данное сравнение работает вообще.
      noChange
    } else {
      // Сохранить новые настройки трансформации в состояние.
      val v2 = v0.withJdArgs(
        v0.jdArgs.withRenderArgs(
          ra0.withSelJdtBgImgMod( tmOpt )
        )
      )
      updated(v2)
    }
  }

  import japgolly.scalajs.react.vdom.html_<^._

  private def BG_IMG_TRANSFORM: TagMod = {
    // Отгибаем нижний правый угол:
    // matrix3d(1.149423, 0.000334, 0, 0.000334, 0, 1.123908, 0, 0.000457, 0, 0, 1, 0, -0, 2.123451, 0, 1)
    // Матрица построена через http://www.useragentman.com/matrix/#E4UwziAuBCCumQPYDsBMBeASuKACAYosALYBkAZsIsQAwAe6NFVtAnugLQCMpS9jvRDXY9K1Lu1QA2Hki4MmcyVIAszaqgaoAnLMSb0AZhoBWQahGDDWgOxmkhySsOkAJokMARdJGCwQpACWrlzoAMqwAEbEgZBBrhjYEHEAFpDEADYAwiiQIMiQ6AA8AA4AfAAqKYFguDW4AIa4kRmIAMYA1rggGSDE+ZAAdEUA9OUAUKRtYGA5BQPoIwBU47hLBCA9uJSbuEjdrrGNyKy4iOR7KSC4YJCsvbWRPYgA7oOrSyPj48vdyGCwUCNDIZS7XHp9Aa1W73ECuZrPF64WAQZpEVwgYAcSKIOhrL6DFrtDoAGlwAGIAObAYK4ADeq1wTJxdA4YECAC9AshKQAuNHADFYlkAbnGAF9vr8qtdEJEAFYgNqQBHkIjXI71XwNf5qkhw-HjcmILj0xnMhqdamIWDIVz88nkchtF1tMWSn7rGVnBVKlUNch5YB1FUpBqPTbIPbAHVgPX9eGfI36M1Mi1Wqi2+0U11tJ3uqXrHLEYgoG53B7bIhovFyxXKsCDQ2E1qdVNptUFNmckD87lXGmQd24Qu4LJhMJV4OQK64am0pNUmnwhlp5qWjrWrP8wEZAAUACJAsQGpTwCNIhut3bBpTAuQDwBKXCgEogBoqsBtKgg3A0P+4C81R5GKa5XIElJpPyAAcNA0CUdCgWmJSIOykCBCg-KgBkH6BAAbiASFMogBHAOQrQvPy1SuBiyAFvEhjhFEMQqgAEhUACyAAyxzwuOYTxCo6BZDqbQ9PEJjoG0rQQPEUhSTJIBAA
    val matrix =
      1.149423 :: 0.000334 :: 0 :: 0.000334 ::
      0        :: 1.123908 :: 0 :: 0.000457 ::
      0        :: 0        :: 1 :: 0        ::
      -0       :: 2.123451 :: 0 :: 1        ::
      Nil

    val transformStr = Css.Anim.Transform.MATRIX_3D + `(` + matrix.mkString(COMMA) + `)`
    ^.transform := transformStr
  }

  private def BG_IMG_ALL_TRANSFORM: TagMod = {
    val A = Css.Anim
    TagMod(
      ^.transformOrigin := A.Origin.TOP_LEFT,
      ^.transition      := {
        // "all 1s ease-in-out"
        val t = A.Transition
        t.all( 1, t.TimingFuns.EASE_IN_OUT )
      }
    )
  }

}
