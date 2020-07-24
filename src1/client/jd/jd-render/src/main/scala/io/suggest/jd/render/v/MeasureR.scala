package io.suggest.jd.render.v

import com.github.souporserious.react.measure.{ContentRect, Measure, MeasureChildrenArgs, MeasureProps}
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.async.AsyncUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.07.2020 11:04
  * Description: Реализация простой обёртки над react-measure.
  * Some(true) - надо измерять.
  * Some(false) - измерение сейчас не требуется.
  *
  * Подразумевается использование внутри connection в случае изменения размера после рендера.
  */
class MeasureR {

  /** @param onMeasured Что делать при успешном замере?
    * @param isToMeasure Прямо сейчас происходит ли измерение?
    * @param mBounds measure bounds?
    * @param mClient measure client rect?
    */
  case class PropsVal(
                       onMeasured           : ContentRect => Callback,
                       isToMeasure          : () => Boolean,
                       mBounds              : Boolean,
                       mClient              : Boolean,
                       childrenTag          : TagOf[_ <: dom.html.Element],
                     )

  type Props = PropsVal


  class Backend($ : BackendScope[Props, Unit]) {

    /** Callback после измерения размера. */
    private val __onResizeJsCbF = ReactCommonUtil.cbFun1ToJsCb { contentRect: ContentRect =>
      $.props >>= { props: Props =>
        // Проверять $.props.rHeightPx.isPending?
        if (props.isToMeasure()) {
          props.onMeasured( contentRect )
        } else {
          Callback.empty
        }
      }
    }


    def render(p: Props): VdomElement = {
      // Флаг для костыля принудительного ручного re-measure в mkChildrenF().
      // Нужна для запрета бесконечного цикла измерения размеров.
      var isReMeasured = false

      // Функция сборки children:
      def __mkChildrenF(chArgs: MeasureChildrenArgs): raw.PropsChildren = {
        // Костыль для принудительного запуска одного повторного измерения, если в состоянии задано (Pot[QdBl*].pending).
        // Запуск измерения вызывает повторный __mkChildrenF(), поэтому нужна максимально жесткая и гарантированная
        // система предотвращения бесконечного цикла.
        isReMeasured.synchronized {
          if (!isReMeasured && p.isToMeasure()) {
            isReMeasured = true
            Future {
              chArgs.measure()
            }
          }
        }

        p.childrenTag(
          ^.genericRef := chArgs.measureRef,
        )
          .rawElement
      }

      // Измерить список.
      Measure {
        new MeasureProps {
          override val bounds   = p.mBounds
          override val client   = p.mClient
          override val children = __mkChildrenF
          override val onResize = __onResizeJsCbF
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
