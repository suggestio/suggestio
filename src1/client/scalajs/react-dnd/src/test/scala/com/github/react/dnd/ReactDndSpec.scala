package com.github.react.dnd

import com.github.react.dnd.backend.test.TestBackend
import japgolly.scalajs.react.test.ReactTestUtils
import minitest._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.testinterface.TestUtils

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.08.2019 11:19
  */
object ReactDndSpec extends SimpleTestSuite {

  test("useDrag() must exist in namespace") {
    assert( !js.isUndefined(useDragJs) )
    assert( useDragJs != null )
  }

  // TODO Не пашет: JavaScriptException: ReferenceError: document is not defined
  // Надо разобраться с ReactTestUtils - react-dom. http://react-dnd.github.io/react-dnd/docs/testing
  /*
  test("useDrag must be usable in vdom") {
    val comp = ScalaComponent
      .builder[Unit]("test1")
      .stateless
      .render_ {
        val dragItem = new DragsSpecItem {
          val `type`: ItemType_t = "ttt"
        }
        val useDragRes = ReactDnd.useDrag(
          new DragSpec {
            override val item = dragItem
          }
        )
        DndProvider.component(
          new DndProviderProps {
            override val backend = TestBackend
          }
        )(
          <.div.withRef( useDragRes.dragSourceRef )(
            "test"
          )
        )
      }
      .build

    ReactTestUtils.withRenderedIntoDocument( comp() ) { m =>
      assert( m != null )
    }
  }
  */

  test("useDrop() must exist in namespace") {
    assert( !js.isUndefined(useDropJs) )
    assert( useDropJs != null )
  }

  test("useDragLayer() must exist in namespace") {
    assert( !js.isUndefined(useDragLayerJs) )
    assert( useDragLayerJs != null )
  }

}
