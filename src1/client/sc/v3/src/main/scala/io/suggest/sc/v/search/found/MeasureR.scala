package io.suggest.sc.v.search.found

import io.suggest.react.ReactCommonUtil.Implicits._
import com.github.souporserious.react.measure.{ContentRect, Measure, MeasureChildrenArgs, MeasureProps}
import diode.react.ModelProxy
import io.suggest.common.geom.d2.MSize2di
import io.suggest.react.ReactCommonUtil
import io.suggest.spa.DAction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.07.2020 11:04
  * Description: Реализация простой обёртки над react-measure.
  * Some(true) - надо измерять.
  * Some(false) - измерение сейчас не требуется.
  */
class MeasureR {

  case class PropsVal(
                       mkActionF            : MSize2di => DAction,
                       isMeasuringNowPx     : ModelProxy[Some[Boolean]],
                     )

  type Props = PropsVal


  class Backend($ : BackendScope[Props, Unit]) {

    /** Callback после измерения размера. */
    private val __onResizeJsCbF = ReactCommonUtil.cbFun1ToJsCb { contentRect: ContentRect =>
      $.props >>= { props: Props =>
        // Проверять $.props.rHeightPx.isPending?
        if (props.isMeasuringNowPx.value.value) {
          val b = contentRect.bounds.get
          val bounds2d = MSize2di(
            width  = b.width.toInt,
            height = b.height.toInt,
          )
          val action = props.mkActionF( bounds2d )
          props.isMeasuringNowPx.dispatchCB( action )
        } else {
          Callback.empty
        }
      }
    }


    def render(children: PropsChildren): VdomElement = {
      // Функция сборки children:
      def __mkChildrenF(args: MeasureChildrenArgs): raw.PropsChildren = {
        <.div(
          ^.genericRef := args.measureRef,
          children,
        )
          .rawElement
      }

      // Измерить список.
      Measure {
        new MeasureProps {
          override val bounds = true
          override val children = __mkChildrenF
          override val onResize = __onResizeJsCbF
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

}
