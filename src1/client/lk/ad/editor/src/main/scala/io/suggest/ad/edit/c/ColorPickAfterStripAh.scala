package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m._
import io.suggest.jd.render.m.{IJdAction, IJdTagClick}
import io.suggest.jd.tags.MJdTagNames
import japgolly.scalajs.react.vdom.TagMod
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 17:53
  * Description: Специальный Strip-only color pick редактора, вешается ПАРАЛЛЕЛЬНО после [[ColorPickAh]].
  * Нужен для воздействия на рендер стрипа, чтобы цвет фона был виден.
  */
class ColorPickAfterStripAh[M](modelRW: ModelRW[M, MDocS]) extends ActionHandler(modelRW) {

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    case ColorBtnClick =>
      val v0 = value

      val needTransformOpt = for {
        // Если выделен стрип, имеющий фоновое изображение...
        selJdt <- v0.jdArgs.selectedTag
        if selJdt.jdTagName ==* MJdTagNames.STRIP &&
           selJdt.props1.bgImg.nonEmpty
        // и открыт стрип-редактор...
        stripEd <- v0.stripEd
      } yield {
        stripEd.bgColorPick.isShown
      }

      needTransformOpt.fold(noChange)(_doTransform(_, v0))


    case _: ColorCheckboxChange =>
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
    if (ra0.selJdtBgImgMod == tmOpt) {
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
    // Матрица построена через http://www.useragentman.com/matrix/
    val matrix =
      1.149423 :: 0.000334 :: 0 :: 0.000334 ::
      0        :: 1.123908 :: 0 :: 0.000457 ::
      0        :: 0        :: 1 :: 0        ::
      -0       :: 2.123451 :: 0 :: 1        ::
      Nil

    ^.transform       := "matrix3d(" + matrix.mkString(",") + ")"
  }

  private def BG_IMG_ALL_TRANSFORM: TagMod = {
    TagMod(
      ^.transformOrigin := "top left",
      ^.transition      := "all 1s ease-in-out"
    )
  }

}
