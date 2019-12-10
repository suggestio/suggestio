package io.suggest.ueq

import java.time.OffsetDateTime

import japgolly.univeq.UnivEq
import play.api.libs.json._
import scalaz.NonEmptyList

import scala.collection.immutable.ListMap
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 10:11
  * Description: Кросс-платформенная утиль для UnivEq.
  */
object UnivEqUtil {

  /** Расширенные операции для UnivEq. */
  implicit final class UnivEqExtOps2[A <: AnyRef](private val a: A) extends AnyVal {

    /**
      * Поддержка eq-сравнивания для UnivEq.
      * Вместо "eq*" пишем "===*", потому что смесь букв со знаками в названии метода не работает.
      */
    @inline def ===*[B >: A <: AnyRef : UnivEq](b: B): Boolean = {
      a eq b
    }

    /**
      * Поддержка ne-сравнивания для UnivEq.
      * Вместо "ne*" пишем "!==*", потому что смесь букв со знаками в названии метода не работает.
      */
    @inline def !===*[B >: A <: AnyRef : UnivEq](b: B): Boolean = {
      a ne b
    }

  }


  // Поддержка разных типов для UnivEq.
  @inline implicit def doubleUe           : UnivEq[Double]          = UnivEq.force
  @inline implicit def floadUe            : UnivEq[Float]           = UnivEq.force
  @inline implicit def sBigDecimalUe      : UnivEq[BigDecimal]      = UnivEq.force
  @inline implicit def jBigDecimalUe      : UnivEq[java.math.BigDecimal] = UnivEq.force
  @inline implicit def jBigIntUe          : UnivEq[java.math.BigInteger] = UnivEq.force


  @inline implicit def scSeqUe[T: UnivEq]       : UnivEq[collection.Seq[T]] = UnivEq.force
  @inline implicit def sciSeqUe[T: UnivEq]      : UnivEq[Seq[T]]          = UnivEq.force
  @inline implicit def setUe[T: UnivEq]         : UnivEq[Set[T]]          = UnivEq.force
  @inline implicit def indexedSeqUe[T: UnivEq]  : UnivEq[IndexedSeq[T]]   = UnivEq.force
  @inline implicit def iterableUe[T: UnivEq]    : UnivEq[Iterable[T]]     = UnivEq.force

  @inline implicit def throwableUe        : UnivEq[Throwable]       = UnivEq.force

  @inline implicit def fun0Ue[R]          : UnivEq[() => R]         = UnivEq.force
  @inline implicit def fun1Ue[T, R]       : UnivEq[(T) => R]        = UnivEq.force

  @inline implicit def jsValueUe          : UnivEq[JsValue]         = UnivEq.force
  @inline implicit def jsObjectUe         : UnivEq[JsObject]        = UnivEq.force
  @inline implicit def jsStringUe         : UnivEq[JsString]        = UnivEq.force
  @inline implicit def jsNumberUe         : UnivEq[JsNumber]        = UnivEq.force
  @inline implicit def jsNullUe           : UnivEq[JsNull.type]     = UnivEq.force
  @inline implicit def jsArrayUe          : UnivEq[JsArray]         = UnivEq.force

  @inline implicit def mapUe[K: UnivEq, V: UnivEq]: UnivEq[Map[K, V]] = UnivEq.force
  @inline implicit def immListMapUe[K: UnivEq, V: UnivEq]: UnivEq[ListMap[K, V]] = UnivEq.force

  // java.time
  @inline implicit def offsetDateTimeUe   : UnivEq[OffsetDateTime]  = UnivEq.force

  @inline implicit def tryUe[T: UnivEq]   : UnivEq[Try[T]]          = UnivEq.force

  // scalaz
  @inline implicit def nelUe[T: UnivEq]   : UnivEq[NonEmptyList[T]] = UnivEq.force

  @inline implicit def uuidUe             : UnivEq[java.util.UUID]  = UnivEq.force

}
