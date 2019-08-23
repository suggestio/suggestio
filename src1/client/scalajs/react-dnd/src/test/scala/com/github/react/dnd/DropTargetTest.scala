package com.github.react.dnd

import com.github.react.dnd.backend.test.TestBackend
import minitest._
import japgolly.scalajs.react._
import japgolly.scalajs.react.test._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.2019 17:43
  */
object DropTargetTest extends SimpleTestSuite {

  test("DropTargetJs must exist in namespace") {
    assert( !js.isUndefined(DropTargetJs) )
    assert( DropTargetJs != null )
  }


  test("DropTarget API must work") {
    val PREFIX = "DROP ZONE:"
    def IS_OVER(is: Boolean) = " isOver=" + is.toString

    val compInner = ScalaComponent
      .builder[DropTargetTestJsCompProps1]( "Inner" )
      .stateless
      .render_P { p =>
        val el = <.div(
          PREFIX,
          " name=", p.name,
          IS_OVER(p.isOver),
        )

        p.connectDropTargetF.applyVdomEl(el)
      }
      .build

    val jsCompInner = compInner
      .toJsComponent
      .raw

    val DROPNAME = "DROPPER"

    val jsCompDroppable = DropTarget(
      itemType = "asdasd",
      collect = { (connector, monitor) =>
        new DropTargetTestJsCompProps1 {
          override val name = DROPNAME
          override val connectDropTargetF = connector.dropTarget()
          override val isOver: Boolean = monitor.isOver()
        }
      }
    )(jsCompInner)
    val compDroppable = JsComponent[DropTargetTestJsCompProps1, Children.None, Null](jsCompDroppable)

    val outerComp = ScalaComponent
      .builder[Unit]("Outer")
      .render_ {
        <.div(
          DndProvider.component(
            new DndProviderProps {
              override val backend = TestBackend
            }
          )(
            compDroppable(
              new DropTargetTestJsCompProps1 {
                override val name = "unbinded"
                override val isOver = false
              }
            )
          )
        )
      }
      .build

    ReactTestUtils.withRenderedIntoDocument( outerComp() ) { mounted =>
      // <div><div>DROP ZONE: name=DROPPER isOver=false</div></div>
      val html = mounted.outerHtmlScrubbed()

      assert( html contains PREFIX )
      assert( html contains DROPNAME )
      assert( html contains IS_OVER(false) )
    }
  }

}


sealed trait DropTargetTestJsCompProps1 extends js.Object {
  val name: String
  val connectDropTargetF: js.UndefOr[DropTargetF] = js.undefined
  val isOver: Boolean
}
