package com.github.react.dnd

import com.github.react.dnd.backend.test.TestBackend
import minitest._

import scala.scalajs.js
import japgolly.scalajs.react._
import japgolly.scalajs.react.test._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.2019 12:29
  */
object DragSourceTest extends SimpleTestSuite {

  test("DragSourceJs must exist in namespace") {
    assert( !js.isUndefined(DragSourceJs) )
    assert( DragSourceJs != null )
  }


  test("DragSource API must be usable") {
    val PREFIX = "Hello world,"
    def IS_DRAGGING(is: Boolean) = s"drag?$is"

    val compInner = ScalaComponent
      .builder[DragSourceTestJsCompProps1]("Inner")
      .stateless
      .render_P { p =>
        val el = <.div(
          PREFIX, " ", p.name, "! ", IS_DRAGGING(p.isDragging)
        )
        p.connectDragSourceF.applyVdomEl(el)
      }
      .build

    val DRAGGER = "DRAGGER"

    val compDraggable = DragSource[DragSourceTestJsCompProps1, DragSourceTestJsCompProps1, js.Object, Children.None](
      itemType = "test",
      spec = new DragSourceSpec[DragSourceTestJsCompProps1, js.Object] {
        override def beginDrag(props: DragSourceTestJsCompProps1, monitor: DragSourceMonitor, component: js.Any): js.Object = ???
      },
      collect = { (connect: DragSourceConnector, monitor: DragSourceMonitor) =>
        new DragSourceTestJsCompProps1 {
          override val name               = DRAGGER
          override val connectDragSourceF = connect.dragSource()
          override val isDragging         = monitor.isDragging()
        }
      }
    )(
      compInner
        .toJsComponent
        .raw
    )

    val outerComp = ScalaComponent
      .builder[Unit]("Outer")
      .render_ {
        <.div(
          DndProvider.component(
            new DndProviderProps {
              override val backend = TestBackend
            }
          )(
            compDraggable(
              new DragSourceTestJsCompProps1 {
                override val name = "ErrorDragSourceNotBinded"
                override val isDragging = false
              }
            )
          )
        )
      }
      .build

    ReactTestUtils.withRenderedIntoDocument( outerComp() ) { mounted =>
      // <div><div>Hello world, DRAGGER! drag?false</div></div>
      val htmlStr = mounted.outerHtmlScrubbed()

      assert( htmlStr contains PREFIX, "<Inner> component not mounted?" )
      assert( htmlStr contains DRAGGER, "Looks like, props was NOT overwritten by DragSource HOC." )
      assert( htmlStr contains IS_DRAGGING(false), "Unexpectedly, dragging already active, but it should not." )
    }
  }

}


sealed trait DragSourceTestJsCompProps1 extends js.Object {
  val name: String
  val connectDragSourceF: js.UndefOr[DragSourceF[DragSourceFOptions]] = js.undefined
  val isDragging: Boolean
}
