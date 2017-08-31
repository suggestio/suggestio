package com.quilljs.delta

import scala.scalajs.js
import scala.scalajs.js.|
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 16:07
  * Description: Quill delta format spec.
  */
@js.native
@JSImport("quill-delta", JSImport.Namespace)
// from type may be simplified to js.Object
class Delta( from: js.Array[DeltaOp] | Delta | js.Object = js.native ) extends js.Object {

  var ops: js.Array[DeltaOp] = js.native

  // Construction

  /** Add insert operation.
    *
    * @param data Text:String | EmbedType:Int | Embed:js.Object
    * @param attributes Optional js Object.
    * @return this.
    */
  def insert(data: DeltaInsertData_t, attributes: js.UndefOr[DeltaOpAttrs] = js.native): this.type = js.native

  def delete(howMany: Int): this.type = js.native

  def retain(length: Int, attributes: js.UndefOr[DeltaOpAttrs] = js.native): this.type = js.native


  // Documents

  def concat(other: Delta): Delta = js.native

  def diff(other: Delta, index: Int = js.native): Delta = js.native

  def eachLine(f: js.Function3[Delta, DeltaOpAttrs, Int, Boolean]): Unit = js.native


  // Utility

  def filter(f: js.Function1[DeltaOp, Boolean]): js.Array[DeltaOp] = js.native

  def forEach(f: js.Function1[DeltaOp, _]): Unit = js.native

  def length(): Int = js.native

  def map[A](f: js.Function1[DeltaOp, A]): js.Array[A] = js.native

  def partition(f: js.Function1[DeltaOp, Boolean]): js.Array[js.Array[DeltaOp]] = js.native

  def reduce[A](f: js.Function1[DeltaOp, A], initial: A): A = js.native

  def slice(start: Int = js.native, end: Int = js.native): Delta = js.native


  // Operational Transform

  def compose(other: Delta): Delta = js.native

  def transform(other: Delta, priority: Boolean): Delta = js.native

  def transformPosition(index: Int): Int = js.native

}

