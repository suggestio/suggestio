package io.suggest.bill.cart

import io.suggest.url.bind.{QsBindable, QsBinderF, QsUnbinderF, QueryStringBindableUtil}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Container for one cart submit method.
  *
  * @param isTest Is test mode here?
  */
final case class MPayableVia(
                              // isTest: no Option[] here, because QSB needs at least one mandatory field.
                              isTest: Boolean,
                            )

object MPayableVia {

  object Fields {
    final def IS_TEST = "test"
  }

  @inline implicit def univEq: UnivEq[MPayableVia] = UnivEq.derive

  /** JSON support for [[MPayableVia]] instances. */
  implicit def payableViaJson: OFormat[MPayableVia] = {
    val F = Fields
    (__ \ F.IS_TEST)
      .format[Boolean]
      .inmap[MPayableVia](apply, _.isTest)
  }


  /** Sorting support. */
  implicit def payableViaOrdering: Ordering[MPayableVia] = {
    new Ordering[MPayableVia] {
      override def compare(x: MPayableVia, y: MPayableVia): Int = {
        val boolOrd = implicitly[Ordering[Boolean]]
        boolOrd.compare( x.isTest, y.isTest )
      }
    }
  }


  /** Query-string bindable for [[MPayableVia]] instances. */
  implicit def payableViaQsB(implicit boolB: QsBindable[Boolean]): QsBindable[MPayableVia] = {
    new QsBindable[MPayableVia] {

      override def bindF: QsBinderF[MPayableVia] = { (key, params) =>
        val k = QueryStringBindableUtil.key1F( key )
        val F = Fields
        for {
          isTestE <- boolB.bindF( k( F.IS_TEST ), params )
        } yield {
          for {
            isTest <- isTestE
          } yield {
            MPayableVia(
              isTest = isTest,
            )
          }
        }
      }

      override def unbindF: QsUnbinderF[MPayableVia] = { (key, value) =>
        val k = QueryStringBindableUtil.key1F( key )
        val F = Fields
        boolB.unbindF( k(F.IS_TEST), value.isTest )
      }

    }
  }

}
