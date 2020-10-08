package io.suggest.primo

import io.suggest.common.empty.NonEmpty
import io.suggest.scalaz.ScalazUtil
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, OFormat, __}
import scalaz.{NonEmptyList, Validation, ValidationNel}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:46
  * Description: Модель, хранящая абстрактное значение set(x) или unset.
  *
  * По сути своей напоминает Option[X], но подразумевает явную сериализацию в json-объект при любом раскладе.
  * UnSetVal подразумевает необходимость стирания любого значения, а не отсутствие значение.
  * SetVal подразумевает выставление нового значения.
  */

object ISetUnset {

  /** Сборка поддержки play-json. */
  implicit def setUnsetFormat[T: Format]: OFormat[ISetUnset[T]] = {
    (__ \ "v").formatNullable[T]
      .inmap[ISetUnset[T]]( apply, _.toOption )
  }

  @inline implicit def univEq[T: UnivEq]: UnivEq[ISetUnset[T]] = UnivEq.force

  /** Собрать из option'а. */
  def apply[T](vOpt: Option[T]): ISetUnset[T] = {
    vOpt.fold[ISetUnset[T]](UnSetVal)(SetVal.apply)
  }


  def validateSet[E, T](su: ISetUnset[T], errorIfUnset: => E,
                        f: T => ValidationNel[E, T] = Validation.success[NonEmptyList[E], T]): ValidationNel[E, ISetUnset[T]] = {
    ScalazUtil.liftNelSome(su.toOption, errorIfUnset)(f)
      .map(fromOption)
  }


  /** Провалидировать Set или None значение положительно.
    * По сути, это враппер над validateSetOpt()(), потому что scala не переваривает дефолтовое значение в f.
    */
  def validateSetOptDflt[E, T](suOpt: Option[ISetUnset[T]], errorIfUnset: => E): ValidationNel[E, Option[ISetUnset[T]]] = {
    validateSetOpt(suOpt, errorIfUnset)(Validation.success)
  }
  /** Провалидировать None положительно, Set с помощью функции, Unset отрицательно. */
  def validateSetOpt[E, T](suOpt: Option[ISetUnset[T]], errorIfUnset: => E)
                          (f: T => ValidationNel[E, T]): ValidationNel[E, Option[ISetUnset[T]]] = {
    ScalazUtil.liftNelOpt(suOpt) { su =>
      ISetUnset.validateSet(su, errorIfUnset, f)
    }
  }


  def fromOption[T](opt: Option[T]): ISetUnset[T] = {
    opt.fold[ISetUnset[T]](UnSetVal)(SetVal.apply)
  }

}

/** Интерфейс модели Set/unset. */
sealed trait ISetUnset[+T] extends NonEmpty {
  def isSet: Boolean
  def get: T
  def toList: List[T]
  def toOption: Option[T]
  def map[A](f: T => A): ISetUnset[A]
  def exists(f: T => Boolean): Boolean =
    isSet && f(get)
  def foreach[U](f: T => U): Unit
}


object SetVal {
  @inline implicit def univEq[T: UnivEq]: UnivEq[SetVal[T]] = UnivEq.derive
}

/** Выставление значения указанного типа. */
case class SetVal[T](value: T) extends ISetUnset[T] {
  override def isSet    = true
  override def get      = value
  override def toList   = value :: Nil
  override def toOption = Some(value)
  override def map[A](f: T => A) = SetVal(f(value))
  override def foreach[U](f: (T) => U): Unit = f(value)
  override def isEmpty = false
  override def toString = value.toString
}


/** Отмена значения. */
case object UnSetVal extends ISetUnset[Nothing] {
  override def isSet    = false
  override def get      = throw new IllegalArgumentException
  override def toList   = Nil
  override def toOption = None
  override def map[A](f: Nothing => A) = this
  override def foreach[U](f: (Nothing) => U): Unit = {}
  override def isEmpty = true
  override def toString = ""

  @inline implicit def univEq: UnivEq[UnSetVal.type] = UnivEq.derive
}
