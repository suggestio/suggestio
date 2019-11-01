package io.suggest.ad.edit.v.edit.strip

import com.materialui.MuiSliderProps.Value_t
import com.materialui.{MuiFormGroup, MuiFormGroupClasses, MuiFormGroupProps, MuiFormLabel, MuiSlider, MuiSliderMark, MuiSliderProps, MuiSliderValueLabelDisplay}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.blk.{IBlockSize, IBlockSizes}
import io.suggest.ad.edit.m.BlockSizeBtnClick
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 19:04
  * Description: Кнопки +/- для управления шириной или высотой рекламного блока.
  *
  * Компонент следует использовать через .wrap() вместо connect().
  */
class PlusMinusControlsR(
                          lkAdEditCss   : LkAdEditCss,
                        ) {

  /** Контейнер настроек для работы этого компонента. */
  case class PropsVal(
                       labelMsgCode     : String,
                       model            : IBlockSizes[_ <: IBlockSize],
                       current          : IBlockSize
                     )
  implicit object PlusMinusControlsPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.labelMsgCode ===* b.labelMsgCode) &&
      (a.model eq b.model) &&
      (a.current eq b.current)
    }
  }


  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  private lazy val _css = new MuiFormGroupClasses {
    override val row = lkAdEditCss.WhControls.slider.htmlClass
  }


  /** Бэкэнд рендера. */
  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на клик по одной из кнопок увеличения/уменьшения размера. */
    private def onBtnClick(v2: Value_t): Callback = {
      // Надо бы применить dispatchOnProxyScopeCB(), но тут зависимость от props.value...
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { p: Props =>
        BlockSizeBtnClick(p.value.get.model, v2)
      }
    }
    private lazy val onBtnClickCbF = ReactCommonUtil.cbFun2ToJsCb { (e: ReactEventFromHtml, v2: Value_t) =>
      onBtnClick( v2 )
    }

    /** Рендеринг компонента. */
    def render(p: Props): VdomElement = {
      p.value.whenDefinedEl { props: PropsVal =>
        val _marks = (for {
          v <- props
            .model
            .values
            .iterator
        } yield {
          new MuiSliderMark {
            override val value = v.value
            override val label = v.value.px
          }
        })
          .toJSArray

        MuiFormGroup(
          new MuiFormGroupProps {
            override val row = true
            override val classes = _css
          }
        )(
          MuiFormLabel()(
            Messages( props.labelMsgCode ),
          ),

          MuiSlider(
            new MuiSliderProps {
              override val max = props.model.max.value
              override val min = 0 //props.model.min.value
              override val marks = js.defined( _marks )
              override val value = js.defined( props.current.value )
              override val step = null
              override val onChangeCommitted = onBtnClickCbF
              override val valueLabelDisplay = MuiSliderValueLabelDisplay.Off
            }
          )
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build


  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
