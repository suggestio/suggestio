package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m.{ColorBtnClick, ColorChanged, ColorCheckboxChange}
import io.suggest.ad.edit.m.edit.MColorPick
import io.suggest.model.n2.node.meta.colors.MColorData

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 16:09
  * Description: Action handler для опциональных color-picker'ов.
  * Здесь используется собственная модель вместо MDocS чтобы астрагироваться от множества
  * однотипных colorPick'ов ровно одним контроллером с помощью разных zoomRW-комбинаций.
  */
class ColorPickAh[M](
                      // TODO Надо как-то избавится от Option, он только мешает.
                      modelRW: ModelRW[M, Option[MColorPick]]
                    )
  extends ActionHandler( modelRW )
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал об изменении цвета.
    case m: ColorChanged =>
      val v0 = value.get
      val v2 = v0.withColorOpt(
        colorOpt = Some( m.mcd )
      )
      updated( Some(v2) )


    // Color picker
    // Переключение между прозрачным цветом и заливкой.
    case m: ColorCheckboxChange =>
      val v0 = value.get

      if (m.isEnabled && v0.colorOpt.isEmpty) {
        // Включен выбор цвета вместо прозрачного. Пошукать "старый" цвет, который был до выключения. Или какой-нибудь дефолтовый.
        val mcd2 = v0.pickS
          .oldColor
          .orElse {
            // "Старого" цвета нет, но возможно есть среди других цветов что-нибудь
            v0.colorsState
              .colorPresets
              .headOption
          }
          .getOrElse {
            // Вообще нет никаких цветов, ужас какой-то.
            MColorData("000000")
          }
        val v2 = v0.withColorOpt( Some(mcd2) )
        updated( Some(v2) )

      } else if (!m.isEnabled && v0.colorOpt.nonEmpty) {
        val color0 = v0.colorOpt.get

        // Выключился цвет. Переместить текущий цвет в colorPicker-состояние, picker скрыть, основной цвет в None
        val v2 = v0.copy(
          colorOpt    = None,
          pickS       = v0.pickS.withOldColor( v0.colorOpt ),
          // И запихивать старый цвет в список цветов вместо oldColor?
          colorsState = v0.colorsState.withColorPresets(
            (color0 :: v0.colorsState.colorPresets).distinct
          )
        )
        updated( Some(v2) )

      } else {
        noChange
      }


    // Сигнал клика по селектору цвета.
    case ColorBtnClick =>
      val v0 = value.get
      val v2 = v0.withPickS(
        v0.pickS.withIsShownInverted
      )
      updated( Some(v2) )

  }

}
