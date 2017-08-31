package com.quilljs.delta

import minitest._

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 12:40
  * Description: Tests for [[Delta]] class.
  */
object DeltaSpec extends SimpleTestSuite {

  test("instantiate a new delta") {
    val d = new Delta()
    assertEquals( d.length() , 0 )
    assertEquals( d.ops.length, 0 )
  }


  test("delta.insert() text") {
    val s = new Delta()
      .insert("the text example")
    val l = s.length()
    assert( l > 0, l.toString )
  }


  test("instantiate new Delta from other Delta") {
    val d0 = new Delta()
      .insert("asd")
    val d1 = new Delta( d0 )
    assertEquals( d1.length(), d0.length() )
  }


  test("Non-native delta op has usable API") {
    val text = "asdasd"
    val deltaOp = new Delta()
      .insert(text)
      .ops
      .head
    assertEquals( deltaOp.insert, text: UndefOr[String] )
  }

}
