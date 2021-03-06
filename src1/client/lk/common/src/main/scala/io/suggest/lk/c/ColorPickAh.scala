package io.suggest.lk.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.color.{IColorPickerMarker, MColorData}
import io.suggest.common.empty.OptionUtil
import io.suggest.lk.m.color.{MColorPick, MColorPickerS, MColorsState}
import io.suggest.lk.m.{ColorBtnClick, ColorChanged, ColorCheckboxChange, DocBodyClick}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 16:09
  * Description: Action handler для опциональных color-picker'ов.
  * Здесь используется собственная модель вместо MDocS чтобы астрагироваться от множества
  * однотипных colorPick'ов ровно одним контроллером с помощью разных zoomRW-комбинаций.
  * @param myMarker маркер, который ожидается в сообщениях.
  */
class ColorPickAh[M](
                      myMarker    : Option[IColorPickerMarker],
                      // TODO Надо как-то избавится от Option, он только мешает.
                      modelRW     : ModelRW[M, Option[MColorPick]]
                    )
  extends ActionHandler( modelRW )
{

  private def isMyMarker(marker: Option[IColorPickerMarker]): Boolean =
    marker ==* myMarker

  private def isMyPickerOpened(): Boolean = {
    value.exists(
      _.colorsState.picker
        .exists { c => isMyMarker(c.marker) }
    )
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал об изменении цвета. m.marker - используется в ColorSuggestR, чтобы заменять цвета без picker'а.
    case m: ColorChanged if isMyPickerOpened() || isMyMarker(m.marker) =>
      val v0 = value.get
      var v2 = (MColorPick.colorOpt replace Some( m.mcd ))(v0)

      // Запилить в состояние презетов выбранный цвет.
      if (m.isCompleted && !(v2.colorsState.colorPresets.colors contains[MColorData] m.mcd)) {
        v2 = MColorPick.colorsState
          .modify( _.prependPresets(m.mcd) )(v2)
      }

      updated( Some(v2) )


    // Клик где-то за пределами picker'а, когда тот открыт. Значит надо скрыть текущий picker.
    case DocBodyClick if isMyPickerOpened() =>
      val v0 = value.get

      // Убрать с экрана picker
      var v2 = MColorPick.colorsState
        .andThen( MColorsState.picker )
        .replace( None )(v0)

      // Сохранить текущий цвет.
      for (
        mcd <- v0.colorOpt
        if !(v2.colorsState.colorPresets.colors contains[MColorData] mcd)
      ) {
        v2 = MColorPick.colorsState
          .modify( _.prependPresets(mcd) )(v2)
      }

      updated( Some(v2) )


    // Color picker
    // Переключение между прозрачным цветом и заливкой.
    case m: ColorCheckboxChange if isMyMarker(m.marker) =>
      val v0 = value.get

      if (m.isEnabled && v0.colorOpt.isEmpty) {
        // Включен выбор цвета вместо прозрачного. Пошукать "старый" цвет, который был до выключения. Или какой-нибудь дефолтовый.
        val mcd2 = v0.colorsState.picker
          .flatMap(_.oldColor)
          .orElse {
            // "Старого" цвета нет, но возможно есть среди других цветов что-нибудь
            v0.colorsState
              .colorPresets
              .colors
              .headOption
          }
          .getOrElse {
            // Вообще нет никаких цветов, ужас какой-то.
            MColorData.Examples.BLACK
          }
        val v2 = (MColorPick.colorOpt replace Some(mcd2))(v0)
        updated( Some(v2) )

      } else if (!m.isEnabled && v0.colorOpt.nonEmpty) {
        val color0 = v0.colorOpt.get

        // Выключился цвет. Переместить текущий цвет в colorPicker-состояние, picker скрыть, основной цвет в None
        val v2 = v0.copy(
          colorOpt    = None,
          // И запихивать старый цвет в список цветов вместо oldColor?
          colorsState = v0.colorsState.copy(
            colorPresets = v0.colorsState.colorPresets.withColors(
              (color0 :: v0.colorsState.colorPresets.colors).distinct
            ),
            picker = for (p <- v0.colorsState.picker)
              yield p.withOldColor( v0.colorOpt )
          )
        )
        updated( Some(v2) )

      } else {
        noChange
      }


    // Сигнал клика по селектору цвета.
    case m: ColorBtnClick if isMyMarker(m.marker) =>
      val v0 = value.get
      val pickerOpt2 = OptionUtil.maybe( v0.colorsState.picker.isEmpty ) {
        MColorPickerS(
          shownAt  = m.vpXy,
          marker   = m.marker,
          oldColor = v0.colorsState.picker.flatMap(_.oldColor)
        )
      }
      val v2 = MColorPick.colorsState
        .andThen( MColorsState.picker )
        .replace( pickerOpt2 )( v0 )

      updated( Some(v2) )

  }

}
