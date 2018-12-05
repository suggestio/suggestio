package io.suggest.lk.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.color.{IColorPickerMarker, MColorData}
import io.suggest.common.empty.OptionUtil
import io.suggest.lk.m.color.{MColorPick, MColorPickerS}
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

  private def isMyPickerOpened(): Boolean =
    value.exists(_.colorsState.picker.exists(_.marker ==* myMarker))


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал об изменении цвета.
    case m: ColorChanged if isMyPickerOpened()  =>
      val v0 = value.get
      var v2 = v0.withColorOpt(
        colorOpt = Some( m.mcd )
      )

      // Запилить в состояние презетов выбранный цвет.
      if (m.isCompleted && !(v2.colorsState.colorPresets contains m.mcd)) {
        v2 = v2.withColorsState(
          v2.colorsState
            .prependPresets( m.mcd )
        )
      }

      updated( Some(v2) )


    // Клик где-то за пределами picker'а, когда тот открыт. Значит надо скрыть текущий picker.
    case DocBodyClick if isMyPickerOpened() =>
      val v0 = value.get

      // Убрать с экрана picker
      var v2 = v0.withColorsState(
        v0.colorsState.withPicker( None )
      )

      // Сохранить текущий цвет.
      for (
        mcd <- v0.colorOpt
        if !v2.colorsState.colorPresets.contains(mcd)
      ) {
        v2 = v2.withColorsState(
          v2.colorsState.prependPresets( mcd )
        )
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
              .headOption
          }
          .getOrElse {
            // Вообще нет никаких цветов, ужас какой-то.
            MColorData.Examples.BLACK
          }
        val v2 = v0.withColorOpt( Some(mcd2) )
        updated( Some(v2) )

      } else if (!m.isEnabled && v0.colorOpt.nonEmpty) {
        val color0 = v0.colorOpt.get

        // Выключился цвет. Переместить текущий цвет в colorPicker-состояние, picker скрыть, основной цвет в None
        val v2 = v0.copy(
          colorOpt    = None,
          // И запихивать старый цвет в список цветов вместо oldColor?
          colorsState = v0.colorsState.copy(
            colorPresets = (color0 :: v0.colorsState.colorPresets).distinct,
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
      val v2 = v0.withColorsState(
        v0.colorsState
          .withPicker( pickerOpt2 )
      )
      updated( Some(v2) )

  }

}
